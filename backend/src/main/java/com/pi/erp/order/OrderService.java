package com.pi.erp.order;

import com.pi.erp.customer.Customer;
import com.pi.erp.customer.CustomerRepository;
import com.pi.erp.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    public List<Order> search(OrderFilter filter) {
        Specification<Order> spec = Specification.allOf();

        if (filter.paymentMethod() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("paymentMethod"), filter.paymentMethod())
            );
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), filter.status())
            );
        }
        if (filter.customerId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("customer").get("id"), filter.customerId())
            );
        }
        if (filter.userId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("userId"), filter.userId())
            );
        }

        return repository.findAll(spec);
    }

    public Order register(RequestOrderDTO data) {
        if (data.status() == null) {
            throw new IllegalArgumentException("Order status is required.");
        }

        Customer customer = findCustomer(data.customerId());
        Order order = new Order(data, customer);

        return repository.save(order);
    }

    public Order update(Long id, PatchOrderDTO data) {
        Order order = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        Customer customer = order.getCustomer();

        if (data.customerId() != null) {
            customer = findCustomer(data.customerId());
        }

        if (data.totalAmount() != null) {
            order.setTotalAmount(data.totalAmount());
        }
        if (data.paymentMethod() != null) {
            order.setPaymentMethod(data.paymentMethod());
        }
        if (data.status() != null) {
            order.setStatus(data.status());
        }
        if (data.customerId() != null) {
            order.setCustomer(customer);
        }
        if (data.userId() != null) {
            order.setUserId(data.userId());
        }

        return repository.save(order);
    }

    public void delete(Long id) {
        Order order = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        repository.delete(order);
    }

    private Customer findCustomer(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required.");
        }

        return customerRepository
            .findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    }
}
