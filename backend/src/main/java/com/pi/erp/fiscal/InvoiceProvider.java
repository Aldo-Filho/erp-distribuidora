package com.pi.erp.fiscal;

import com.pi.erp.order.Order;

/** Produces an immutable demonstrative snapshot; no authorization or external side effects.
 * A real fiscal provider needs its own authorization/event workflow, never a silent replacement.
 */
public interface InvoiceProvider {
    DemonstrativeInvoice.Snapshot snapshot(Order order);
}
