package com.pi.erp.financial.receivable;
import com.pi.erp.financial.*;
import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/receivables")
public class ReceivableController {
    @Autowired private ReceivableRepository repository;
    @Autowired private ReceivablePaymentRepository payments;
    @Autowired private ReceivableReversalRepository reversals;
    @Autowired private FinancialService service;
    @Autowired private FinancialTotalsService totals;
    @GetMapping("/summary") public FinancialTotalsService.Totals summary(FinancialFilter filter) {
        return totals.summarize(Receivable.class,filter.specification("customerId","orderId"));
    }
    @GetMapping public Page<Receivable> search(FinancialFilter filter,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return repository.findAll(filter.specification("customerId","orderId"),FinancialPages.of(page,size));
    }
    @GetMapping("/{id}") public Receivable get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conta financeira nÃ£o encontrada."));
    }
    @GetMapping("/{id}/settlements") public Page<ReceivablePayment> history(@PathVariable Long id,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        get(id);
        return payments.findByReceivableId(id,FinancialPages.of(page,size));
    }
    @GetMapping("/{id}/settlements/{paymentId}/reversals") public Page<ReceivableReversal> reversals(
        @PathVariable Long id,@PathVariable Long paymentId,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="25") int size) {
        get(id);
        var payment=payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Baixa nÃ£o encontrada."));
        if(!payment.getReceivableId().equals(id)) throw new ResourceNotFoundException("Baixa nÃ£o encontrada.");
        return reversals.findByPaymentId(paymentId,FinancialPages.of(page,size));
    }
    // 200 for both creation and identical retry: the title + operation is the natural key.
    @PostMapping("/{id}/settlements") public ReceivablePayment settle(@PathVariable Long id,@RequestBody @Valid SettlementDTO data) {
        return service.settleReceivable(id,data);
    }
    @PostMapping("/{id}/settlements/{paymentId}/reversals") public ReceivableReversal reverse(
        @PathVariable Long id,@PathVariable Long paymentId,@RequestBody @Valid ReversalDTO data) {
        return service.reverseReceivable(id,paymentId,data);
    }
}
