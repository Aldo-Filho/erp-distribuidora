package com.pi.erp.order;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.customer.Customer;
import com.pi.erp.customer.CustomerRepository;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.item.DiscountType;
import com.pi.erp.order.item.OrderItem;
import com.pi.erp.order.item.OrderItemRepository;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.product.presentation.ProductPresentationService;
import com.pi.erp.product.price.ProductPriceService;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.stock.reservation.StockReservationService;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private ProductPresentationService productPresentationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPriceService productPriceService;

    @Autowired
    private EntityManager entityManager;

    public Page<Order> search(OrderFilter filter, Pageable pageable) {
        Specification<Order> spec = Specification.allOf();

        if (filter.paymentMethod() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("paymentMethod"), filter.paymentMethod())
            );
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.customerId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("customer").get("id"), filter.customerId())
            );
        }
        if (filter.userId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("user").get("id"), filter.userId())
            );
        }

        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal resolvePrice(Long id, Long productId, Long presentationId) {
        Order order = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        validateEditableOrder(order);
        Product product = productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product must be active.");
        }
        var presentation = productPresentationService.findForProduct(productId, presentationId);
        return productPriceService.resolve(
            order.getCustomer().getId(),
            product,
            presentation,
            LocalDate.now()
        );
    }

    @Transactional
    public Order register(RequestOrderDTO data) {
        Customer customer = findCustomer(data.customerId());
        validateActiveCustomer(customer);
        User user = findUser(data.userId());
        Order order = new Order(data, customer, user);

        return repository.save(order);
    }

    @Transactional
    public Order update(Long id, PatchOrderDTO data) {
        Order order = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        entityManager.refresh(order);

        OrderStatus status = data.status() != null ? data.status() : order.getStatus();
        validateStatusTransition(order.getStatus(), status);
        if (order.getStatus() != status && status == OrderStatus.CONFIRMED) {
            CurrentUser.require("orders.confirm");
        } else if (order.getStatus() != status && status == OrderStatus.CANCELED) {
            CurrentUser.require("orders.cancel");
        }

        boolean hasCommercialChanges =
            data.customerId() != null || data.paymentMethod() != null || data.userId() != null;

        if (hasCommercialChanges) {
            validateEditableOrder(order);

            if (status == OrderStatus.CANCELED) {
                throw new IllegalArgumentException(
                    "Cannot cancel an order and change its commercial data at the same time."
                );
            }
        }

        Customer customer = order.getCustomer();

        if (data.customerId() != null) {
            customer = findCustomer(data.customerId());
        }

        if (order.getStatus() != status && status == OrderStatus.CONFIRMED) {
            validateConfirmation(order, customer);
        } else if (data.customerId() != null) {
            validateActiveCustomer(customer);
        }

        User user = order.getUser();

        if (data.userId() != null) {
            user = findUser(data.userId());
        }

        if (order.getStatus() != status) {
            if (status == OrderStatus.CONFIRMED) {
                stockReservationService.reserve(order);
            } else if (status == OrderStatus.CANCELED) {
                stockReservationService.release(order);
            }
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
            order.setUser(user);
        }

        return repository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        Order order = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        entityManager.refresh(order);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be deleted.");
        }
        if (orderItemRepository.existsByOrderId(id)) {
            throw new IllegalArgumentException("Cannot delete an order with items.");
        }

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

    private User findUser(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        validateActiveUser(user);
        return user;
    }

    private void validateActiveUser(User user) {
        entityManager.refresh(user);

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("User must be active.");
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus status) {
        if (currentStatus == status) {
            return;
        }
        if (status == OrderStatus.INVOICED) {
            throw new IllegalArgumentException(
                "Use the commercial dispatch operation to complete an order."
            );
        }
        if (
            currentStatus == OrderStatus.PENDING &&
                (status == OrderStatus.CONFIRMED || status == OrderStatus.CANCELED)
        ) {
            return;
        }
        if (currentStatus == OrderStatus.CONFIRMED && status == OrderStatus.CANCELED) {
            return;
        }

        throw new IllegalArgumentException("Invalid order status transition.");
    }

    private void validateEditableOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be edited.");
        }
    }

    private void validateActiveCustomer(Customer customer) {
        entityManager.refresh(customer);

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new IllegalArgumentException("Customer must be active.");
        }
    }

    private void validateConfirmation(Order order, Customer customer) {
        validateActiveCustomer(customer);
        orderItemRepository.flush();

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException(
                "An order must have at least one item to be confirmed."
            );
        }

        Set<String> productIds = new HashSet<>();
        productCatalogService.lockProducts(orderItems.stream().map(OrderItem::getProduct).toList());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            entityManager.refresh(product);

            if (!product.isActive()) {
                throw new IllegalArgumentException("All order products must be active.");
            }
            if (orderItem.getWarehouse() == null) {
                throw new IllegalArgumentException("All order items must have a warehouse.");
            }
            if (
                !stockItemRepository.existsByWarehouseIdAndProductId(
                    orderItem.getWarehouse().getId(),
                    product.getId()
                )
            ) {
                throw new IllegalArgumentException(
                    "An order product has no stock item in its selected warehouse."
                );
            }

            if (
                !productIds.add(
                    product.getId() +
                        ":" +
                        orderItem.getWarehouse().getId() +
                        ":" +
                        (orderItem.getPresentation() == null
                            ? "base"
                            : orderItem.getPresentation().getId())
                )
            ) {
                throw new IllegalArgumentException("An order cannot contain duplicate products.");
            }

            if (orderItem.getPresentation() != null) {
                productPresentationService.findForProduct(
                    product.getId(),
                    orderItem.getPresentation().getId()
                );
            }
            validateOrderItem(orderItem);
            BigDecimal gross = orderItem
                .getUnitPrice()
                .multiply(orderItem.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal discount =
                orderItem.getDiscountType() == DiscountType.PERCENTAGE
                    ? gross
                    .multiply(orderItem.getDiscount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : orderItem.getDiscount();
            totalAmount = totalAmount.add(gross.subtract(discount));
        }
        ProductCatalogService.validateMoney(totalAmount);
        if (order.getTotalAmount() == null || totalAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                "Order total is inconsistent with its contracted items."
            );
        }
    }

    private void validateOrderItem(OrderItem orderItem) {
        if (orderItem.getQuantity() == null || orderItem.getQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Order item quantity must be greater than zero.");
        }

        ProductQuantity.requireBase(BigDecimal.ONE, orderItem.getProduct().getUnit(), false);
        var unit =
            orderItem.getUnitSnapshot() != null
                ? orderItem.getUnitSnapshot()
                : orderItem.getProduct().getUnit();
        BigDecimal expectedBase = ProductQuantity.toBase(
            orderItem.getQuantity(),
            orderItem.getBaseFactorSnapshot(),
            unit,
            orderItem.getPresentation() == null || orderItem.getPresentation().isBasePresentation()
        );
        if (
            orderItem.getBaseQuantity() == null ||
                expectedBase.compareTo(orderItem.getBaseQuantity()) != 0
        ) {
            throw new IllegalArgumentException(
                "Order item base quantity is inconsistent with its contract."
            );
        }
        BigDecimal unitPrice = orderItem.getUnitPrice();
        BigDecimal discount = orderItem.getDiscount();
        DiscountType discountType = orderItem.getDiscountType();

        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Order item unit price must be non-negative.");
        }
        if (discount == null || discount.signum() < 0) {
            throw new IllegalArgumentException("Order item discount must be non-negative.");
        }
        if (discountType == null) {
            throw new IllegalArgumentException("Order item discount type is required.");
        }
        ProductCatalogService.validateMoney(unitPrice);
        ProductCatalogService.validateMoney(discount);

        BigDecimal grossAmount = unitPrice
            .multiply(orderItem.getQuantity())
            .setScale(2, RoundingMode.HALF_UP);

        if (discountType == DiscountType.PERCENTAGE) {
            if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Discount percentage cannot exceed 100.");
            }
        } else if (discount.compareTo(grossAmount) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the item gross amount.");
        }
    }
}
