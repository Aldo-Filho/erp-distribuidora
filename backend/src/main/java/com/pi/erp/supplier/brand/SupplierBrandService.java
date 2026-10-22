package com.pi.erp.supplier.brand;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.brand.BrandRepository;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.SupplierRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierBrandService {

    @Autowired
    private SupplierBrandRepository repository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private BrandRepository brandRepository;

    public List<SupplierBrand> search(Long supplierId) {
        findSupplier(supplierId);
        return repository.findAllBySupplierId(supplierId);
    }

    @Transactional
    public SupplierBrand register(Long supplierId, RequestSupplierBrandDTO data) {
        Supplier supplier = findSupplier(supplierId);
        Brand brand = findBrand(data.brandId());
        SupplierBrandId id = new SupplierBrandId(supplierId, data.brandId());

        if (repository.existsById(id)) {
            throw new IllegalArgumentException("This brand is already linked to this supplier.");
        }

        SupplierBrand supplierBrand = new SupplierBrand(supplier, brand);
        return repository.save(supplierBrand);
    }

    @Transactional
    public void delete(Long supplierId, Long brandId) {
        SupplierBrand supplierBrand = repository
            .findById(new SupplierBrandId(supplierId, brandId))
            .orElseThrow(() -> new ResourceNotFoundException("Supplier brand not found."));

        repository.delete(supplierBrand);
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        return supplierRepository
            .findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }

    private Brand findBrand(Long brandId) {
        if (brandId == null) {
            throw new IllegalArgumentException("Brand ID is required.");
        }

        return brandRepository
            .findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
    }
}
