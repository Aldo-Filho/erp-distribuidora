package com.pi.erp.financial;
import com.pi.erp.auth.CurrentUser;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FinancialTotalsService {
    @Autowired private EntityManager em;
    public record Totals(Long count, BigDecimal totalAmount) {}
    @Transactional(readOnly=true)
    public <T> Totals summarize(Class<T> type, Specification<T> spec) {
        CurrentUser.require("finance.read");
        var cb=em.getCriteriaBuilder(); var query=cb.createTupleQuery(); var root=query.from(type);
        query.multiselect(cb.count(root),cb.coalesce(cb.sum(root.<BigDecimal>get("amount")),BigDecimal.ZERO));
        var predicate=spec.toPredicate(root,query,cb); if(predicate!=null) query.where(predicate);
        var result=em.createQuery(query).getSingleResult();
        return new Totals(result.get(0,Long.class),result.get(1,BigDecimal.class));
    }
}
