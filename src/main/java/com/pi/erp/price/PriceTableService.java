package com.pi.erp.price;

import com.pi.erp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PriceTableService {
    @Autowired
    private PriceTableRepository repository;

    public List<PriceTable> search(PriceTableFilter filter) {
        Specification<PriceTable> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")),
                            "%" + filter.name().toLowerCase() + "%"));
        }
        if (filter.startDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("startDate"), filter.startDate()));
        }
        if (filter.endDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("endDate"), filter.endDate()));
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("active"), filter.active()));
        }

        return repository.findAll(spec);
    }

    public PriceTable register(RequestPriceTableDTO data) {
        validatePeriod(data.startDate(), data.endDate());

        if (repository.existsByNameIgnoreCase(data.name())) {
            throw new IllegalArgumentException("Price table already exists.");
        }

        return repository.save(new PriceTable(data));
    }

    public PriceTable update(Long id, PatchPriceTableDTO data) {
        PriceTable priceTable = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price table not found."));

        if (data.name() != null && !data.name().isBlank()) {
            if (repository.existsByNameIgnoreCaseAndIdNot(data.name(), id)) {
                throw new IllegalArgumentException("Price table already exists.");
            }
            priceTable.setName(data.name());
        }

        LocalDate startDate = data.startDate() != null
                ? data.startDate()
                : priceTable.getStartDate();
        LocalDate endDate = data.endDate() != null
                ? data.endDate()
                : priceTable.getEndDate();
        validatePeriod(startDate, endDate);

        if (data.startDate() != null) {
            priceTable.setStartDate(data.startDate());
        }
        if (data.endDate() != null) {
            priceTable.setEndDate(data.endDate());
        }
        if (data.active() != null) {
            priceTable.setActive(data.active());
        }

        return repository.save(priceTable);
    }

    @Transactional
    public void delete(Long id) {
        PriceTable priceTable = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price table not found."));

        repository.delete(priceTable);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
    }
}
