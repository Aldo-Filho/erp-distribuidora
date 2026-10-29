package com.pi.erp.stock.reservation;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.Order;
import com.pi.erp.order.OrderStatus;
import com.pi.erp.order.item.OrderItem;
import com.pi.erp.order.item.OrderItemRepository;
import com.pi.erp.stock.StockItem;
import com.pi.erp.stock.StockItemRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockReservationService {

    @Autowired
    private StockReservationRepository repository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private EntityManager entityManager;

    // The coordinator must hold the order lock before calling these operations.
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can reserve stock.");
        }
        if (!repository.findByOrderId(order.getId()).isEmpty()) {
            throw new IllegalArgumentException("Order already has stock reservations.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot reserve stock for an empty order.");
        }

        Map<Long, Long> stockItemIds = new HashMap<>();
        Map<Long, Long> quantities = new TreeMap<>();

        for (OrderItem orderItem : orderItems) {
            validateOrderItem(orderItem);
            Long stockItemId = stockItemRepository
                .findIdByWarehouseIdAndProductId(
                    orderItem.getWarehouse().getId(), orderItem.getProduct().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Order stock item not found."));

            stockItemIds.put(orderItem.getId(), stockItemId);
            quantities.merge(stockItemId, orderItem.getQuantity().longValue(), Long::sum);
        }

        Map<Long, StockItem> stockItems = lockStockItems(quantities.keySet());

        for (OrderItem orderItem : orderItems) {
            validateOrigin(orderItem, stockItems.get(stockItemIds.get(orderItem.getId())));
        }
        for (Map.Entry<Long, Long> entry : quantities.entrySet()) {
            StockItem stockItem = stockItems.get(entry.getKey());
            long availableQuantity = stockItem.getQuantity().longValue()
                - stockItem.getReservedQuantity().longValue();

            if (entry.getValue() > availableQuantity) {
                throw new IllegalArgumentException("Insufficient available stock for the order.");
            }
        }

        for (Map.Entry<Long, Long> entry : quantities.entrySet()) {
            StockItem stockItem = stockItems.get(entry.getKey());
            long reservedQuantity = stockItem.getReservedQuantity().longValue() + entry.getValue();
            stockItem.setReservedQuantity((int) reservedQuantity);
            stockItemRepository.save(stockItem);
        }
        for (OrderItem orderItem : orderItems) {
            StockItem stockItem = stockItems.get(stockItemIds.get(orderItem.getId()));
            repository.save(new StockReservation(orderItem, stockItem));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Order order) {
        if (order.getStatus() == OrderStatus.PENDING) {
            if (!repository.findByOrderId(order.getId()).isEmpty()) {
                throw new IllegalArgumentException("Pending order has unexpected stock reservations.");
            }
            return;
        }

        List<StockReservation> reservations = findActiveReservations(order);

        for (StockReservation reservation : reservations) {
            StockItem stockItem = reservation.getStockItem();
            long reservedQuantity = stockItem.getReservedQuantity().longValue()
                - reservation.getQuantity().longValue();

            if (reservedQuantity < 0) {
                throw new IllegalArgumentException("Stock item has inconsistent reservations.");
            }

            stockItem.setReservedQuantity((int) reservedQuantity);
            reservation.setStatus(StockReservationStatus.RELEASED);
            stockItemRepository.save(stockItem);
            repository.save(reservation);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<StockReservation> prepareDispatch(Order order) {
        return findActiveReservations(order);
    }

    private List<StockReservation> findActiveReservations(Order order) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Order must be confirmed to process reservations.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<StockReservation> reservations = repository.findByOrderId(order.getId());

        if (orderItems.isEmpty() || reservations.size() != orderItems.size()) {
            throw new IllegalArgumentException("Order has incomplete stock reservations.");
        }

        Set<Long> orderItemIds = new HashSet<>();
        Set<Long> stockItemIds = new HashSet<>();

        for (OrderItem orderItem : orderItems) {
            validateOrderItem(orderItem);
            orderItemIds.add(orderItem.getId());
        }
        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() != StockReservationStatus.ACTIVE
                || !orderItemIds.remove(reservation.getOrderItem().getId())
                || !reservation.getOrderItem().getQuantity().equals(reservation.getQuantity())) {
                throw new IllegalArgumentException("Order has inconsistent stock reservations.");
            }
            stockItemIds.add(reservation.getStockItem().getId());
        }

        Map<Long, StockItem> stockItems = lockStockItems(stockItemIds);

        for (StockReservation reservation : reservations) {
            validateOrigin(reservation.getOrderItem(), stockItems.get(reservation.getStockItem().getId()));
        }

        reservations.sort(
            Comparator.comparing((StockReservation reservation) -> reservation.getStockItem().getId())
                .thenComparing(StockReservation::getId)
        );
        return reservations;
    }

    private Map<Long, StockItem> lockStockItems(Set<Long> stockItemIds) {
        repository.flush();
        Map<Long, StockItem> stockItems = new TreeMap<>();

        for (Long stockItemId : stockItemIds.stream().sorted().toList()) {
            StockItem stockItem = stockItemRepository
                .findByIdForUpdate(stockItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

            entityManager.refresh(stockItem);
            validateBalance(stockItem);
            stockItems.put(stockItemId, stockItem);
        }

        return stockItems;
    }

    private void validateBalance(StockItem stockItem) {
        Integer quantity = stockItem.getQuantity();
        Integer reservedQuantity = stockItem.getReservedQuantity();

        if (quantity == null || reservedQuantity == null || quantity < 0
            || reservedQuantity < 0 || reservedQuantity > quantity) {
            throw new IllegalArgumentException("Stock item has inconsistent balances.");
        }
        if (reservedQuantity.longValue() != repository.sumActiveQuantityByStockItemId(stockItem.getId())) {
            throw new IllegalArgumentException("Stock item has inconsistent reservations.");
        }
    }

    private void validateOrderItem(OrderItem orderItem) {
        if (orderItem.getWarehouse() == null || orderItem.getProduct() == null
            || orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Order item has invalid stock data.");
        }
    }

    private void validateOrigin(OrderItem orderItem, StockItem stockItem) {
        if (!stockItem.getWarehouse().getId().equals(orderItem.getWarehouse().getId())
            || !stockItem.getProduct().getId().equals(orderItem.getProduct().getId())) {
            throw new IllegalArgumentException("Stock item no longer matches the order item origin.");
        }
    }
}
