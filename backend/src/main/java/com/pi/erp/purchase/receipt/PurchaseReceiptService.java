package com.pi.erp.purchase.receipt;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.purchase.Purchase;
import com.pi.erp.purchase.PurchaseRepository;
import com.pi.erp.purchase.PurchaseStatus;
import com.pi.erp.purchase.item.PurchaseItem;
import com.pi.erp.purchase.item.PurchaseItemRepository;
import com.pi.erp.stock.StockItem;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.stock.movement.StockMovementService;
import com.pi.erp.stock.reservation.StockReservationRepository;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseReceiptService {

    @Autowired
    private PurchaseReceiptRepository repository;

    @Autowired
    private PurchaseReceiptItemRepository purchaseReceiptItemRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseItemRepository purchaseItemRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<PurchaseReceipt> search(Long id) {
        if (!purchaseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Purchase not found.");
        }
        return repository.findByPurchaseIdOrderByIdAsc(id);
    }

    @Transactional(readOnly = true)
    public Optional<PurchaseReceipt> findOne(Long purchaseId, Long id) {
        return repository.findByIdAndPurchaseId(id, purchaseId);
    }

    @Transactional
    public ReceiptRegistration register(Long id, RequestPurchaseReceiptDTO data) {
        CurrentUser.require("purchases.receive");
        Purchase purchase = purchaseRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase not found."));
        entityManager.refresh(purchase);

        if (data.requestId() == null) {
            throw new IllegalArgumentException("Request ID is required.");
        }
        Map<Long, BigDecimal> quantities = requestedQuantities(data);
        String notes = data.notes() == null || data.notes().isBlank() ? null : data.notes().trim();
        Optional<PurchaseReceipt> previous = repository.findByPurchaseIdAndRequestId(
            id,
            data.requestId()
        );

        // An identical retry remains valid after completion or operator deactivation.
        if (previous.isPresent()) {
            PurchaseReceipt receipt = previous.get();
            Map<Long, BigDecimal> recordedQuantities = new TreeMap<>();
            for (PurchaseReceiptItem item : receipt.getItems()) {
                recordedQuantities.put(
                    item.getPurchaseItem().getId(),
                    item.getQuantity().stripTrailingZeros()
                );
            }
            if (
                (data.operatorUserId() != null &&
                    !receipt.getOperator().getId().equals(data.operatorUserId())) ||
                !Objects.equals(receipt.getNotes(), notes) ||
                !recordedQuantities.equals(quantities)
            ) {
                throw new IllegalArgumentException(
                    "Request ID was already used with different receipt data."
                );
            }
            return new ReceiptRegistration(receipt, false);
        }

        if (
            purchase.getStatus() != PurchaseStatus.CONFIRMED &&
            purchase.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED
        ) {
            throw new IllegalArgumentException(
                "Only confirmed or partially received purchases can receive goods."
            );
        }
        Long operatorUserId = CurrentUser.operator(data.operatorUserId());
        User operator = userRepository
            .findById(operatorUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        entityManager.refresh(operator);
        if (!Boolean.TRUE.equals(operator.getActive())) {
            throw new IllegalArgumentException("Operator user must be active.");
        }

        List<PurchaseItem> allItems = purchaseItemRepository.findByPurchaseId(id);
        if (allItems.isEmpty()) {
            throw new IllegalArgumentException("Purchase must have at least one item.");
        }
        productCatalogService.lockProducts(
            allItems.stream().map(PurchaseItem::getProduct).toList()
        );
        for (PurchaseItem item : allItems) {
            validateCounters(item);
        }

        // Root, scoped item rows and stock rows are locked in this order, with sorted IDs.
        Map<Long, PurchaseItem> items = new TreeMap<>();
        Map<Long, Long> stockIds = new TreeMap<>();
        Map<Long, BigDecimal> stockQuantities = new TreeMap<>();
        for (Map.Entry<Long, BigDecimal> entry : quantities.entrySet()) {
            PurchaseItem item = purchaseItemRepository
                .findByIdAndPurchaseIdForUpdate(entry.getKey(), id)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Purchase item not found in this purchase.")
                );
            entityManager.refresh(item);
            validateCounters(item);
            ProductQuantity.requireBase(
                entry.getValue(),
                item.getUnitSnapshot() != null
                    ? item.getUnitSnapshot()
                    : item.getProduct().getUnit(),
                false
            );
            if (
                entry
                    .getValue()
                    .compareTo(item.getBaseQuantity().subtract(item.getReceivedQuantity())) > 0
            ) {
                throw new IllegalArgumentException(
                    "Received quantity exceeds the outstanding purchase quantity."
                );
            }
            Long stockId = stockItemRepository
                .findIdByWarehouseIdAndProductId(
                    item.getWarehouse().getId(),
                    item.getProduct().getId()
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException("Stock item not found for purchase item.")
                );
            items.put(item.getId(), item);
            stockIds.put(item.getId(), stockId);
            stockQuantities.merge(stockId, entry.getValue(), BigDecimal::add);
        }

        Map<Long, StockItem> stocks = new TreeMap<>();
        for (Map.Entry<Long, BigDecimal> entry : stockQuantities.entrySet()) {
            StockItem stock = stockItemRepository
                .findByIdForUpdate(entry.getKey())
                .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));
            entityManager.refresh(stock);
            stocks.put(stock.getId(), stock);
        }
        stockReservationRepository.flush();
        for (Map.Entry<Long, BigDecimal> entry : stockQuantities.entrySet()) {
            StockItem stock = stocks.get(entry.getKey());
            if (
                stock.getQuantity() == null ||
                stock.getReservedQuantity() == null ||
                stock.getQuantity().signum() < 0 ||
                stock.getReservedQuantity().signum() < 0 ||
                stock.getReservedQuantity().compareTo(stock.getQuantity()) > 0 ||
                stock
                    .getReservedQuantity()
                    .compareTo(
                        stockReservationRepository.sumActiveQuantityByStockItemId(stock.getId())
                    ) != 0
            ) {
                throw new IllegalArgumentException(
                    "Stock item has inconsistent balances or reservations."
                );
            }
            ProductQuantity.requireBase(
                stock.getQuantity().add(entry.getValue()),
                stock.getProduct().getUnit(),
                true
            );
        }
        for (PurchaseItem item : items.values()) {
            StockItem stock = stocks.get(stockIds.get(item.getId()));
            if (
                !stock.getProduct().getId().equals(item.getProduct().getId()) ||
                !stock.getWarehouse().getId().equals(item.getWarehouse().getId())
            ) {
                throw new IllegalArgumentException("Stock origin changed. Retry the receipt.");
            }
        }

        PurchaseReceipt receipt = repository.save(
            new PurchaseReceipt(purchase, data.requestId(), operator, notes)
        );
        for (PurchaseItem item : items.values()) {
            BigDecimal quantity = quantities.get(item.getId());
            PurchaseReceiptItem receiptItem = purchaseReceiptItemRepository.save(
                new PurchaseReceiptItem(
                    receipt,
                    item,
                    stocks.get(stockIds.get(item.getId())),
                    quantity
                )
            );
            stockMovementService.registerPurchase(receiptItem);
            item.setReceivedQuantity(item.getReceivedQuantity().add(quantity));
            purchaseItemRepository.save(item);
            receipt.getItems().add(receiptItem);
        }
        purchaseItemRepository.flush();
        boolean complete = allItems
            .stream()
            .allMatch(item -> item.getBaseQuantity().compareTo(item.getReceivedQuantity()) == 0);
        purchase.setStatus(complete ? PurchaseStatus.RECEIVED : PurchaseStatus.PARTIALLY_RECEIVED);
        purchase.setUpdatedAt(LocalDateTime.now());
        purchaseRepository.save(purchase);
        return new ReceiptRegistration(receipt, true);
    }

    private Map<Long, BigDecimal> requestedQuantities(RequestPurchaseReceiptDTO data) {
        if (data.items() == null || data.items().isEmpty()) {
            throw new IllegalArgumentException("At least one receipt item is required.");
        }
        Map<Long, BigDecimal> quantities = new TreeMap<>();
        for (RequestPurchaseReceiptDTO.ItemDTO item : data.items()) {
            if (
                item == null ||
                item.purchaseItemId() == null ||
                item.quantity() == null ||
                item.quantity().signum() <= 0
            ) {
                throw new IllegalArgumentException(
                    "Receipt items require an ID and a positive quantity."
                );
            }
            if (
                quantities.putIfAbsent(
                    item.purchaseItemId(),
                    item.quantity().stripTrailingZeros()
                ) != null
            ) {
                throw new IllegalArgumentException(
                    "Receipt cannot contain duplicate purchase items."
                );
            }
        }
        return quantities;
    }

    private void validateCounters(PurchaseItem item) {
        if (
            item.getBaseQuantity() == null ||
            item.getBaseQuantity().signum() <= 0 ||
            item.getReceivedQuantity() == null ||
            item.getReceivedQuantity().signum() < 0 ||
            item.getReceivedQuantity().compareTo(item.getBaseQuantity()) > 0
        ) {
            throw new IllegalArgumentException(
                "Purchase item has inconsistent received quantities."
            );
        }
    }

    public record ReceiptRegistration(PurchaseReceipt receipt, boolean created) {}
}
