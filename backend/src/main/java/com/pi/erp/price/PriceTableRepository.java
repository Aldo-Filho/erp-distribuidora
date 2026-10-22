package com.pi.erp.price;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceTableRepository
    extends JpaRepository<PriceTable, Long>, JpaSpecificationExecutor<PriceTable>
{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT priceTable FROM PriceTable priceTable WHERE priceTable.id = :id")
    Optional<PriceTable> findByIdForUpdate(
        @Param("id") Long id
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM price_tables
                WHERE lower(btrim(name)) = lower(btrim(cast(:name AS text)))
            )
            """,
        nativeQuery = true
    )
    boolean existsByNormalizedName(
        @Param("name") String name
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM price_tables
                WHERE lower(btrim(name)) = lower(btrim(cast(:name AS text)))
                  AND price_table_id <> :id
            )
            """,
        nativeQuery = true
    )
    boolean existsByNormalizedNameAndIdNot(
        @Param("name") String name,
        @Param("id") Long id
    );
}
