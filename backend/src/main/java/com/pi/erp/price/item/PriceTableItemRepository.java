package com.pi.erp.price.item;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceTableItemRepository
    extends JpaRepository<PriceTableItem, Long>, JpaSpecificationExecutor<PriceTableItem>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "presentation.product.catalog.brand",
            "presentation.product.catalog.category",
            "priceTable",
        }
    )
    @Override
    Optional<PriceTableItem> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "presentation.product.catalog.brand",
            "presentation.product.catalog.category",
            "priceTable",
        }
    )
    @Override
    List<PriceTableItem> findAll(Specification<PriceTableItem> spec);

    @Query(
        value = "SELECT * FROM price_table_items WHERE price_table_item_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<PriceTableItem> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPriceTableId(Long priceTableId);

    @Query(
        value = """
            SELECT item.price
            FROM price_table_items item
            JOIN price_tables price_table ON price_table.price_table_id = item.price_table_id
            JOIN customers customer ON customer.price_table_id = price_table.price_table_id
            WHERE customer.customer_id = :customerId
              AND item.product_id = :productId
              AND item.presentation_id IS NULL
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

    @Query(
        value = """
            SELECT item.price FROM price_table_items item
            JOIN price_tables t ON t.price_table_id=item.price_table_id
            JOIN customers c ON c.price_table_id=t.price_table_id
            WHERE c.customer_id=:customerId AND item.product_id=:productId AND item.presentation_id=:presentationId
            AND t.active AND (t.start_date IS NULL OR t.start_date<=:date) AND (t.end_date IS NULL OR t.end_date>=:date)
        """,
        nativeQuery = true
    )
    Optional<BigDecimal> findApplicablePresentationPrice(
        @Param("customerId") Long customerId,
        @Param("productId") Long productId,
        @Param("presentationId") Long presentationId,
        @Param("date") LocalDate date
    );

    @Query(
        value = """
            SELECT EXISTS(SELECT 1 FROM price_table_items WHERE price_table_id=:tableId AND product_id=:productId
            AND presentation_id IS NOT DISTINCT FROM :presentationId AND price_table_item_id<>coalesce(:id,0))
        """,
        nativeQuery = true
    )
    boolean existsByIdentity(
        @Param("tableId") Long tableId,
        @Param("productId") Long productId,
        @Param("presentationId") Long presentationId,
        @Param("id") Long id
    );
}
