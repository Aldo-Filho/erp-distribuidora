package com.pi.erp.financial.bankaccount;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BankAccountService {
    @Autowired private BankAccountRepository repository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private jakarta.persistence.EntityManager em;
    @Transactional public BankAccount register(RequestBankAccountDTO data) {
        CurrentUser.require("finance.configure");
        BankAccount account=new BankAccount(); account.setName(name(data.name()));
        account.setType(data.type()); account.setInitialBalance(data.initialBalance());
        return repository.save(account);
    }
    @Transactional public BankAccount update(Long id, PatchBankAccountDTO data) {
        CurrentUser.require("finance.configure");
        BankAccount account=repository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Conta bancária não encontrada."));
        em.refresh(account);
        boolean moneyChange=(data.initialBalance()!=null && data.initialBalance().compareTo(account.getInitialBalance())!=0)
            || (data.type()!=null && data.type()!=account.getType());
        if(moneyChange && Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from accounts_receivable_payments where bank_account_id=?)
                or exists(select 1 from accounts_payable_payments where bank_account_id=?)
            """,Boolean.class,id,id))) throw new IllegalArgumentException("Saldo inicial e tipo não podem mudar após movimentação.");
        if(data.name()!=null) account.setName(name(data.name()));
        if(data.type()!=null) account.setType(data.type());
        if(data.initialBalance()!=null) account.setInitialBalance(data.initialBalance());
        if(data.active()!=null) account.setActive(data.active());
        return account;
    }
    private String name(String name) {
        if(name==null || name.isBlank() || name.trim().length()>100) throw new IllegalArgumentException("Nome inválido.");
        return name.trim();
    }
}
