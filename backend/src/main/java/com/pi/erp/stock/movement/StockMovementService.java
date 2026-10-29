package com.pi.erp.stock.movement;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.OrderStatus;
import com.pi.erp.order.item.OrderItem;
import com.pi.erp.purchase.PurchaseStatus;
import com.pi.erp.purchase.item.PurchaseItem;
import com.pi.erp.purchase.receipt.PurchaseReceiptItem;
import com.pi.erp.stock.StockItem;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.stock.reservation.StockReservation;
import com.pi.erp.stock.reservation.StockReservationRepository;
import com.pi.erp.stock.reservation.StockReservationStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
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
    private EntityManager entityManager;

    public List<StockMovement> search(StockMovementFilter filter) {
        if (filter.occurredFrom() != null && filter.occurredTo() != null
            && filter.occurredFrom().isAfter(filter.occurredTo())) {
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
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("type"), filter.type())
            );
        }
        if (filter.reason() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("reason"), filter.reason())
            );
        }
        if (filter.orderId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("orderId"), filter.orderId())
            );
        }
        if (filter.purchaseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("purchaseId"), filter.purchaseId())
            );
        }
        if (filter.userId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("userId"), filter.userId())
            );
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
    public StockMovement register(RequestStockMovementDTO data) {
        CurrentUser.require("stock.adjust");
        Long operatorUserId = CurrentUser.operator(data.userId());
        validateMovement(data.type(), data.reason(), data.quantity());
        validateUser(operatorUserId);

        StockItem stockItem = findStockItem(data.stockItemId());
        Integer previousBalance = stockItem.getQuantity();
        Integer newBalance = calculateNewBalance(stockItem, data.type(), data.quantity());
        String notes = data.notes();

        if (notes != null) {
            notes = notes.isBlank() ? null : notes.trim();
        }

        StockMovement stockMovement = new StockMovement(
            new RequestStockMovementDTO(
                data.stockItemId(), data.type(), data.reason(), data.quantity(), operatorUserId, data.notes()
            ),
            stockItem,
            previousBalance,
            newBalance,
            notes
        );

        stockItem.setQuantity(newBalance);
        stockItemRepository.save(stockItem);

        return repository.save(stockMovement);
    }

    // Called only by the coordinator with the order and all stock items already locked.
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement registerSale(
        StockReservation reservation,
        Long operatorUserId,
        String notes
    ) {
        if (reservation == null || reservation.getId() == null
            || !entityManager.contains(reservation)
            || reservation.getStatus() != StockReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("An active persisted stock reservation is required.");
        }

        OrderItem orderItem = reservation.getOrderItem();
        StockItem stockItem = reservation.getStockItem();
        Integer quantity = reservation.getQuantity();

        if (orderItem.getOrder().getStatus() != OrderStatus.CONFIRMED
            || quantity == null || quantity <= 0 || !quantity.equals(orderItem.getQuantity())
            || !stockItem.getProduct().getId().equals(orderItem.getProduct().getId())
            || orderItem.getWarehouse() == null
            || !stockItem.getWarehouse().getId().equals(orderItem.getWarehouse().getId())) {
            throw new IllegalArgumentException("Reservation does not match a confirmed order item.");
        }

        validateUser(operatorUserId);
        stockReservationRepository.flush();

        if (repository.existsByStockReservationId(reservation.getId())) {
            throw new IllegalArgumentException("Stock reservation already has a sale movement.");
        }

        validateStockBalance(stockItem);
        Integer previousBalance = stockItem.getQuantity();
        long newBalance = previousBalance.longValue() - quantity.longValue();
        long reservedQuantity = stockItem.getReservedQuantity().longValue() - quantity.longValue();

        if (reservedQuantity < 0 || newBalance < reservedQuantity) {
            throw new IllegalArgumentException("Stock reservation cannot be consumed from this balance.");
        }

        String movementNotes = notes == null || notes.isBlank() ? null : notes.trim();
        StockMovement stockMovement = new StockMovement(
            reservation, operatorUserId, previousBalance, (int) newBalance, movementNotes
        );

        stockItem.setQuantity((int) newBalance);
        stockItem.setReservedQuantity((int) reservedQuantity);
        reservation.setStatus(StockReservationStatus.CONSUMED);
        stockItemRepository.save(stockItem);
        stockReservationRepository.save(reservation);

        return repository.save(stockMovement);
    }

    // Called only by the receipt coordinator with purchase, items and stocks already locked.
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement registerPurchase(PurchaseReceiptItem receiptItem) {
        if (receiptItem == null || receiptItem.getId() == null
            || !entityManager.contains(receiptItem) || receiptItem.getReceipt() == null
            || receiptItem.getReceipt().getId() == null) {
            throw new IllegalArgumentException("A persisted purchase receipt item is required.");
        }

        PurchaseItem purchaseItem = receiptItem.getPurchaseItem();
        StockItem stockItem = receiptItem.getStockItem();
        Integer quantity = receiptItem.getQuantity();
        PurchaseStatus status = receiptItem.getReceipt().getPurchase().getStatus();

        if ((status != PurchaseStatus.CONFIRMED && status != PurchaseStatus.PARTIALLY_RECEIVED)
            || !purchaseItem.getPurchase().getId().equals(receiptItem.getReceipt().getPurchase().getId())
            || quantity == null || quantity <= 0
            || purchaseItem.getReceivedQuantity() == null || purchaseItem.getReceivedQuantity() < 0
            || purchaseItem.getQuantity() == null
            || quantity > purchaseItem.getQuantity() - purchaseItem.getReceivedQuantity()
            || !stockItem.getProduct().getId().equals(purchaseItem.getProduct().getId())
            || !stockItem.getWarehouse().getId().equals(purchaseItem.getWarehouse().getId())) {
            throw new IllegalArgumentException("Receipt does not match an outstanding purchase item.");
        }

        validateUser(receiptItem.getReceipt().getOperator().getId());
        repository.flush();
        if (repository.existsByPurchaseReceiptItemId(receiptItem.getId())) {
            throw new IllegalArgumentException("Receipt item already has a purchase movement.");
        }

        Integer previousBalance = stockItem.getQuantity();
        Integer newBalance = calculateNewBalance(stockItem, StockMovementType.INBOUND, quantity);
        StockMovement stockMovement = new StockMovement(receiptItem, previousBalance, newBalance);
        stockItem.setQuantity(newBalance);
        stockItemRepository.save(stockItem);
        return repository.save(stockMovement);
    }

    private StockItem findStockItem(Long stockItemId) {
        if (stockItemId == null) {
            throw new IllegalArgumentException("Stock item ID is required.");
        }

        StockItem stockItem = stockItemRepository
            .findByIdForUpdate(stockItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

        entityManager.refresh(stockItem);
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
        Integer quantity
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
            if (quantity == 0) {
                throw new IllegalArgumentException("Adjustment quantity cannot be zero.");
            }
            if (reason != StockMovementReason.MANUAL_ADJUSTMENT) {
                throw new IllegalArgumentException("Adjustment requires a manual adjustment reason.");
            }
        } else {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Inbound and outbound quantities must be positive.");
            }
            if (reason != StockMovementReason.RETURN
                && reason != StockMovementReason.MANUAL_ADJUSTMENT
                && (type != StockMovementType.OUTBOUND || reason != StockMovementReason.LOSS)) {
                throw new IllegalArgumentException("Movement reason is not allowed for this type.");
            }
        }
    }

    private Integer calculateNewBalance(
        StockItem stockItem,
        StockMovementType type,
        Integer quantity
    ) {
        stockReservationRepository.flush();
        validateStockBalance(stockItem);
        Integer previousBalance = stockItem.getQuantity();
        Integer reservedQuantity = stockItem.getReservedQuantity();

        long newBalance = previousBalance.longValue();

        if (type == StockMovementType.OUTBOUND) {
            newBalance -= quantity.longValue();
        } else {
            newBalance += quantity.longValue();
        }

        if (newBalance < 0) {
            throw new IllegalArgumentException("New balance cannot be negative.");
        }
        if (newBalance > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("New balance exceeds the supported quantity.");
        }
        if (newBalance < reservedQuantity) {
            throw new IllegalArgumentException("New balance cannot be less than reserved quantity.");
        }

        return (int) newBalance;
    }

    private void validateStockBalance(StockItem stockItem) {
        Integer quantity = stockItem.getQuantity();
        Integer reservedQuantity = stockItem.getReservedQuantity();

        if (quantity == null || reservedQuantity == null || quantity < 0
            || reservedQuantity < 0 || reservedQuantity > quantity) {
            throw new IllegalArgumentException("Stock item has inconsistent balances.");
        }
        if (reservedQuantity.longValue()
            != stockReservationRepository.sumActiveQuantityByStockItemId(stockItem.getId())) {
            throw new IllegalArgumentException("Stock item has inconsistent reservations.");
        }
    }
}
