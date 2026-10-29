package com.pi.erp.customer;

import com.pi.erp.customer.address.CustomerAddressService;
import com.pi.erp.customer.address.RequestCustomerAddressDTO;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.price.PriceTable;
import com.pi.erp.price.PriceTableRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<Customer> search(CustomerFilter filter, Pageable pageable) {
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
                cb.like(cb.lower(root.get("legalName")), likePattern(filter.legalName()), '!')
            );
        }
        if (filter.tradeName() != null && !filter.tradeName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("tradeName")), likePattern(filter.tradeName()), '!')
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

        if (filter.query() != null && !filter.query().isBlank()) {
            spec = spec.and(groupedSearch(filter.query(), filter.searchField()));
        }

        return repository.findAll(spec, pageable);
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

        if (Boolean.TRUE.equals(data.removeBirthDate()) && data.birthDate() != null) {
            throw new IllegalArgumentException(
                "Cannot provide a birth date and request its removal at the same time."
            );
        }
        if (data.birthDate() != null && data.birthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
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
        if (Boolean.TRUE.equals(data.removeBirthDate())) {
            customer.setBirthDate(null);
        } else if (data.birthDate() != null) {
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

    private Specification<Customer> groupedSearch(String query, CustomerSearchField searchField) {
        CustomerSearchField field = searchField == null ? CustomerSearchField.NAME : searchField;
        String text = query.trim();
        String document = text.replaceAll("[^\\p{L}\\p{N}]", "");
        if (field == CustomerSearchField.DOCUMENT && document.isEmpty()) {
            throw new IllegalArgumentException("Document search must contain letters or digits.");
        }
        return (root, criteria, cb) -> {
            if (field == CustomerSearchField.NAME) {
                return cb.or(
                    cb.like(cb.lower(root.get("legalName")), likePattern(text), '!'),
                    cb.like(cb.lower(root.get("tradeName")), likePattern(text), '!')
                );
            }
            if (field == CustomerSearchField.DOCUMENT) {
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
