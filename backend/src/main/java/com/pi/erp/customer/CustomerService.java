package com.pi.erp.customer;

import com.pi.erp.customer.address.CustomerAddressService;
import com.pi.erp.customer.address.RequestCustomerAddressDTO;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.price.PriceTable;
import com.pi.erp.price.PriceTableRepository;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private PriceTableRepository priceTableRepository;

    @Autowired
    private CustomerAddressService customerAddressService;

    @Autowired
    private Validator validator;

    public List<Customer> search(CustomerFilter filter) {
        Specification<Customer> spec = Specification.allOf();

        if (filter.personType() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("personType"), filter.personType())
            );
        }
        if (filter.taxId() != null && !filter.taxId().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("taxId"), filter.taxId())
            );
        }
        if (filter.legalName() != null && !filter.legalName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("legalName")), "%" + filter.legalName().toLowerCase() + "%")
            );
        }
        if (filter.tradeName() != null && !filter.tradeName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("tradeName")), "%" + filter.tradeName().toLowerCase() + "%")
            );
        }
        if (filter.municipalReg() != null && !filter.municipalReg().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("municipalReg"), filter.municipalReg())
            );
        }
        if (filter.stateReg() != null && !filter.stateReg().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("stateReg"), filter.stateReg())
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
        if (filter.priceTableId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("priceTable").get("id"), filter.priceTableId())
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
    public Customer register(RequestCustomerDTO data) {
        if (repository.existsByTaxId(data.taxId())) {
            throw new IllegalArgumentException("Customer already exists.");
        }

        if (data.personType() == PersonType.PJ
            && (data.tradeName() == null || data.tradeName().isBlank())) {
            throw new IllegalArgumentException(
                "Trade name is required for legal entities."
            );
        }

        if (data.birthDate() != null && data.birthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Birth date cannot be in the future."
            );
        }

        PriceTable priceTable = findPriceTable(data.priceTableId());

        Customer customer = new Customer(data, priceTable);
        Customer savedCustomer = repository.save(customer);

        if (data.address() != null) {
            RequestCustomerDTO.AddressDTO address = data.address();
            RequestCustomerAddressDTO addressData = new RequestCustomerAddressDTO(
                savedCustomer.getId(),
                address.addressType(),
                address.state(),
                address.city(),
                address.street(),
                address.neighborhood(),
                address.number(),
                address.complement(),
                address.zipCode()
            );
            customerAddressService.register(addressData);
        }

        return savedCustomer;
    }

    public Customer update(Long id, PatchCustomerDTO data) {
        Customer customer = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        if (Boolean.TRUE.equals(data.removePriceTable()) && data.priceTableId() != null) {
            throw new IllegalArgumentException(
                "Cannot provide a price table and request its removal at the same time."
            );
        }

        String email = customer.getEmail();

        if (data.email() != null) {
            email = data.email().isBlank() ? null : data.email();
            validateEmail(email);
        }

        if (data.taxId() != null) {
            if (data.taxId().isBlank()) {
                throw new IllegalArgumentException("Customer tax ID is required.");
            }
            if (repository.existsByTaxIdAndIdNot(data.taxId(), id)) {
                throw new IllegalArgumentException("Customer already exists.");
            }
            customer.setTaxId(data.taxId());
        }

        if (data.tradeName() != null) {
            String tradeName = data.tradeName().isBlank()
                ? null
                : data.tradeName();

            if (customer.getPersonType() == PersonType.PJ && tradeName == null) {
                throw new IllegalArgumentException(
                    "Trade name is required for legal entities."
                );
            }

            customer.setTradeName(tradeName);
        }

        if (data.legalName() != null) {
            if (data.legalName().isBlank()) {
                throw new IllegalArgumentException("Customer name is required.");
            }
            customer.setLegalName(data.legalName());
        }
        if (data.birthDate() != null) {
            if (data.birthDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                    "Birth date cannot be in the future."
                );
            }
            customer.setBirthDate(data.birthDate());
        }
        if (data.municipalReg() != null) {
            customer.setMunicipalReg(
                data.municipalReg().isBlank() ? null : data.municipalReg());
        }
        if (data.stateReg() != null) {
            customer.setStateReg(
                data.stateReg().isBlank() ? null : data.stateReg());
        }
        if (data.email() != null) {
            customer.setEmail(email);
        }
        if (data.phone() != null) {
            if (data.phone().isBlank()) {
                throw new IllegalArgumentException("Customer phone is required.");
            }
            customer.setPhone(data.phone());
        }
        if (data.whatsapp() != null) {
            customer.setWhatsapp(
                data.whatsapp().isBlank() ? null : data.whatsapp());
        }
        if (data.active() != null) {
            customer.setActive(data.active());
        }
        if (Boolean.TRUE.equals(data.removePriceTable())) {
            customer.setPriceTable(null);
        } else if (data.priceTableId() != null) {
            customer.setPriceTable(findPriceTable(data.priceTableId()));
        }

        return repository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        repository.delete(customer);
    }

    private PriceTable findPriceTable(Long priceTableId) {
        if (priceTableId == null) {
            return null;
        }

        return priceTableRepository
            .findById(priceTableId)
            .orElseThrow(() -> new ResourceNotFoundException("Price table not found."));
    }

    private void validateEmail(String email) {
        if (!validator.validateValue(RequestCustomerDTO.class, "email", email).isEmpty()) {
            throw new IllegalArgumentException("Customer email must be a valid email address.");
        }
    }
}
