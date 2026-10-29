package com.pi.erp.purchase;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.purchase.item.PurchaseItem;
import com.pi.erp.purchase.item.PurchaseItemRepository;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.SupplierRepository;
import com.pi.erp.supplier.product.ProductSupplier;
import com.pi.erp.supplier.product.ProductSupplierRepository;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    @Autowired
    private PurchaseRepository repository;

    @Autowired
    private PurchaseItemRepository purchaseItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductSupplierRepository productSupplierRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private EntityManager entityManager;

    public List<Purchase> search(PurchaseFilter filter) {
        if (filter.createdFrom() != null && filter.createdTo() != null
            && filter.createdFrom().isAfter(filter.createdTo())) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        Specification<Purchase> spec = Specification.allOf();

        if (filter.supplierId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), filter.supplierId())
            );
        }
        if (filter.userId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("user").get("id"), filter.userId())
            );
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.createdFrom() != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom())
            );
        }
        if (filter.createdTo() != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo())
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public Purchase register(RequestPurchaseDTO data) {
        Supplier supplier = findSupplier(data.supplierId());
        User user = findUser(data.userId());
        Purchase purchase = new Purchase(data, supplier, user);
        return repository.save(purchase);
    }

    @Transactional
    public Purchase update(Long id, PatchPurchaseDTO data) {
        Purchase purchase = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase not found."));

        entityManager.refresh(purchase);
        PurchaseStatus status = data.status() != null ? data.status() : purchase.getStatus();
        validateStatusTransition(purchase.getStatus(), status);
        if (purchase.getStatus() != status) {
            if (status == PurchaseStatus.SENT) {
                CurrentUser.require("purchases.send");
            } else if (status == PurchaseStatus.CONFIRMED) {
                CurrentUser.require("purchases.confirm");
            } else if (status == PurchaseStatus.CANCELED) {
                CurrentUser.require("purchases.cancel");
            }
        }

        boolean hasCommercialChanges = data.supplierId() != null || data.userId() != null
            || data.expectedDeliveryDate() != null || Boolean.TRUE.equals(data.removeExpectedDeliveryDate());

        if (hasCommercialChanges) {
            if (purchase.getStatus() != PurchaseStatus.DRAFT) {
                throw new IllegalArgumentException("Only draft purchases can be edited.");
            }
            if (status == PurchaseStatus.CANCELED) {
                throw new IllegalArgumentException("Cannot cancel a purchase and edit it at the same time.");
            }
        }
        if (Boolean.TRUE.equals(data.removeExpectedDeliveryDate()) && data.expectedDeliveryDate() != null) {
            throw new IllegalArgumentException("Cannot provide and remove the delivery date at the same time.");
        }

        Supplier supplier = purchase.getSupplier();
        User user = purchase.getUser();

        if (data.supplierId() != null) {
            if (!data.supplierId().equals(supplier.getId()) && purchaseItemRepository.existsByPurchaseId(id)) {
                throw new IllegalArgumentException("Cannot change the supplier of a purchase with items.");
            }
            supplier = findSupplier(data.supplierId());
        }
        if (data.userId() != null) {
            user = findUser(data.userId());
        }
        if (purchase.getStatus() != status
            && (status == PurchaseStatus.SENT || status == PurchaseStatus.CONFIRMED)) {
            validateReady(purchase, supplier, user);
        }
        if (purchase.getStatus() != status && status == PurchaseStatus.CANCELED
            && purchaseItemRepository.existsByPurchaseIdAndReceivedQuantityGreaterThan(id, 0)) {
            throw new IllegalArgumentException("Cannot cancel a purchase after receiving goods.");
        }

        LocalDate expectedDeliveryDate = purchase.getExpectedDeliveryDate();

        if (Boolean.TRUE.equals(data.removeExpectedDeliveryDate())) {
            expectedDeliveryDate = null;
        } else if (data.expectedDeliveryDate() != null) {
            expectedDeliveryDate = data.expectedDeliveryDate();
        }

        purchase.setSupplier(supplier);
        purchase.setUser(user);
        purchase.setExpectedDeliveryDate(expectedDeliveryDate);
        purchase.setStatus(status);
        return repository.save(purchase);
    }

    @Transactional
    public void delete(Long id) {
        Purchase purchase = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase not found."));

        entityManager.refresh(purchase);

        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchases can be deleted.");
        }
        if (purchaseItemRepository.existsByPurchaseId(id)) {
            throw new IllegalArgumentException("Cannot delete a purchase with items.");
        }

        repository.delete(purchase);
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        Supplier supplier = supplierRepository
            .findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));

        entityManager.refresh(supplier);

        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Supplier must be active.");
        }
        return supplier;
    }

    private User findUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        entityManager.refresh(user);

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("User must be active.");
        }
        return user;
    }

    private void validateStatusTransition(PurchaseStatus currentStatus, PurchaseStatus status) {
        if (currentStatus == status) {
            return;
        }
        if (currentStatus == PurchaseStatus.DRAFT
            && (status == PurchaseStatus.SENT || status == PurchaseStatus.CANCELED)) {
            return;
        }
        if (currentStatus == PurchaseStatus.SENT
            && (status == PurchaseStatus.CONFIRMED || status == PurchaseStatus.CANCELED)) {
            return;
        }
        if (currentStatus == PurchaseStatus.CONFIRMED && status == PurchaseStatus.CANCELED) {
            return;
        }
        throw new IllegalArgumentException("Invalid purchase status transition. Use receipt for goods entries.");
    }

    private void validateReady(Purchase purchase, Supplier supplier, User user) {
        findSupplier(supplier.getId());
        findUser(user.getId());
        List<PurchaseItem> purchaseItems = purchaseItemRepository.findByPurchaseId(purchase.getId());

        if (purchaseItems.isEmpty()) {
            throw new IllegalArgumentException("Purchase must have at least one item.");
        }

        Set<Long> productIds = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseItem purchaseItem : purchaseItems) {
            Product product = purchaseItem.getProduct();
            entityManager.refresh(product);
            ProductSupplier productSupplier = productSupplierRepository
                .findBySupplierIdAndProductId(supplier.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException("Product is not linked to the supplier."));

            entityManager.refresh(productSupplier);

            if (!product.isActive() || !productSupplier.isActive()) {
                throw new IllegalArgumentException("Purchase products and supplier links must be active.");
            }
            if (!productIds.add(product.getId())) {
                throw new IllegalArgumentException("Purchase cannot contain duplicate products.");
            }
            if (purchaseItem.getQuantity() == null || purchaseItem.getQuantity() <= 0
                || purchaseItem.getUnitCost() == null || purchaseItem.getUnitCost().signum() < 0
                || purchaseItem.getUnitCost().compareTo(MAX_AMOUNT) > 0
                || purchaseItem.getUnitCost().stripTrailingZeros().scale() > 2
                || purchaseItem.getReceivedQuantity() == null || purchaseItem.getReceivedQuantity() != 0) {
                throw new IllegalArgumentException("Purchase item has invalid quantities or cost.");
            }
            if (purchaseItem.getWarehouse() == null
                || !stockItemRepository.existsByWarehouseIdAndProductId(
                    purchaseItem.getWarehouse().getId(), product.getId()
                )) {
                throw new IllegalArgumentException("Purchase product has no stock item in its warehouse.");
            }
            totalAmount = totalAmount.add(
                purchaseItem.getUnitCost().multiply(BigDecimal.valueOf(purchaseItem.getQuantity()))
            );
        }

        if (totalAmount.compareTo(MAX_AMOUNT) > 0 || purchase.getTotalAmount() == null
            || totalAmount.compareTo(purchase.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Purchase total is inconsistent or exceeds the supported amount.");
        }
    }
}
