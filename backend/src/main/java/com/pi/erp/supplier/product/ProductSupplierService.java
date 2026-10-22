package com.pi.erp.supplier.product;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.SupplierRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSupplierService {

    @Autowired
    private ProductSupplierRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    public List<ProductSupplier> search(ProductSupplierFilter filter) {
        if (filter.minCost() != null
            && filter.maxCost() != null
            && filter.minCost().compareTo(filter.maxCost()) > 0) {
            throw new IllegalArgumentException("Minimum cost cannot be greater than maximum cost.");
        }

        Specification<ProductSupplier> spec = Specification.allOf();

        if (filter.supplierId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), filter.supplierId())
            );
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("id"), filter.productId())
            );
        }
        if (filter.brandId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("brand").get("id"), filter.brandId())
            );
        }
        if (filter.categoryId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("category").get("id"), filter.categoryId())
            );
        }
        if (filter.productName() != null && !filter.productName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("product").get("name")),
                    "%" + filter.productName().toLowerCase(Locale.ROOT) + "%"
                )
            );
        }
        if (filter.supplierProductCode() != null && !filter.supplierProductCode().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("supplierProductCode")),
                    "%" + filter.supplierProductCode().toLowerCase(Locale.ROOT) + "%"
                )
            );
        }
        if (filter.minCost() != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("cost"), filter.minCost())
            );
        }
        if (filter.maxCost() != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("cost"), filter.maxCost())
            );
        }
        if (filter.avgDeliveryDays() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("avgDeliveryDays"), filter.avgDeliveryDays())
            );
        }
        if (filter.preferred() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("preferred"), filter.preferred())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("active"), filter.active())
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public ProductSupplier register(RequestProductSupplierDTO data) {
        Supplier supplier = findSupplier(data.supplierId());
        Product product = findProduct(data.productId());

        if (repository.existsByProductIdAndSupplierId(data.productId(), data.supplierId())) {
            throw new IllegalArgumentException("This product is already linked to this supplier.");
        }

        ProductSupplier productSupplier = new ProductSupplier(data, product, supplier);
        return repository.save(productSupplier);
    }

    @Transactional
    public ProductSupplier update(Long id, PatchProductSupplierDTO data) {
        ProductSupplier productSupplier = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));

        Long productId = data.productId() != null
            ? data.productId()
            : productSupplier.getProduct().getId();
        Long supplierId = data.supplierId() != null
            ? data.supplierId()
            : productSupplier.getSupplier().getId();

        if (repository.existsByProductIdAndSupplierIdAndIdNot(productId, supplierId, id)) {
            throw new IllegalArgumentException("This product is already linked to this supplier.");
        }

        Product product = productSupplier.getProduct();
        Supplier supplier = productSupplier.getSupplier();

        if (data.productId() != null) {
            product = findProduct(productId);
        }
        if (data.supplierId() != null) {
            supplier = findSupplier(supplierId);
        }

        productSupplier.setProduct(product);
        productSupplier.setSupplier(supplier);

        if (data.cost() != null) {
            productSupplier.setCost(data.cost());
        }
        if (data.avgDeliveryDays() != null) {
            productSupplier.setAvgDeliveryDays(data.avgDeliveryDays());
        }
        if (data.supplierProductCode() != null) {
            productSupplier.setSupplierProductCode(
                data.supplierProductCode().isBlank() ? null : data.supplierProductCode()
            );
        }
        if (data.preferred() != null) {
            productSupplier.setPreferred(data.preferred());
        }
        if (data.active() != null) {
            productSupplier.setActive(data.active());
        }

        return repository.save(productSupplier);
    }

    @Transactional
    public void delete(Long id) {
        ProductSupplier productSupplier = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));

        repository.delete(productSupplier);
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        return supplierRepository
            .findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }
}
