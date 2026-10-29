package com.pi.erp.supplier;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.supplier.address.RequestSupplierAddressDTO;
import com.pi.erp.supplier.address.SupplierAddressService;
import com.pi.erp.supplier.brand.SupplierBrandRepository;
import com.pi.erp.supplier.category.SupplierCategoryRepository;
import com.pi.erp.supplier.product.ProductSupplierRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.validation.Validator;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<Supplier> search(SupplierFilter filter, Pageable pageable) {
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
                    likePattern(filter.legalName()), '!'
                )
            );
        }
        if (filter.tradeName() != null && !filter.tradeName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("tradeName")),
                    likePattern(filter.tradeName()), '!'
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
                cb.like(cb.lower(root.get("email")), likePattern(filter.email()), '!')
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

        if (filter.query() != null && !filter.query().isBlank()) {
            spec = spec.and(groupedSearch(filter.query(), filter.searchField()));
        }

        return repository.findAll(spec, pageable);
    }

    @Transactional
    public Supplier register(RequestSupplierDTO data) {
        String taxId = normalizeTaxId(data.taxId());
        if (data.phone() == null || data.phone().isBlank()) {
            throw new IllegalArgumentException("Supplier phone is required.");
        }
        if (repository.existsByTaxIdIgnoreCase(taxId)) {
            throw new IllegalArgumentException("Supplier already exists.");
        }

        Supplier supplier = new Supplier(data);
        supplier.setTaxId(taxId);
        supplier.setTradeName(data.tradeName() == null || data.tradeName().isBlank()
            ? null : data.tradeName());
        supplier.setEmail(data.email() == null || data.email().isBlank() ? null : data.email());
        supplier.setWhatsapp(data.whatsapp() == null || data.whatsapp().isBlank()
            ? null : data.whatsapp());
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
        String taxId = data.taxId() != null ? normalizeTaxId(data.taxId()) : supplier.getTaxId();
        String legalName = data.legalName() != null ? data.legalName() : supplier.getLegalName();

        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("Supplier tax ID is required.");
        }
        if (legalName == null || legalName.isBlank()) {
            throw new IllegalArgumentException("Supplier legal name is required.");
        }
        if (data.taxId() != null && repository.existsByTaxIdIgnoreCaseAndIdNot(taxId, id)) {
            throw new IllegalArgumentException("Supplier already exists.");
        }

        boolean registrationChange = data.legalName() != null || data.tradeName() != null
            || data.taxId() != null || data.email() != null || data.phone() != null
            || data.whatsapp() != null || data.avgDeliveryDays() != null
            || Boolean.TRUE.equals(data.removeAvgDeliveryDays());
        String phone = data.phone() != null ? data.phone() : supplier.getPhone();
        if (registrationChange && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("Supplier phone is required.");
        }
        if (Boolean.TRUE.equals(data.removeAvgDeliveryDays()) && data.avgDeliveryDays() != null) {
            throw new IllegalArgumentException(
                "Cannot provide average delivery days and request removal at the same time.");
        }
        if (data.avgDeliveryDays() != null && data.avgDeliveryDays() < 0) {
            throw new IllegalArgumentException("Average delivery days cannot be negative.");
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
            supplier.setPhone(phone);
        }
        if (data.whatsapp() != null) {
            supplier.setWhatsapp(data.whatsapp().isBlank() ? null : data.whatsapp());
        }
        if (Boolean.TRUE.equals(data.removeAvgDeliveryDays())) {
            supplier.setAvgDeliveryDays(null);
        } else if (data.avgDeliveryDays() != null) {
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

    private Specification<Supplier> groupedSearch(String query, SupplierSearchField searchField) {
        SupplierSearchField field = searchField == null ? SupplierSearchField.NAME : searchField;
        String text = query.trim();
        String document = text.replaceAll("[^\\p{L}\\p{N}]", "");
        if (field == SupplierSearchField.DOCUMENT && document.isEmpty()) {
            throw new IllegalArgumentException("Document search must contain letters or digits.");
        }
        return (root, criteria, cb) -> {
            if (field == SupplierSearchField.NAME) {
                return cb.or(
                    cb.like(cb.lower(root.get("legalName")), likePattern(text), '!'),
                    cb.like(cb.lower(root.get("tradeName")), likePattern(text), '!')
                );
            }
            if (field == SupplierSearchField.DOCUMENT) {
                return cb.like(
                    cb.lower(normalizedSearch(cb, root.get("taxId"))),
                    likePattern(document),
                    '!'
                );
            }
            var email = cb.like(cb.lower(root.get("email")), likePattern(text), '!');
            if (!text.matches("[+()0-9.\\s-]+") || !text.matches(".*[0-9].*")) {
                return email;
            }
            String phone = text.replaceAll("[^0-9]", "");
            return cb.or(
                email,
                cb.like(normalizedSearch(cb, root.get("phone")), likePattern(phone), '!'),
                cb.like(normalizedSearch(cb, root.get("whatsapp")), likePattern(phone), '!')
            );
        };
    }

    private Expression<String> normalizedSearch(CriteriaBuilder cb, Expression<String> value) {
        return cb.function(
            "regexp_replace", String.class, value,
            cb.literal("[^[:alnum:]]"), cb.literal(""), cb.literal("g")
        );
    }

    private String likePattern(String text) {
        return "%" + text.toLowerCase(Locale.ROOT).replace("!", "!!")
            .replace("%", "!%").replace("_", "!_") + "%";
    }

    private String normalizeTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("Supplier tax ID is required.");
        }
        String normalized = taxId.replaceAll("[\\s./-]", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{14}")) {
            throw new IllegalArgumentException("Supplier tax ID must contain 14 letters or digits.");
        }
        return normalized;
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
