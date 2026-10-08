package com.pi.erp.supplier;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.supplier.address.RequestSupplierAddressDTO;
import com.pi.erp.supplier.address.SupplierAddressService;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository repository;

    @Autowired
    private SupplierAddressService supplierAddressService;

    public List<Supplier> search(SupplierFilter filter) {
        Specification<Supplier> spec = Specification.allOf();

        if (filter.supplierAddressId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("supplierAddress").get("id"), filter.supplierAddressId())
            );
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
            spec = spec.and((root, query, cb) -> cb.equal(root.get("taxId"), filter.taxId()));
        }
        if (filter.email() != null && !filter.email().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + filter.email().toLowerCase() + "%")
            );
        }
        if (filter.phone() != null && !filter.phone().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("phone"), filter.phone()));
        }
        if (filter.whatsapp() != null && !filter.whatsapp().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("whatsapp"), filter.whatsapp()));
        }
        if (filter.avgDeliveryDays() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("avgDeliveryDays"), filter.avgDeliveryDays())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }

        return repository.findAll(spec);
    }

    @Transactional
    public Supplier register(RequestSupplierDTO data) {
        if (repository.existsByTaxId(data.taxId())) {
            throw new IllegalArgumentException("Supplier already exists!");
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
        Supplier supplier = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        if (data.taxId() != null && !data.taxId().isBlank()) {
            if (repository.existsByTaxIdAndIdNot(data.taxId(), id)) {
                throw new IllegalArgumentException("Supplier already exists!");
            }
            supplier.setTaxId(data.taxId());
        }
        if (data.legalName() != null && !data.legalName().isBlank()) {
            supplier.setLegalName(data.legalName());
        }
        if (data.tradeName() != null && !data.tradeName().isBlank()) {
            supplier.setTradeName(data.tradeName());
        }
        if (data.email() != null && !data.email().isBlank()) {
            supplier.setEmail(data.email());
        }
        if (data.phone() != null && !data.phone().isBlank()) {
            supplier.setPhone(data.phone());
        }
        if (data.whatsapp() != null && !data.whatsapp().isBlank()) {
            supplier.setWhatsapp(data.whatsapp());
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
        Supplier supplier = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        repository.delete(supplier);
    }
}
