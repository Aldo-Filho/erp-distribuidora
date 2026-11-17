package com.pi.erp.financial;
import com.pi.erp.order.PaymentMethod;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
public record FinancialFilter(FinancialStatus status, Long categoryId, Long partyId,
    Long documentId, PaymentMethod paymentMethod, LocalDate dueFrom, LocalDate dueTo) {
    public <T extends FinancialTitle> Specification<T> specification(String party, String document) {
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo))
            throw new IllegalArgumentException("A data inicial não pode ser posterior à final.");
        Specification<T> spec = Specification.allOf();
        if (status != null) spec=spec.and((r,q,c)->c.equal(r.get("status"),status));
        if (categoryId != null) spec=spec.and((r,q,c)->c.equal(r.get("categoryId"),categoryId));
        if (partyId != null) spec=spec.and((r,q,c)->c.equal(r.get(party),partyId));
        if (documentId != null) spec=spec.and((r,q,c)->c.equal(r.get(document),documentId));
        if (paymentMethod != null) spec=spec.and((r,q,c)->c.equal(r.get("paymentMethod"),paymentMethod));
        if (dueFrom != null) spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("dueDate"),dueFrom));
        if (dueTo != null) spec=spec.and((r,q,c)->c.lessThanOrEqualTo(r.get("dueDate"),dueTo));
        return spec;
    }
}
