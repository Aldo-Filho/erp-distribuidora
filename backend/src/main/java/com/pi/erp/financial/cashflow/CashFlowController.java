package com.pi.erp.financial.cashflow;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.financial.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/cash-flow")
public class CashFlowController {
    @Autowired private CashFlowRepository repository;
    @Autowired private FinancialTotalsService totals;
    @Autowired private JdbcTemplate jdbc;
    @GetMapping public Page<CashFlowEntry> search(CashFlowFilter filter,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        CurrentUser.require("finance.read");
        FinancialPages.of(page,size);
        return repository.findAll(filter.specification(),PageRequest.of(page,size,Sort.by("occurredOn").descending().and(Sort.by("id"))));
    }
    public record Summary(FinancialTotalsService.Totals actual, FinancialTotalsService.Totals projected) {}
    @GetMapping("/summary") public Summary summary(CashFlowFilter filter) {
        var spec=filter.specification();
        return new Summary(totals.summarize(CashFlowEntry.class,spec.and((r,q,c)->c.equal(r.get("kind"),"ACTUAL"))),
            totals.summarize(CashFlowEntry.class,spec.and((r,q,c)->c.equal(r.get("kind"),"PROJECTED"))));
    }
    public record Balance(LocalDate through, Long bankAccountId, BigDecimal initialBalance, BigDecimal movements, BigDecimal balance) {}
    @GetMapping("/balance") public Balance balance(@RequestParam(required=false) Long bankAccountId,
        @RequestParam(required=false) LocalDate through) {
        CurrentUser.require("finance.read");
        LocalDate end=through==null ? LocalDate.now() : through;
        String bankClause=bankAccountId==null ? "" : " WHERE bank_account_id = ?";
        Object[] args=bankAccountId==null ? new Object[]{} : new Object[]{bankAccountId};
        BigDecimal initial=jdbc.queryForObject("SELECT COALESCE(sum(initial_balance),0) FROM bank_accounts"+bankClause,BigDecimal.class,args);
        BigDecimal movements=jdbc.queryForObject("SELECT COALESCE(sum(amount),0) FROM financial_cash_flow WHERE kind='ACTUAL' AND occurred_on <= ?"+
            (bankAccountId==null ? "" : " AND bank_account_id = ?"),BigDecimal.class,
            bankAccountId==null ? new Object[]{end} : new Object[]{end,bankAccountId});
        return new Balance(end,bankAccountId,initial,movements,initial.add(movements));
    }
}
