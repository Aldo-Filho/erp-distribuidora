package com.pi.erp.fiscal;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.order.Order;
import com.pi.erp.order.OrderStatus;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class DemonstrativeInvoiceService {
    @Autowired private DemonstrativeInvoiceRepository repository;
    @Autowired private InvoiceProvider provider;

    /** Caller holds the order write lock. Shares the dispatch transaction and rolls back with it. */
    @Transactional(propagation=Propagation.MANDATORY)
    public DemonstrativeInvoice issue(Order order) {
        CurrentUser.require("orders.dispatch");
        if(order.getStatus()!=OrderStatus.CONFIRMED) throw new IllegalArgumentException("Emissão demonstrativa somente no despacho de venda confirmada.");
        return repository.findByOrder(order.getId()).orElseGet(()->repository.insert(order.getId(),CurrentUser.requireId(),
            OffsetDateTime.now(ZoneId.of("America/Sao_Paulo")),provider.snapshot(order)));
    }
    @Transactional(readOnly=true)
    public DemonstrativeInvoice find(Long orderId) {
        CurrentUser.require("orders.read");
        return repository.findByOrder(orderId).orElseThrow(()->new ResourceNotFoundException("Documento demonstrativo não encontrado para esta venda."));
    }
}
