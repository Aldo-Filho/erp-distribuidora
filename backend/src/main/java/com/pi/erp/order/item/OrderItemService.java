package com.pi.erp.order.item;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.Order;
import com.pi.erp.order.OrderRepository;
import com.pi.erp.order.OrderStatus;
import com.pi.erp.price.item.PriceTableItemRepository;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderItemService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    @Autowired
    private OrderItemRepository repository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceTableItemRepository priceTableItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private EntityManager entityManager;

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
        if (filter.warehouseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("warehouse").get("id"), filter.warehouseId())
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public OrderItem register(RequestOrderItemDTO data) {
        CurrentUser.require("orders.create");
        validatePricingPermissions(data.unitPrice(), data.discount(), data.discountType());
        BigDecimal discount = data.discount() == null ? BigDecimal.ZERO : data.discount();
        DiscountType discountType = data.discountType() == null
            ? DiscountType.AMOUNT
            : data.discountType();

        Order order = findOrder(data.orderId());
        Product product = findProduct(data.productId());
        validateActiveProduct(product);
        Warehouse warehouse = findWarehouse(data.warehouseId());
        validateStock(product, warehouse);

        BigDecimal unitPrice = data.unitPrice() != null
            ? data.unitPrice()
            : findPrice(order, product);
        validateValues(data.quantity(), unitPrice, discount, discountType);

        if (repository.existsByOrderIdAndProductId(order.getId(), product.getId())) {
            throw new IllegalArgumentException("This product already exists in this order.");
        }

        OrderItem orderItem = new OrderItem(data, order, product, warehouse, unitPrice);
        OrderItem savedOrderItem = repository.save(orderItem);
        repository.flush();
        recalculateTotal(order);

        return savedOrderItem;
    }

    @Transactional
    public OrderItem update(Long id, PatchOrderItemDTO data) {
        CurrentUser.require("orders.update");
        validatePricingPermissions(data.unitPrice(), data.discount(), data.discountType());
        OrderItem orderItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order item not found."));

        entityManager.refresh(orderItem);

        Integer quantity = data.quantity() != null ? data.quantity() : orderItem.getQuantity();
        BigDecimal discount = data.discount() != null ? data.discount() : orderItem.getDiscount();
        DiscountType discountType = data.discountType() != null
            ? data.discountType()
            : orderItem.getDiscountType();

        Long sourceOrderId = orderItem.getOrder().getId();
        Long orderId = data.orderId() != null ? data.orderId() : sourceOrderId;
        Order sourceOrder;
        Order order;

        // Lock both orders in ID order to avoid deadlocks between transfers.
        if (sourceOrderId.equals(orderId)) {
            sourceOrder = findOrder(sourceOrderId);
            order = sourceOrder;
        } else if (sourceOrderId < orderId) {
            sourceOrder = findOrder(sourceOrderId);
            order = findOrder(orderId);
        } else {
            order = findOrder(orderId);
            sourceOrder = findOrder(sourceOrderId);
        }

        Product product = orderItem.getProduct();

        if (data.productId() != null) {
            product = findProduct(data.productId());
        }

        Warehouse warehouse = orderItem.getWarehouse();

        if (data.warehouseId() != null) {
            warehouse = findWarehouse(data.warehouseId());
        }

        validateStock(product, warehouse);

        boolean hasProductChange = !product.getId().equals(orderItem.getProduct().getId());

        if (hasProductChange || !sourceOrderId.equals(orderId)) {
            validateActiveProduct(product);
        }

        BigDecimal unitPrice = orderItem.getUnitPrice();

        if (data.unitPrice() != null) {
            unitPrice = data.unitPrice();
        } else if (hasProductChange) {
            unitPrice = findPrice(order, product);
        }

        validateValues(quantity, unitPrice, discount, discountType);

        if (repository.existsByOrderIdAndProductIdAndIdNot(orderId, product.getId(), id)) {
            throw new IllegalArgumentException("This product already exists in this order.");
        }

        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setWarehouse(warehouse);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setDiscount(discount);
        orderItem.setDiscountType(discountType);

        OrderItem savedOrderItem = repository.save(orderItem);
        repository.flush();
        recalculateTotal(sourceOrder);

        if (!sourceOrderId.equals(orderId)) {
            recalculateTotal(order);
        }

        return savedOrderItem;
    }

    @Transactional
    public void delete(Long id) {
        OrderItem orderItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order item not found."));

        entityManager.refresh(orderItem);
        Order order = findOrder(orderItem.getOrder().getId());

        repository.delete(orderItem);
        repository.flush();
        recalculateTotal(order);
    }

    private void validatePricingPermissions(BigDecimal unitPrice, BigDecimal discount, DiscountType discountType) {
        if (unitPrice != null) {
            CurrentUser.require("orders.manualPrice");
        }
        if (discount != null || discountType != null) {
            CurrentUser.require("orders.discount");
        }
    }

    private Order findOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        Order order = orderRepository
            .findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        // The item's eager association may have loaded this order before the lock.
        entityManager.refresh(order);
        validateEditableOrder(order);
        return order;
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private Warehouse findWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required.");
        }

        return warehouseRepository
            .findById(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
    }

    private void validateStock(Product product, Warehouse warehouse) {
        if (warehouse == null) {
            throw new IllegalArgumentException("Order item warehouse is required.");
        }
        if (!stockItemRepository.existsByWarehouseIdAndProductId(warehouse.getId(), product.getId())) {
            throw new IllegalArgumentException("Product has no stock item in the selected warehouse.");
        }
    }

    private void recalculateTotal(Order order) {
        BigDecimal totalAmount = repository.calculateTotalByOrderId(order.getId());
        validateDecimal(totalAmount, "Order total");

        order.setTotalAmount(totalAmount);
        orderRepository.save(order);
    }

    private BigDecimal findPrice(Order order, Product product) {
        return priceTableItemRepository
            .findApplicablePrice(order.getCustomer().getId(), product.getId(), LocalDate.now())
            .orElse(product.getPrice());
    }

    private void validateEditableOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can have their items modified.");
        }
    }

    private void validateActiveProduct(Product product) {
        entityManager.refresh(product);

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product must be active.");
        }
    }

    private void validateValues(
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        DiscountType discountType
    ) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        validateDecimal(unitPrice, "Unit price");
        validateDecimal(discount, "Discount");

        if (discountType == null) {
            throw new IllegalArgumentException("Discount type is required.");
        }
        if (discountType == DiscountType.PERCENTAGE) {
            if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Discount percentage cannot exceed 100.");
            }
        } else if (discount.compareTo(unitPrice.multiply(BigDecimal.valueOf(quantity))) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the item gross amount.");
        }
    }

    private void validateDecimal(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " cannot be negative.");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(field + " cannot exceed 9999999999.99.");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(field + " must have at most two decimal places.");
        }
    }
}
