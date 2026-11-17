package com.pi.erp.financial.cashflow;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
public record CashFlowFilter(LocalDate from, LocalDate to, Long bankAccountId, Long categoryId,
    String kind, String direction) {
    public Specification<CashFlowEntry> specification() {
        if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("Período inválido.");
        if(kind!=null && !java.util.Set.of("ACTUAL","PROJECTED").contains(kind)) throw new IllegalArgumentException("Tipo inválido.");
        if(direction!=null && !java.util.Set.of("RECEIVABLE","PAYABLE").contains(direction)) throw new IllegalArgumentException("Origem inválida.");
        Specification<CashFlowEntry> spec=Specification.allOf();
        if(from!=null) spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("occurredOn"),from));
        if(to!=null) spec=spec.and((r,q,c)->c.lessThanOrEqualTo(r.get("occurredOn"),to));
        if(bankAccountId!=null) spec=spec.and((r,q,c)->c.equal(r.get("bankAccountId"),bankAccountId));
        if(categoryId!=null) spec=spec.and((r,q,c)->c.equal(r.get("categoryId"),categoryId));
        if(kind!=null) spec=spec.and((r,q,c)->c.equal(r.get("kind"),kind));
        if(direction!=null) spec=spec.and((r,q,c)->c.equal(r.get("direction"),direction));
        return spec;
    }
}
