package com.pi.erp.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository
    extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order>
{
    @Query(
        value = "SELECT * FROM orders WHERE order_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<Order> findByIdForUpdate(
        @Param("id") Long id
    );
}
