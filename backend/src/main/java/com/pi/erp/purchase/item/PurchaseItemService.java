package com.pi.erp.purchase.item;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.purchase.Purchase;
import com.pi.erp.purchase.PurchaseRepository;
import com.pi.erp.purchase.PurchaseStatus;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.supplier.product.ProductSupplier;
import com.pi.erp.supplier.product.ProductSupplierRepository;
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
    private ProductRepository productRepository;

    @Autowired
    private ProductSupplierRepository productSupplierRepository;

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
        ProductSupplier productSupplier = findProductSupplier(purchase, product);
        Warehouse warehouse = findWarehouse(data.warehouseId());
        BigDecimal unitCost = data.unitCost() != null ? data.unitCost() : productSupplier.getCost();
        validateValues(data.quantity(), unitCost);
        validateStock(product, warehouse);

        if (repository.existsByPurchaseIdAndProductId(purchase.getId(), product.getId())) {
            throw new IllegalArgumentException("This product already exists in this purchase.");
        }

        PurchaseItem purchaseItem = new PurchaseItem(data, purchase, product, warehouse, unitCost);
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
        BigDecimal unitCost = data.unitCost() != null ? data.unitCost() : purchaseItem.getUnitCost();

        if (data.productId() != null && !data.productId().equals(product.getId())) {
            product = findProduct(data.productId());
            ProductSupplier productSupplier = findProductSupplier(purchase, product);
            if (data.unitCost() == null) {
                unitCost = productSupplier.getCost();
            }
        }
        if (data.warehouseId() != null) {
            warehouse = findWarehouse(data.warehouseId());
        }

        Integer quantity = data.quantity() != null ? data.quantity() : purchaseItem.getQuantity();
        validateValues(quantity, unitCost);
        validateStock(product, warehouse);

        if (repository.existsByPurchaseIdAndProductIdAndIdNot(purchaseId, product.getId(), id)) {
            throw new IllegalArgumentException("This product already exists in this purchase.");
        }

        purchaseItem.setProduct(product);
        purchaseItem.setWarehouse(warehouse);
        purchaseItem.setQuantity(quantity);
        purchaseItem.setUnitCost(unitCost);
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
        return repository.findPurchaseIdByItemId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase item not found."));
    }

    private Purchase findPurchase(Long purchaseId) {
        if (purchaseId == null) {
            throw new IllegalArgumentException("Purchase ID is required.");
        }
        Purchase purchase = purchaseRepository.findByIdForUpdate(purchaseId)
            .orElseThrow(() -> new ResourceNotFoundException("Purchase not found."));

        entityManager.refresh(purchase);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft purchases can have their items edited.");
        }
        return purchase;
    }

    private PurchaseItem findPurchaseItem(Long id, Long purchaseId) {
        PurchaseItem purchaseItem = repository.findByIdAndPurchaseIdForUpdate(id, purchaseId)
            .orElseThrow(() -> new IllegalArgumentException("Purchase item changed. Retry the operation."));

        entityManager.refresh(purchaseItem);
        if (purchaseItem.getReceivedQuantity() == null || purchaseItem.getReceivedQuantity() != 0) {
            throw new IllegalArgumentException("Cannot edit a purchase item with received quantities.");
        }
        return purchaseItem;
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        entityManager.refresh(product);
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product must be active.");
        }
        return product;
    }

    private ProductSupplier findProductSupplier(Purchase purchase, Product product) {
        ProductSupplier productSupplier = productSupplierRepository
            .findBySupplierIdAndProductId(purchase.getSupplier().getId(), product.getId())
            .orElseThrow(() -> new IllegalArgumentException("Product is not linked to the supplier."));

        entityManager.refresh(productSupplier);
        if (!productSupplier.isActive()) {
            throw new IllegalArgumentException("Product supplier link must be active.");
        }
        return productSupplier;
    }

    private Warehouse findWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required.");
        }
        return warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
    }

    private void validateStock(Product product, Warehouse warehouse) {
        if (warehouse == null
            || !stockItemRepository.existsByWarehouseIdAndProductId(warehouse.getId(), product.getId())) {
            throw new IllegalArgumentException("Product has no stock item in the selected warehouse.");
        }
    }

    private void recalculateTotal(Purchase purchase) {
        BigDecimal totalAmount = repository.calculateTotalByPurchaseId(purchase.getId());
        validateAmount(totalAmount);
        purchase.setTotalAmount(totalAmount);
        purchaseRepository.save(purchase);
    }

    private void validateValues(Integer quantity, BigDecimal unitCost) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        validateAmount(unitCost);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(MAX_AMOUNT) > 0
            || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount must be non-negative, at most 9999999999.99 and in cents.");
        }
    }
}
