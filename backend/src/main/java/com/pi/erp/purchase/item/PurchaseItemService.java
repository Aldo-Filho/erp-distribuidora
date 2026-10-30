package com.pi.erp.purchase.item;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.purchase.Purchase;
import com.pi.erp.purchase.PurchaseRepository;
import com.pi.erp.purchase.PurchaseStatus;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.supplier.product.option.ProductSupplierOptionService;
import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseItemService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    @Autowired
    private PurchaseItemRepository repository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProductSupplierOptionService productSupplierOptionService;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private EntityManager entityManager;

    public List<PurchaseItem> search(PurchaseItemFilter filter) {
        Specification<PurchaseItem> spec = Specification.allOf();

        if (filter.purchaseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("purchase").get("id"), filter.purchaseId())
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
    public PurchaseItem register(RequestPurchaseItemDTO data) {
        Purchase purchase = findPurchase(data.purchaseId());
        Product product = findProduct(data.productId());
        var option = productSupplierOptionService.findForPurchase(
            purchase.getSupplier().getId(),
            product.getId(),
            data.supplierOptionId()
        );
        Warehouse warehouse = findWarehouse(data.warehouseId());
        BigDecimal unitCost = data.unitCost() != null ? data.unitCost() : option.getCost();
        validateValues(data.quantity(), unitCost);
        validateStock(product, warehouse);

        if (
            repository.existsByIdentity(
                purchase.getId(),
                product.getId(),
                warehouse.getId(),
                option.getId(),
                null
            )
        ) {
            throw new IllegalArgumentException("This product already exists in this purchase.");
        }

        PurchaseItem purchaseItem = new PurchaseItem(data, purchase, product, warehouse, unitCost);
        purchaseItem.captureSnapshot(option);
        PurchaseItem savedPurchaseItem = repository.save(purchaseItem);
        repository.flush();
        recalculateTotal(purchase);
        return savedPurchaseItem;
    }

    @Transactional
    public PurchaseItem update(Long id, PatchPurchaseItemDTO data) {
        Long purchaseId = findPurchaseId(id);
        Purchase purchase = findPurchase(purchaseId);
        PurchaseItem purchaseItem = findPurchaseItem(id, purchaseId);
        Product product = purchaseItem.getProduct();
        Warehouse warehouse = purchaseItem.getWarehouse();
        BigDecimal unitCost =
            data.unitCost() != null ? data.unitCost() : purchaseItem.getUnitCost();

        boolean productChange =
            data.productId() != null && !data.productId().equals(product.getId());
        if (productChange) {
            product = findProduct(data.productId());
        } else {
            product = findProduct(product.getId());
        }
        boolean optionChange = productChange || data.supplierOptionId() != null;
        var option = purchaseItem.getSupplierOption();
        if (optionChange) {
            option = productSupplierOptionService.findForPurchase(
                purchase.getSupplier().getId(),
                product.getId(),
                data.supplierOptionId()
            );
            if (data.unitCost() == null) {
                unitCost = option.getCost();
            }
        } else if (option != null) {
            productSupplierOptionService.findForPurchase(
                purchase.getSupplier().getId(),
                product.getId(),
                option.getId()
            );
        } else {
            throw new IllegalArgumentException(
                "Select a confirmed supplier option for this legacy draft item."
            );
        }
        if (data.warehouseId() != null) {
            warehouse = findWarehouse(data.warehouseId());
        }
        BigDecimal quantity =
            data.quantity() != null ? data.quantity() : purchaseItem.getQuantity();
        validateValues(quantity, unitCost);
        validateStock(product, warehouse);
        if (
            repository.existsByIdentity(
                purchaseId,
                product.getId(),
                warehouse.getId(),
                option.getId(),
                id
            )
        ) {
            throw new IllegalArgumentException(
                "This supplier option already exists in this purchase and warehouse."
            );
        }

        purchaseItem.setProduct(product);
        purchaseItem.setWarehouse(warehouse);
        purchaseItem.setQuantity(quantity);
        purchaseItem.setUnitCost(unitCost);
        if (optionChange) {
            purchaseItem.captureSnapshot(option);
        } else {
            purchaseItem.updateBaseQuantity();
        }
        PurchaseItem savedPurchaseItem = repository.save(purchaseItem);
        repository.flush();
        recalculateTotal(purchase);
        return savedPurchaseItem;
    }

    @Transactional
    public void delete(Long id) {
        Long purchaseId = findPurchaseId(id);
        Purchase purchase = findPurchase(purchaseId);
        PurchaseItem purchaseItem = findPurchaseItem(id, purchaseId);
        repository.delete(purchaseItem);
        repository.flush();
        recalculateTotal(purchase);
    }

    private Long findPurchaseId(Long id) {
        return repository
            .findPurchaseIdByItemId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase item not found."));
    }

    private Purchase findPurchase(Long purchaseId) {
        if (purchaseId == null) {
            throw new IllegalArgumentException("Purchase ID is required.");
        }
        Purchase purchase = purchaseRepository
            .findByIdForUpdate(purchaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase not found."));

        entityManager.refresh(purchase);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchases can have their items edited.");
        }
        return purchase;
    }

    private PurchaseItem findPurchaseItem(Long id, Long purchaseId) {
        PurchaseItem purchaseItem = repository
            .findByIdAndPurchaseIdForUpdate(id, purchaseId)
            .orElseThrow(() ->
                new IllegalArgumentException("Purchase item changed. Retry the operation.")
            );

        entityManager.refresh(purchaseItem);
        if (
            purchaseItem.getReceivedQuantity() == null ||
            purchaseItem.getReceivedQuantity().signum() != 0
        ) {
            throw new IllegalArgumentException(
                "Cannot edit a purchase item with received quantities."
            );
        }
        return purchaseItem;
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        Product product = productCatalogService.lockProduct(productId);

        entityManager.refresh(product);
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product must be active.");
        }
        return product;
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
        if (
            warehouse == null ||
            !stockItemRepository.existsByWarehouseIdAndProductId(warehouse.getId(), product.getId())
        ) {
            throw new IllegalArgumentException(
                "Product has no stock item in the selected warehouse."
            );
        }
    }

    private void recalculateTotal(Purchase purchase) {
        BigDecimal totalAmount = repository.calculateTotalByPurchaseId(purchase.getId());
        validateAmount(totalAmount);
        purchase.setTotalAmount(totalAmount);
        purchaseRepository.save(purchase);
    }

    private void validateValues(BigDecimal quantity, BigDecimal unitCost) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        validateAmount(unitCost);
    }

    private void validateAmount(BigDecimal amount) {
        if (
            amount == null ||
            amount.signum() < 0 ||
            amount.compareTo(MAX_AMOUNT) > 0 ||
            amount.stripTrailingZeros().scale() > 2
        ) {
            throw new IllegalArgumentException(
                "Amount must be non-negative, at most 9999999999.99 and in cents."
            );
        }
    }
}
