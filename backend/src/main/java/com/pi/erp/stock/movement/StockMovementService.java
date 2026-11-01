package com.pi.erp.stock.movement;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.OrderStatus;
import com.pi.erp.order.item.OrderItem;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.purchase.PurchaseStatus;
import com.pi.erp.purchase.item.PurchaseItem;
import com.pi.erp.purchase.receipt.PurchaseReceiptItem;
import com.pi.erp.stock.StockItem;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.stock.reservation.StockReservation;
import com.pi.erp.stock.reservation.StockReservationRepository;
import com.pi.erp.stock.reservation.StockReservationStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

    @Autowired
    private StockMovementRepository repository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private EntityManager entityManager;

    public List<StockMovement> search(StockMovementFilter filter) {
        if (
            filter.occurredFrom() != null &&
                filter.occurredTo() != null &&
                filter.occurredFrom().isAfter(filter.occurredTo())
        ) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        Specification<StockMovement> spec = Specification.allOf();

        if (filter.stockItemId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("stockItem").get("id"), filter.stockItemId())
            );
        }
        if (filter.warehouseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("stockItem").get("warehouse").get("id"), filter.warehouseId())
            );
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("stockItem").get("product").get("id"), filter.productId())
            );
        }
        if (filter.type() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (filter.reason() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reason"), filter.reason()));
        }
        if (filter.orderId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("orderId"), filter.orderId()));
        }
        if (filter.purchaseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("purchaseId"), filter.purchaseId())
            );
        }
        if (filter.userId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), filter.userId()));
        }
        if (filter.occurredFrom() != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("occurredAt"), filter.occurredFrom())
            );
        }
        if (filter.occurredTo() != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("occurredAt"), filter.occurredTo())
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public MovementRegistration register(RequestStockMovementDTO data) {
        CurrentUser.require("stock.adjust");
        Long operatorUserId = CurrentUser.operator(data.userId());
        if (data.requestId() == null) {
            throw new IllegalArgumentException("Request ID is required.");
        }

        // Coordinate the UUID across stock items and application instances until commit.
        // Hash collisions only serialize unrelated requests; equality uses the full UUID.
        repository.lockRequest(
            data.requestId().getMostSignificantBits() ^ data.requestId().getLeastSignificantBits()
        );
        String notes = data.notes() == null || data.notes().isBlank()
            ? null
            : data.notes().trim();

        StockMovement previous = repository.findByRequestId(data.requestId()).orElse(null);
        if (previous != null) {
            if (
                !previous.getStockItem().getId().equals(data.stockItemId()) ||
                    previous.getType() != data.type() ||
                    previous.getReason() != data.reason() ||
                    (data.quantity() == null || previous.getQuantity().compareTo(data.quantity()) != 0) ||
                    (
                        data.expectedQuantity() != null &&
                        previous.getPreviousBalance().compareTo(data.expectedQuantity()) != 0
                    ) ||
                    !previous.getUserId().equals(operatorUserId) ||
                    !Objects.equals(previous.getNotes(), notes)
            ) {
                throw new IllegalArgumentException(
                    "Request ID already belongs to a different stock movement."
                );
            }
            return new MovementRegistration(previous, false);
        }

        StockItem stockItem = findStockItem(data.stockItemId());
        if (data.expectedQuantity() != null) {
            if (data.type() != StockMovementType.ADJUSTMENT) {
                throw new IllegalArgumentException("Expected quantity is only allowed for adjustments.");
            }
            ProductQuantity.requireBase(
                data.expectedQuantity(),
                stockItem.getProduct().getUnit(),
                true
            );
            if (stockItem.getQuantity().compareTo(data.expectedQuantity()) != 0) {
                throw new IllegalArgumentException(
                    "Stock balance changed. Refresh the balance before correcting it."
                );
            }
        }
        validateMovement(data.type(), data.reason(), data.quantity());
        validateUser(operatorUserId);
        if (data.type() == StockMovementType.ADJUSTMENT) {
            ProductQuantity.requireSignedBase(data.quantity(), stockItem.getProduct().getUnit());
        } else {
            ProductQuantity.requireBase(data.quantity(), stockItem.getProduct().getUnit(), false);
        }
        BigDecimal previousBalance = stockItem.getQuantity();
        BigDecimal newBalance = calculateNewBalance(stockItem, data.type(), data.quantity());

        StockMovement stockMovement = new StockMovement(
            new RequestStockMovementDTO(
                data.requestId(),
                data.stockItemId(),
                data.type(),
                data.reason(),
                data.quantity(),
                operatorUserId,
                data.notes(),
                data.expectedQuantity()
            ),
            stockItem,
            previousBalance,
            newBalance,
            notes
        );

        stockItem.setQuantity(newBalance);
        stockItemRepository.save(stockItem);

        return new MovementRegistration(repository.save(stockMovement), true);
    }

    public record MovementRegistration(StockMovement movement, boolean created) {}

    // Called only by the coordinator with the order and all stock items already locked.
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement registerSale(
        StockReservation reservation,
        Long operatorUserId,
        String notes
    ) {
        if (
            reservation == null ||
                reservation.getId() == null ||
                !entityManager.contains(reservation) ||
                reservation.getStatus() != StockReservationStatus.ACTIVE
        ) {
            throw new IllegalArgumentException(
                "An active persisted stock reservation is required."
            );
        }

        OrderItem orderItem = reservation.getOrderItem();
        StockItem stockItem = reservation.getStockItem();
        BigDecimal quantity = reservation.getQuantity();

        if (
            orderItem.getOrder().getStatus() != OrderStatus.CONFIRMED ||
                quantity == null ||
                quantity.signum() <= 0 ||
                quantity.compareTo(orderItem.getBaseQuantity()) != 0 ||
                !stockItem.getProduct().getId().equals(orderItem.getProduct().getId()) ||
                orderItem.getWarehouse() == null ||
                !stockItem.getWarehouse().getId().equals(orderItem.getWarehouse().getId())
        ) {
            throw new IllegalArgumentException(
                "Reservation does not match a confirmed order item."
            );
        }

        validateUser(operatorUserId);
        stockReservationRepository.flush();

        if (repository.existsByStockReservationId(reservation.getId())) {
            throw new IllegalArgumentException("Stock reservation already has a sale movement.");
        }

        validateStockBalance(stockItem);
        BigDecimal previousBalance = stockItem.getQuantity();
        BigDecimal newBalance = previousBalance.subtract(quantity);
        BigDecimal reservedQuantity = stockItem.getReservedQuantity().subtract(quantity);

        if (reservedQuantity.signum() < 0 || newBalance.compareTo(reservedQuantity) < 0) {
            throw new IllegalArgumentException(
                "Stock reservation cannot be consumed from this balance."
            );
        }

        String movementNotes = notes == null || notes.isBlank() ? null : notes.trim();
        StockMovement stockMovement = new StockMovement(
            reservation,
            operatorUserId,
            previousBalance,
            newBalance,
            movementNotes
        );

        stockItem.setQuantity(newBalance);
        stockItem.setReservedQuantity(reservedQuantity);
        reservation.setStatus(StockReservationStatus.CONSUMED);
        stockItemRepository.save(stockItem);
        stockReservationRepository.save(reservation);

        return repository.save(stockMovement);
    }

    // Called only by the receipt coordinator with purchase, items and stocks already locked.
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement registerPurchase(PurchaseReceiptItem receiptItem) {
        if (
            receiptItem == null ||
                receiptItem.getId() == null ||
                !entityManager.contains(receiptItem) ||
                receiptItem.getReceipt() == null ||
                receiptItem.getReceipt().getId() == null
        ) {
            throw new IllegalArgumentException("A persisted purchase receipt item is required.");
        }

        PurchaseItem purchaseItem = receiptItem.getPurchaseItem();
        StockItem stockItem = receiptItem.getStockItem();
        BigDecimal quantity = receiptItem.getQuantity();
        PurchaseStatus status = receiptItem.getReceipt().getPurchase().getStatus();

        if (
            (status != PurchaseStatus.CONFIRMED && status != PurchaseStatus.PARTIALLY_RECEIVED) ||
                !purchaseItem
                    .getPurchase()
                    .getId()
                    .equals(receiptItem.getReceipt().getPurchase().getId()) ||
                quantity == null ||
                quantity.signum() <= 0 ||
                purchaseItem.getReceivedQuantity() == null ||
                purchaseItem.getReceivedQuantity().signum() < 0 ||
                purchaseItem.getBaseQuantity() == null ||
                quantity.compareTo(
                    purchaseItem.getBaseQuantity().subtract(purchaseItem.getReceivedQuantity())
                ) > 0 ||
                !stockItem.getProduct().getId().equals(purchaseItem.getProduct().getId()) ||
                !stockItem.getWarehouse().getId().equals(purchaseItem.getWarehouse().getId())
        ) {
            throw new IllegalArgumentException(
                "Receipt does not match an outstanding purchase item."
            );
        }

        validateUser(receiptItem.getReceipt().getOperator().getId());
        repository.flush();
        if (repository.existsByPurchaseReceiptItemId(receiptItem.getId())) {
            throw new IllegalArgumentException("Receipt item already has a purchase movement.");
        }

        BigDecimal previousBalance = stockItem.getQuantity();
        BigDecimal newBalance = calculateNewBalance(stockItem, StockMovementType.INBOUND, quantity);
        StockMovement stockMovement = new StockMovement(receiptItem, previousBalance, newBalance);
        stockItem.setQuantity(newBalance);
        stockItemRepository.save(stockItem);
        return repository.save(stockMovement);
    }

    private StockItem findStockItem(Long stockItemId) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("Stock item ID is required.");
        }

        StockItem observed = stockItemRepository
            .findById(stockItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));
        Long originalProductId = observed.getProduct().getId();
        productCatalogService.lockProduct(originalProductId);
        StockItem stockItem = stockItemRepository
            .findByIdForUpdate(stockItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

        entityManager.refresh(stockItem);
        if (!stockItem.getProduct().getId().equals(originalProductId)) {
            throw new IllegalArgumentException("Stock item changed. Retry the operation.");
        }
        return stockItem;
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        Boolean active = repository
            .findUserActiveById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (!Boolean.TRUE.equals(active)) {
            throw new IllegalArgumentException("User must be active.");
        }
    }

    private void validateMovement(
        StockMovementType type,
        StockMovementReason reason,
        BigDecimal quantity
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Movement type is required.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Movement reason is required.");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required.");
        }
        if (type == StockMovementType.ADJUSTMENT) {
            if (quantity.signum() == 0) {
                throw new IllegalArgumentException("Adjustment quantity cannot be zero.");
            }
            if (reason != StockMovementReason.MANUAL_ADJUSTMENT) {
                throw new IllegalArgumentException(
                    "Adjustment requires a manual adjustment reason."
                );
            }
        } else {
            if (quantity.signum() <= 0) {
                throw new IllegalArgumentException(
                    "Inbound and outbound quantities must be positive."
                );
            }
            if (
                reason != StockMovementReason.RETURN &&
                    reason != StockMovementReason.MANUAL_ADJUSTMENT &&
                    (type != StockMovementType.OUTBOUND || reason != StockMovementReason.LOSS)
            ) {
                throw new IllegalArgumentException("Movement reason is not allowed for this type.");
            }
        }
    }

    private BigDecimal calculateNewBalance(
        StockItem stockItem,
        StockMovementType type,
        BigDecimal quantity
    ) {
        stockReservationRepository.flush();
        validateStockBalance(stockItem);
        BigDecimal previousBalance = stockItem.getQuantity();
        BigDecimal reservedQuantity = stockItem.getReservedQuantity();

        BigDecimal newBalance = previousBalance;

        if (type == StockMovementType.OUTBOUND) {
            newBalance = newBalance.subtract(quantity);
        } else {
            newBalance = newBalance.add(quantity);
        }

        if (newBalance.signum() < 0) {
            throw new IllegalArgumentException("New balance cannot be negative.");
        }
        ProductQuantity.requireBase(newBalance, stockItem.getProduct().getUnit(), true);
        if (newBalance.compareTo(reservedQuantity) < 0) {
            throw new IllegalArgumentException(
                "New balance cannot be less than reserved quantity."
            );
        }

        return newBalance;
    }

    private void validateStockBalance(StockItem stockItem) {
        BigDecimal quantity = stockItem.getQuantity();
        BigDecimal reservedQuantity = stockItem.getReservedQuantity();

        if (
            quantity == null ||
                reservedQuantity == null ||
                quantity.signum() < 0 ||
                reservedQuantity.signum() < 0 ||
                reservedQuantity.compareTo(quantity) > 0
        ) {
            throw new IllegalArgumentException("Stock item has inconsistent balances.");
        }
        if (
            reservedQuantity.compareTo(
                stockReservationRepository.sumActiveQuantityByStockItemId(stockItem.getId())
            ) != 0
        ) {
            throw new IllegalArgumentException("Stock item has inconsistent reservations.");
        }
    }
}
