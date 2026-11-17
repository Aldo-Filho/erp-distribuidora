package com.pi.erp.financial.payable;
import com.pi.erp.financial.*;
import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/payables")
public class PayableController {
    @Autowired private PayableRepository repository;
    @Autowired private PayablePaymentRepository payments;
    @Autowired private PayableReversalRepository reversals;
    @Autowired private FinancialService service;
    @Autowired private FinancialTotalsService totals;
    @GetMapping("/summary") public FinancialTotalsService.Totals summary(FinancialFilter filter) {
        return totals.summarize(Payable.class,filter.specification("supplierId","purchaseId"));
    }
    @GetMapping public Page<Payable> search(FinancialFilter filter,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return repository.findAll(filter.specification("supplierId","purchaseId"),FinancialPages.of(page,size));
    }
    @GetMapping("/{id}") public Payable get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conta financeira nÃ£o encontrada."));
    }
    @GetMapping("/{id}/settlements") public Page<PayablePayment> history(@PathVariable Long id,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        get(id);
        return payments.findByPayableId(id,FinancialPages.of(page,size));
    }
    @GetMapping("/{id}/settlements/{paymentId}/reversals") public Page<PayableReversal> reversals(
        @PathVariable Long id,@PathVariable Long paymentId,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="25") int size) {
        get(id);
        var payment=payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Baixa nÃ£o encontrada."));
        if(!payment.getPayableId().equals(id)) throw new ResourceNotFoundException("Baixa nÃ£o encontrada.");
        return reversals.findByPaymentId(paymentId,FinancialPages.of(page,size));
    }
    // 200 for both creation and identical retry: the title + operation is the natural key.
    @PostMapping("/{id}/settlements") public PayablePayment settle(@PathVariable Long id,@RequestBody @Valid SettlementDTO data) {
        return service.settlePayable(id,data);
    }
    @PostMapping("/{id}/settlements/{paymentId}/reversals") public PayableReversal reverse(
        @PathVariable Long id,@PathVariable Long paymentId,@RequestBody @Valid ReversalDTO data) {
        return service.reversePayable(id,paymentId,data);
    }
}
