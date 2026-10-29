package com.pi.erp.price.item;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceTableItemRepository
    extends JpaRepository<PriceTableItem, Long>, JpaSpecificationExecutor<PriceTableItem>
{
    @Query(
        value = "SELECT * FROM price_table_items WHERE price_table_item_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<PriceTableItem> findByIdForUpdate(
        @Param("id") Long id
    );

    boolean existsByPriceTableId(
        Long priceTableId
    );

    boolean existsByPriceTableIdAndProductId(
        Long priceTableId,
        Long productId
    );

    boolean existsByPriceTableIdAndProductIdAndIdNot(
        Long priceTableId,
        Long productId,
        Long id
    );

    @Query(
        value = """
            SELECT item.price
            FROM price_table_items item
            JOIN price_tables price_table ON price_table.price_table_id = item.price_table_id
            JOIN customers customer ON customer.price_table_id = price_table.price_table_id
            WHERE customer.customer_id = :customerId
              AND item.product_id = :productId
              AND price_table.active = TRUE
              AND (price_table.start_date IS NULL OR price_table.start_date <= :date)
              AND (price_table.end_date IS NULL OR price_table.end_date >= :date)
            """,
        nativeQuery = true
    )
    Optional<BigDecimal> findApplicablePrice(
        @Param("customerId") Long customerId,
        @Param("productId") Long productId,
        @Param("date") LocalDate date
    );
}
