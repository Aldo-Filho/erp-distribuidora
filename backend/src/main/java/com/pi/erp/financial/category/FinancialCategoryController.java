package com.pi.erp.financial.category;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.financial.FinancialPages;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/financial-categories")
public class FinancialCategoryController {
    @Autowired private FinancialCategoryRepository repository;
    @Autowired private FinancialCategoryService service;
    @GetMapping public Page<FinancialCategory> search(@RequestParam(required=false) CategoryType type,
        @RequestParam(required=false) Boolean active, @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="25") int size) {
        Specification<FinancialCategory> spec=Specification.allOf();
        if(type!=null) spec=spec.and((r,q,c)->c.equal(r.get("type"),type));
        if(active!=null) spec=spec.and((r,q,c)->c.equal(r.get("active"),active));
        return repository.findAll(spec,FinancialPages.of(page,size));
    }
    @GetMapping("/{id}") public FinancialCategory get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria financeira não encontrada."));
    }
    @PostMapping public ResponseEntity<FinancialCategory> register(@RequestBody @Valid RequestFinancialCategoryDTO data) {
        return ResponseEntity.status(201).body(service.register(data));
    }
    @PatchMapping("/{id}") public FinancialCategory update(@PathVariable Long id,@RequestBody @Valid PatchFinancialCategoryDTO data) {
        return service.update(id,data);
    }
}
