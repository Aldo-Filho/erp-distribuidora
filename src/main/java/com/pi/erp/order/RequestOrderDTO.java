package com.pi.erp.order;

import com.pi.erp.customer.Customer;

public record RequestOrderDTO(
        
        PaymentType paymentType,
        StatusType statusType,
        Long customerId
) {
}
