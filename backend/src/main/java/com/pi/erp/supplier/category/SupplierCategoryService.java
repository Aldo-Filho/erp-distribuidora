package com.pi.erp.supplier.category;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.category.Category;
import com.pi.erp.product.category.CategoryRepository;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.SupplierRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierCategoryService {

    @Autowired
    private SupplierCategoryRepository repository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<SupplierCategory> search(Long supplierId) {
        findSupplier(supplierId);
        return repository.findAllBySupplierId(supplierId);
    }

    @Transactional
    public SupplierCategory register(Long supplierId, RequestSupplierCategoryDTO data) {
        Supplier supplier = findSupplier(supplierId);
        Category category = findCategory(data.categoryId());
        SupplierCategoryId id = new SupplierCategoryId(supplierId, data.categoryId());

        if (repository.existsById(id)) {
            throw new IllegalArgumentException("This category is already linked to this supplier.");
        }

        SupplierCategory supplierCategory = new SupplierCategory(supplier, category);
        return repository.save(supplierCategory);
    }

    @Transactional
    public void delete(Long supplierId, Long categoryId) {
        SupplierCategory supplierCategory = repository
            .findById(new SupplierCategoryId(supplierId, categoryId))
            .orElseThrow(() -> new ResourceNotFoundException("Supplier category not found."));

        repository.delete(supplierCategory);
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        return supplierRepository
            .findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }

    private Category findCategory(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required.");
        }

        return categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
    }
}
