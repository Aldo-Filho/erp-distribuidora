package com.pi.erp.supplier;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.supplier.address.RequestSupplierAddressDTO;
import com.pi.erp.supplier.address.SupplierAddressService;
import com.pi.erp.supplier.brand.SupplierBrandRepository;
import com.pi.erp.supplier.category.SupplierCategoryRepository;
import com.pi.erp.supplier.product.ProductSupplierRepository;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository repository;

    @Autowired
    private SupplierAddressService supplierAddressService;

    @Autowired
    private SupplierBrandRepository supplierBrandRepository;

    @Autowired
    private SupplierCategoryRepository supplierCategoryRepository;

    @Autowired
    private ProductSupplierRepository productSupplierRepository;

    @Autowired
    private Validator validator;

    public List<Supplier> search(SupplierFilter filter) {
        Specification<Supplier> spec = Specification.allOf();

        if (filter.supplierAddressId() != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(
                    root.join("supplierAddresses").get("id"),
                    filter.supplierAddressId()
                );
            });
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(
                    root.join("productSuppliers").get("product").get("id"),
                    filter.productId()
                );
            });
        }
        if (filter.brandId() != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("supplierBrands").get("brand").get("id"), filter.brandId());
            });
        }
        if (filter.categoryId() != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(
                    root.join("supplierCategories").get("category").get("id"),
                    filter.categoryId()
                );
            });
        }
        if (filter.legalName() != null && !filter.legalName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("legalName")),
                    "%" + filter.legalName().toLowerCase() + "%"
                )
            );
        }
        if (filter.tradeName() != null && !filter.tradeName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("tradeName")),
                    "%" + filter.tradeName().toLowerCase() + "%"
                )
            );
        }
        if (filter.taxId() != null && !filter.taxId().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("taxId"), filter.taxId())
            );
        }
        if (filter.email() != null && !filter.email().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + filter.email().toLowerCase() + "%")
            );
        }
        if (filter.phone() != null && !filter.phone().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("phone"), filter.phone())
            );
        }
        if (filter.whatsapp() != null && !filter.whatsapp().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("whatsapp"), filter.whatsapp())
            );
        }
        if (filter.avgDeliveryDays() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("avgDeliveryDays"), filter.avgDeliveryDays())
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
    public Supplier register(RequestSupplierDTO data) {
        if (repository.existsByTaxId(data.taxId())) {
            throw new IllegalArgumentException("Supplier already exists.");
        }

        Supplier supplier = new Supplier(data);
        Supplier savedSupplier = repository.save(supplier);

        if (data.address() != null) {
            RequestSupplierDTO.AddressDTO address = data.address();
            RequestSupplierAddressDTO addressData = new RequestSupplierAddressDTO(
                savedSupplier.getId(),
                address.country(),
                address.state(),
                address.city(),
                address.street(),
                address.neighborhood(),
                address.number(),
                address.complement(),
                address.zipCode(),
                address.addressType()
            );
            supplierAddressService.register(addressData);
        }

        return savedSupplier;
    }

    @Transactional
    public Supplier update(Long id, PatchSupplierDTO data) {
        Supplier supplier = findSupplier(id);
        String taxId = data.taxId() != null ? data.taxId() : supplier.getTaxId();
        String legalName = data.legalName() != null ? data.legalName() : supplier.getLegalName();

        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("Supplier tax ID is required.");
        }
        if (legalName == null || legalName.isBlank()) {
            throw new IllegalArgumentException("Supplier legal name is required.");
        }
        if (repository.existsByTaxIdAndIdNot(taxId, id)) {
            throw new IllegalArgumentException("Supplier already exists.");
        }

        String email = supplier.getEmail();

        if (data.email() != null) {
            email = data.email().isBlank() ? null : data.email();
            validateEmail(email);
        }

        supplier.setTaxId(taxId);
        supplier.setLegalName(legalName);

        if (data.tradeName() != null) {
            supplier.setTradeName(data.tradeName().isBlank() ? null : data.tradeName());
        }
        if (data.email() != null) {
            supplier.setEmail(email);
        }
        if (data.phone() != null) {
            supplier.setPhone(data.phone().isBlank() ? null : data.phone());
        }
        if (data.whatsapp() != null) {
            supplier.setWhatsapp(data.whatsapp().isBlank() ? null : data.whatsapp());
        }
        if (data.avgDeliveryDays() != null) {
            supplier.setAvgDeliveryDays(data.avgDeliveryDays());
        }
        if (data.active() != null) {
            supplier.setActive(data.active());
        }

        return repository.save(supplier);
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = findSupplier(id);

        supplierBrandRepository.deleteAllBySupplierId(id);
        supplierCategoryRepository.deleteAllBySupplierId(id);
        productSupplierRepository.deleteAllBySupplierId(id);
        repository.delete(supplier);
        repository.flush();
    }

    private Supplier findSupplier(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        return repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }

    private void validateEmail(String email) {
        if (!validator.validateValue(RequestSupplierDTO.class, "email", email).isEmpty()) {
            throw new IllegalArgumentException("Supplier email must be a valid email address.");
        }
    }
}
