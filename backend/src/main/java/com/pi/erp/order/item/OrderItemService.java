package com.pi.erp.order.item;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.Order;
import com.pi.erp.order.OrderRepository;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository repository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<OrderItem> search(OrderItemFilter filter) {
        Specification<OrderItem> spec = Specification.allOf();

        if (filter.orderId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("order").get("id"), filter.orderId())
            );
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("id"), filter.productId())
            );
        }

        return repository.findAll(spec);
    }

    public OrderItem register(RequestOrderItemDTO data) {
        BigDecimal discount = data.discount() == null ? BigDecimal.ZERO : data.discount();
        validateValues(data.quantity(), data.unitPrice(), discount);

        Order order = findOrder(data.orderId());
        Product product = findProduct(data.productId());

        OrderItem orderItem = new OrderItem(data, order, product);
        return repository.save(orderItem);
    }

    public OrderItem update(Long id, PatchOrderItemDTO data) {
        OrderItem orderItem = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order item not found."));

        Integer quantity = data.quantity() != null ? data.quantity() : orderItem.getQuantity();
        BigDecimal unitPrice = data.unitPrice() != null ? data.unitPrice() : orderItem.getUnitPrice();
        BigDecimal discount = data.discount() != null ? data.discount() : orderItem.getDiscount();

        validateValues(quantity, unitPrice, discount);

        Order order = orderItem.getOrder();
        Product product = orderItem.getProduct();

        if (data.orderId() != null) {
            order = findOrder(data.orderId());
        }
        if (data.productId() != null) {
            product = findProduct(data.productId());
        }

        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setDiscount(discount);

        return repository.save(orderItem);
    }

    public void delete(Long id) {
        OrderItem orderItem = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order item not found."));

        repository.delete(orderItem);
    }

    private Order findOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        return orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private void validateValues(Integer quantity, BigDecimal unitPrice, BigDecimal discount) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price is required.");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        if (discount == null) {
            throw new IllegalArgumentException("Discount is required.");
        }
        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative.");
        }
    }
}
