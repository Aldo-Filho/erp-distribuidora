package com.pi.erp.financial;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.financial.bankaccount.*;
import com.pi.erp.financial.category.*;
import com.pi.erp.financial.receivable.*;
import com.pi.erp.financial.payable.*;
import com.pi.erp.order.*;
import com.pi.erp.purchase.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinancialService {
    @Autowired private ReceivableRepository receivables;
    @Autowired private PayableRepository payables;
    @Autowired private ReceivablePaymentRepository receipts;
    @Autowired private PayablePaymentRepository payments;
    @Autowired private ReceivableReversalRepository receiptReversals;
    @Autowired private PayableReversalRepository paymentReversals;
    @Autowired private FinancialCategoryRepository categories;
    @Autowired private BankAccountRepository banks;
    @Autowired private OrderRepository orders;
    @Autowired private PurchaseRepository purchases;
    @Autowired private EntityManager em;

    // Caller owns the document lock. Both financial and commercial writes share its transaction.
    public void confirmOrder(Order order) {
        if (receivables.findByOrderId(order.getId()).isPresent())
            throw new IllegalArgumentException("Já existe uma conta financeira para esta venda.");
        PaymentMethod method=order.getPaymentMethod();
        if (method==null) throw new IllegalArgumentException("Escolha um método de pagamento para confirmar.");
        LocalDate today=LocalDate.now();
        LocalDate due=today;
        if (method==PaymentMethod.CREDIT_CARD || method==PaymentMethod.BOLETO) {
            due=order.getPaymentDueDate();
            if(due==null || !due.isAfter(today))
                throw new IllegalArgumentException("Crédito e boleto exigem vencimento futuro na confirmação.");
        }
        order.setPaymentDueDate(due);
        Receivable title=new Receivable();
        initialize(title, CategoryType.REVENUE, order.getTotalAmount(), due, "Venda #"+order.getId());
        title.setOrderId(order.getId());
        title.setCustomerId(order.getCustomer().getId());
        title.setPaymentMethod(method);
        receivables.save(title);
    }

    public void confirmPurchase(Purchase purchase) {
        if(payables.findByPurchaseId(purchase.getId()).isPresent())
            throw new IllegalArgumentException("Já existe uma conta financeira para esta compra.");
        LocalDate due=purchase.getExpectedDeliveryDate();
        if(due==null || due.isBefore(LocalDate.now())) due=LocalDate.now();
        Payable title=new Payable();
        initialize(title, CategoryType.EXPENSE, purchase.getTotalAmount(), due, "Compra #"+purchase.getId());
        title.setPurchaseId(purchase.getId());
        title.setSupplierId(purchase.getSupplier().getId());
        payables.save(title);
    }

    private void initialize(FinancialTitle title, CategoryType type, java.math.BigDecimal amount,
                            LocalDate due, String description) {
        FinancialCategory category=categories.findDefaultForUpdate(type.name()).orElseThrow(() ->
            new IllegalArgumentException("Configure a categoria financeira padrão de "+type+"."));
        em.refresh(category);
        if(!category.isActive() || !category.isDefaultForDocuments()) throw new IllegalArgumentException("A categoria financeira padrão está inativa.");
        if(amount==null || amount.signum()<0) throw new IllegalArgumentException("Total financeiro inválido.");
        title.setCategoryId(category.getId());
        title.setAmount(amount);
        title.setDueDate(due);
        title.setDescription(description);
        title.setStatus(FinancialStatus.OPEN);
        title.setCreatedBy(CurrentUser.requireId());
    }

    public void cancelOrder(Order order) {
        Receivable found=receivables.findByOrderId(order.getId()).orElse(null);
        if(found==null) return; // A pending or historical document may have no title.
        Receivable title=receivables.findByIdForUpdate(found.getId()).orElseThrow();
        em.refresh(title);
        boolean unreversed=receipts.findByReceivableIdOrderById(title.getId()).stream()
            .anyMatch(p -> receiptReversals.findByPaymentId(p.getId()).isEmpty());
        cancel(title,unreversed);
    }

    public void cancelPurchase(Purchase purchase) {
        Payable found=payables.findByPurchaseId(purchase.getId()).orElse(null);
        if(found==null) return;
        Payable title=payables.findByIdForUpdate(found.getId()).orElseThrow();
        em.refresh(title);
        boolean unreversed=payments.findByPayableIdOrderById(title.getId()).stream()
            .anyMatch(p -> paymentReversals.findByPaymentId(p.getId()).isEmpty());
        cancel(title,unreversed);
    }

    private void cancel(FinancialTitle title, boolean unreversed) {
        if(unreversed || title.getStatus()==FinancialStatus.PAID || title.getStatus()==FinancialStatus.PARTIAL)
            throw new IllegalArgumentException("Registre o estorno antes de cancelar o documento.");
        title.setStatus(FinancialStatus.CANCELED);
        title.setUpdatedBy(CurrentUser.requireId());
    }

    public ReceivablePayment settleReceivable(Long id, SettlementDTO data) {
        CurrentUser.require("finance.settle");
        Receivable title=lockedReceivable(id);
        Order order=orders.findById(title.getOrderId()).orElseThrow();
        if(order.getStatus()!=OrderStatus.CONFIRMED && order.getStatus()!=OrderStatus.INVOICED)
            throw new IllegalArgumentException("A venda não está confirmada ou despachada.");
        var existing=receipts.findByReceivableIdOrderById(id);
        if(!existing.isEmpty()) {
            if(existing.size()==1 && title.getStatus()==FinancialStatus.PAID &&
                receiptReversals.findByPaymentId(existing.getFirst().getId()).isEmpty() &&
                same(existing.getFirst(),data)) return existing.getFirst();
            throw new IllegalArgumentException("Esta conta já possui recebimento; uma nova baixa não é permitida.");
        }
        if(data.paymentMethod()!=title.getPaymentMethod())
            throw new IllegalArgumentException("Use o método escolhido na venda.");
        validateSettlement(title,data);
        ReceivablePayment payment=new ReceivablePayment();
        payment.setReceivableId(id);
        fill(payment,data);
        title.setStatus(FinancialStatus.PAID);
        title.setUpdatedBy(CurrentUser.requireId());
        return receipts.save(payment);
    }

    public PayablePayment settlePayable(Long id, SettlementDTO data) {
        CurrentUser.require("finance.settle");
        Payable title=lockedPayable(id);
        Purchase purchase=purchases.findById(title.getPurchaseId()).orElseThrow();
        if(purchase.getStatus()!=PurchaseStatus.CONFIRMED && purchase.getStatus()!=PurchaseStatus.PARTIALLY_RECEIVED
            && purchase.getStatus()!=PurchaseStatus.RECEIVED)
            throw new IllegalArgumentException("A compra não está confirmada ou recebida.");
        var existing=payments.findByPayableIdOrderById(id);
        if(!existing.isEmpty()) {
            if(existing.size()==1 && title.getStatus()==FinancialStatus.PAID &&
                paymentReversals.findByPaymentId(existing.getFirst().getId()).isEmpty() &&
                same(existing.getFirst(),data)) return existing.getFirst();
            throw new IllegalArgumentException("Esta conta já possui pagamento; uma nova baixa não é permitida.");
        }
        validateSettlement(title,data);
        title.setPaymentMethod(data.paymentMethod());
        PayablePayment payment=new PayablePayment();
        payment.setPayableId(id);
        fill(payment,data);
        title.setStatus(FinancialStatus.PAID);
        title.setUpdatedBy(CurrentUser.requireId());
        return payments.save(payment);
    }

    private void validateSettlement(FinancialTitle title, SettlementDTO data) {
        if(title.getStatus()!=FinancialStatus.OPEN && title.getStatus()!=FinancialStatus.OVERDUE)
            throw new IllegalArgumentException("Somente contas abertas sem pagamentos anteriores aceitam baixa integral.");
        if(data.amount()==null || data.amount().compareTo(title.getAmount())!=0)
            throw new IllegalArgumentException("A baixa deve corresponder ao valor integral da conta.");
        if(data.paymentMethod()==null || data.paidAt()==null || data.paidAt().isAfter(LocalDate.now())
            || data.paidAt().isBefore(title.getIssuedAt().toLocalDate()))
            throw new IllegalArgumentException("Data ou método da baixa inválido.");
        BankAccount bank=banks.findByIdForUpdate(data.bankAccountId()).orElseThrow(() -> missing("Conta bancária"));
        em.refresh(bank);
        if(!bank.isActive()) throw new IllegalArgumentException("A conta de destino/origem está inativa.");
    }

    private void fill(FinancialPayment payment, SettlementDTO data) {
        payment.setBankAccountId(data.bankAccountId());
        payment.setPaidAmount(data.amount());
        payment.setPaidAt(data.paidAt());
        payment.setPaymentMethod(data.paymentMethod());
        payment.setNotes(data.notes());
        payment.setUserId(CurrentUser.requireId());
    }

    private boolean same(FinancialPayment p, SettlementDTO data) {
        return !p.isLegacy() && p.getBankAccountId().equals(data.bankAccountId()) &&
            p.getPaidAmount().compareTo(data.amount())==0 && p.getPaidAt().equals(data.paidAt()) &&
            p.getPaymentMethod()==data.paymentMethod() && java.util.Objects.equals(p.getNotes(),data.notes());
    }

    public ReceivableReversal reverseReceivable(Long id, Long paymentId, ReversalDTO data) {
        CurrentUser.require("finance.reverse");
        Receivable title=lockedReceivable(id);
        ReceivablePayment payment=receipts.findById(paymentId).orElseThrow(() -> missing("Recebimento"));
        if(!payment.getReceivableId().equals(id)) throw missing("Recebimento");
        var existing=receiptReversals.findByPaymentId(paymentId);
        if(existing.isPresent()) {
            if(existing.get().getReversedAt().equals(data.reversedAt()) && existing.get().getReason().equals(data.reason().trim()))
                return existing.get();
            throw new IllegalArgumentException("O recebimento já foi estornado com outros dados.");
        }
        Order order=orders.findById(title.getOrderId()).orElseThrow();
        if(order.getStatus()!=OrderStatus.CONFIRMED)
            throw new IllegalArgumentException("Só é permitido estornar venda confirmada antes do despacho.");
        if(receipts.findByReceivableIdOrderById(id).size()!=1)
            throw new IllegalArgumentException("O histórico parcial exige conciliação específica antes do estorno.");
        validateReversal(title,payment,data);
        ReceivableReversal reversal=new ReceivableReversal();
        reversal.setPaymentId(paymentId); reversal.setUserId(CurrentUser.requireId());
        reversal.setReversedAt(data.reversedAt()); reversal.setReason(data.reason().trim());
        title.setStatus(FinancialStatus.OPEN); title.setUpdatedBy(CurrentUser.requireId());
        return receiptReversals.save(reversal);
    }

    public PayableReversal reversePayable(Long id, Long paymentId, ReversalDTO data) {
        CurrentUser.require("finance.reverse");
        Payable title=lockedPayable(id);
        PayablePayment payment=payments.findById(paymentId).orElseThrow(() -> missing("Pagamento"));
        if(!payment.getPayableId().equals(id)) throw missing("Pagamento");
        var existing=paymentReversals.findByPaymentId(paymentId);
        if(existing.isPresent()) {
            if(existing.get().getReversedAt().equals(data.reversedAt()) && existing.get().getReason().equals(data.reason().trim()))
                return existing.get();
            throw new IllegalArgumentException("O pagamento já foi estornado com outros dados.");
        }
        if(purchases.findById(title.getPurchaseId()).orElseThrow().getStatus()!=PurchaseStatus.CONFIRMED)
            throw new IllegalArgumentException("Só é permitido estornar compra confirmada antes do recebimento.");
        if(payments.findByPayableIdOrderById(id).size()!=1)
            throw new IllegalArgumentException("O histórico parcial exige conciliação específica antes do estorno.");
        validateReversal(title,payment,data);
        PayableReversal reversal=new PayableReversal();
        reversal.setPaymentId(paymentId); reversal.setUserId(CurrentUser.requireId());
        reversal.setReversedAt(data.reversedAt()); reversal.setReason(data.reason().trim());
        title.setStatus(FinancialStatus.OPEN); title.setUpdatedBy(CurrentUser.requireId());
        return paymentReversals.save(reversal);
    }

    private void validateReversal(FinancialTitle title, FinancialPayment payment, ReversalDTO data) {
        if(title.getStatus()!=FinancialStatus.PAID || payment.getPaidAmount().compareTo(title.getAmount())!=0)
            throw new IllegalArgumentException("Somente uma baixa integral pode ser estornada por este fluxo.");
        if(data.reversedAt()==null || data.reversedAt().isBefore(payment.getPaidAt())
            || data.reversedAt().isAfter(LocalDate.now()) || data.reason()==null || data.reason().isBlank())
            throw new IllegalArgumentException("Informe data válida e motivo para o estorno.");
        // Lock the original account even when inactive: an inactivation cannot erase its history.
        banks.findByIdForUpdate(payment.getBankAccountId()).orElseThrow(() -> missing("Conta bancária"));
    }

    // A reversal is a prerequisite to cancellation, not a new payment cycle.
    public void requireOrderNotReversed(Long orderId) {
        receivables.findByOrderId(orderId).ifPresent(title -> {
            if(receipts.findByReceivableIdOrderById(title.getId()).stream()
                .anyMatch(p -> receiptReversals.findByPaymentId(p.getId()).isPresent()))
                throw new IllegalArgumentException("A venda estornada deve ser cancelada antes de uma nova venda.");
        });
    }
    public void requirePurchaseNotReversed(Long purchaseId) {
        payables.findByPurchaseId(purchaseId).ifPresent(title -> {
            if(payments.findByPayableIdOrderById(title.getId()).stream()
                .anyMatch(p -> paymentReversals.findByPaymentId(p.getId()).isPresent()))
                throw new IllegalArgumentException("A compra estornada deve ser cancelada antes de uma nova compra.");
        });
    }

    // Global lock order: commercial document -> title -> bank. Never invert it in cancellation.
    private Receivable lockedReceivable(Long id) {
        Receivable found=receivables.findById(id).orElseThrow(() -> missing("Conta a receber"));
        if(found.getOrderId()==null) throw new IllegalArgumentException("Conta legada sem venda vinculada.");
        Order order=orders.findByIdForUpdate(found.getOrderId()).orElseThrow(() -> missing("Venda"));
        em.refresh(order);
        Receivable title=receivables.findByIdForUpdate(id).orElseThrow(); em.refresh(title); return title;
    }
    private Payable lockedPayable(Long id) {
        Payable found=payables.findById(id).orElseThrow(() -> missing("Conta a pagar"));
        if(found.getPurchaseId()==null) throw new IllegalArgumentException("Conta legada sem compra vinculada.");
        Purchase purchase=purchases.findByIdForUpdate(found.getPurchaseId()).orElseThrow(() -> missing("Compra"));
        em.refresh(purchase);
        Payable title=payables.findByIdForUpdate(id).orElseThrow(); em.refresh(title); return title;
    }
    private ResourceNotFoundException missing(String name) { return new ResourceNotFoundException(name+" não encontrado(a)."); }
}
