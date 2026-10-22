package com.pi.erp.price;

import com.pi.erp.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceTableService {

    @Autowired
    private PriceTableRepository repository;

    public List<PriceTable> search(PriceTableFilter filter) {
        Specification<PriceTable> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }
        if (filter.startDate() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("startDate"), filter.startDate())
            );
        }
        if (filter.endDate() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("endDate"), filter.endDate())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("active"), filter.active())
            );
        }

        return repository.findAll(spec);
    }

    public PriceTable register(RequestPriceTableDTO data) {
        validatePeriod(data.startDate(), data.endDate());

        if (repository.existsByNormalizedName(data.name())) {
            throw new IllegalArgumentException("Price table already exists.");
        }

        PriceTable priceTable = new PriceTable(data);
        return repository.save(priceTable);
    }

    @Transactional
    public PriceTable update(Long id, PatchPriceTableDTO data) {
        PriceTable priceTable = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Price table not found."));

        if (data.name() != null && data.name().isBlank()) {
            throw new IllegalArgumentException("Price table name is required.");
        }

        String name = data.name() != null ? data.name() : priceTable.getName();

        if (repository.existsByNormalizedNameAndIdNot(name, id)) {
            throw new IllegalArgumentException("Price table already exists.");
        }

        LocalDate startDate = data.startDate() != null
            ? data.startDate()
            : priceTable.getStartDate();
        LocalDate endDate = data.endDate() != null ? data.endDate() : priceTable.getEndDate();
        validatePeriod(startDate, endDate);

        priceTable.setName(name);
        priceTable.setStartDate(startDate);
        priceTable.setEndDate(endDate);
        if (data.active() != null) {
            priceTable.setActive(data.active());
        }

        return repository.save(priceTable);
    }

    @Transactional
    public void delete(Long id) {
        PriceTable priceTable = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Price table not found."));

        repository.delete(priceTable);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
    }
}
