package com.pi.erp.customer;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.price.PriceTable;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepository repository;

    @Autowired
    private EntityManager entityManager;

    public List<Customer> search(CustomerFilter filter) {
        Specification<Customer> spec = Specification.allOf();

        if (filter.personType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("personType"), filter.personType()));
        }
        if (filter.taxId() != null && !filter.taxId().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("taxId"), filter.taxId()));
        }
        if (filter.legalName() != null && !filter.legalName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("legalName")),
                            "%" + filter.legalName().toLowerCase() + "%"));
        }
        if (filter.tradeName() != null && !filter.tradeName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("tradeName")),
                            "%" + filter.tradeName().toLowerCase() + "%"));
        }
        if (filter.municipalReg() != null && !filter.municipalReg().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("municipalReg"), filter.municipalReg()));
        }
        if (filter.stateReg() != null && !filter.stateReg().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("stateReg"), filter.stateReg()));
        }
        if (filter.email() != null && !filter.email().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")),
                            "%" + filter.email().toLowerCase() + "%"));
        }
        if (filter.phone() != null && !filter.phone().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("phone"), filter.phone()));
        }
        if (filter.whatsapp() != null && !filter.whatsapp().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("whatsapp"), filter.whatsapp()));
        }
        if (filter.priceTableId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("priceTable").get("id"), filter.priceTableId()));
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("active"), filter.active()));
        }

        return repository.findAll(spec);
    }

    public Customer register(RequestCustomerDTO data) {
        if (repository.existsByTaxId(data.taxId())) {
            throw new IllegalArgumentException("Customer already exists.");
        }

        PriceTable priceTable = findPriceTable(data.priceTableId());

        Customer customer = new Customer(data, priceTable);
        return repository.save(customer);
    }

    public Customer update(Long id, PatchCustomerDTO data) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        if (data.personType() != null) {
            customer.setPersonType(data.personType());
        }
        if (data.taxId() != null && !data.taxId().isBlank()) {
            if (repository.existsByTaxIdAndIdNot(data.taxId(), id)) {
                throw new IllegalArgumentException("Customer already exists.");
            }
            customer.setTaxId(data.taxId());
        }
        if (data.legalName() != null && !data.legalName().isBlank()) {
            customer.setLegalName(data.legalName());
        }
        if (data.tradeName() != null && !data.tradeName().isBlank()) {
            customer.setTradeName(data.tradeName());
        }
        if (data.birthDate() != null) {
            customer.setBirthDate(data.birthDate());
        }
        if (data.municipalReg() != null && !data.municipalReg().isBlank()) {
            customer.setMunicipalReg(data.municipalReg());
        }
        if (data.stateReg() != null && !data.stateReg().isBlank()) {
            customer.setStateReg(data.stateReg());
        }
        if (data.email() != null && !data.email().isBlank()) {
            customer.setEmail(data.email());
        }
        if (data.phone() != null && !data.phone().isBlank()) {
            customer.setPhone(data.phone());
        }
        if (data.whatsapp() != null && !data.whatsapp().isBlank()) {
            customer.setWhatsapp(data.whatsapp());
        }
        if (data.active() != null) {
            customer.setActive(data.active());
        }
        if (data.priceTableId() != null) {
            customer.setPriceTable(findPriceTable(data.priceTableId()));
        }

        return repository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        repository.delete(customer);
    }

    private PriceTable findPriceTable(Long priceTableId) {
        if (priceTableId == null) {
            return null;
        }

        PriceTable priceTable = entityManager.find(PriceTable.class, priceTableId);
        if (priceTable == null) {
            throw new ResourceNotFoundException("Price table not found.");
        }
        return priceTable;
    }
}
