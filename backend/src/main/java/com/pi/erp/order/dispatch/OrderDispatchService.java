package com.pi.erp.order.dispatch;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.Order;
import com.pi.erp.order.OrderRepository;
import com.pi.erp.order.OrderStatus;
import com.pi.erp.stock.movement.StockMovementService;
import com.pi.erp.stock.reservation.StockReservation;
import com.pi.erp.stock.reservation.StockReservationService;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderDispatchService {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public Order dispatch(Long id, RequestOrderDispatchDTO data) {
        CurrentUser.require("orders.dispatch");
        Long operatorUserId = CurrentUser.operator(data.operatorUserId());
        Order order = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        entityManager.refresh(order);

        if (order.getStatus() == OrderStatus.INVOICED) {
            return order;
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed orders can be dispatched.");
        }

        User user = userRepository
            .findById(operatorUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Operator user not found."));

        entityManager.refresh(user);

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("Operator user must be active.");
        }

        List<StockReservation> reservations = stockReservationService.prepareDispatch(order);
        String notes = data.notes() == null || data.notes().isBlank() ? null : data.notes().trim();

        for (StockReservation reservation : reservations) {
            stockMovementService.registerSale(reservation, user.getId(), notes);
        }

        order.setStatus(OrderStatus.INVOICED);
        return repository.save(order);
    }
}
