package com.pi.erp.financial.cashflow;
import org.springframework.data.jpa.repository.*;
public interface CashFlowRepository extends JpaRepository<CashFlowEntry,String>, JpaSpecificationExecutor<CashFlowEntry> {}
