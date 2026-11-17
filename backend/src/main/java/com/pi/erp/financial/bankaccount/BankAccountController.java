package com.pi.erp.financial.bankaccount;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.financial.FinancialPages;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/bank-accounts")
public class BankAccountController {
    @Autowired private BankAccountRepository repository;
    @Autowired private BankAccountService service;
    @GetMapping public Page<BankAccount> search(@RequestParam(required=false) BankAccountType type,
        @RequestParam(required=false) Boolean active, @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="25") int size) {
        Specification<BankAccount> spec=Specification.allOf();
        if(type!=null) spec=spec.and((r,q,c)->c.equal(r.get("type"),type));
        if(active!=null) spec=spec.and((r,q,c)->c.equal(r.get("active"),active));
        return repository.findAll(spec,FinancialPages.of(page,size));
    }
    @GetMapping("/{id}") public BankAccount get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conta bancária não encontrada."));
    }
    @PostMapping public ResponseEntity<BankAccount> register(@RequestBody @Valid RequestBankAccountDTO data) {
        return ResponseEntity.status(201).body(service.register(data));
    }
    @PatchMapping("/{id}") public BankAccount update(@PathVariable Long id,@RequestBody @Valid PatchBankAccountDTO data) {
        return service.update(id,data);
    }
}
