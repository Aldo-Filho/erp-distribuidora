package com.pi.erp.warehouse.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseAddressRepository
    extends JpaRepository<WarehouseAddress, Long>, JpaSpecificationExecutor<WarehouseAddress>
{
    boolean existsByWarehouseId(
        Long warehouseId
    );

    boolean existsByWarehouseIdAndIdNot(
        Long warehouseId,
        Long id
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM warehouse_addresses
                WHERE lower(btrim(state)) = lower(btrim(cast(:state AS text)))
                  AND lower(btrim(city)) = lower(btrim(cast(:city AS text)))
                  AND lower(btrim(street)) = lower(btrim(cast(:street AS text)))
                  AND lower(btrim(zip_code)) = lower(btrim(cast(:zipCode AS text)))
                  AND lower(btrim(number)) = lower(btrim(cast(:number AS text)))
                  AND lower(btrim(complement)) = lower(btrim(cast(:complement AS text)))
            )
            """,
        nativeQuery = true
    )
    boolean existsDuplicateAddress(
        @Param("state") String state,
        @Param("city") String city,
        @Param("street") String street,
        @Param("zipCode") String zipCode,
        @Param("number") String number,
        @Param("complement") String complement
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM warehouse_addresses
                WHERE lower(btrim(state)) = lower(btrim(cast(:state AS text)))
                  AND lower(btrim(city)) = lower(btrim(cast(:city AS text)))
                  AND lower(btrim(street)) = lower(btrim(cast(:street AS text)))
                  AND lower(btrim(zip_code)) = lower(btrim(cast(:zipCode AS text)))
                  AND lower(btrim(number)) = lower(btrim(cast(:number AS text)))
                  AND lower(btrim(complement)) = lower(btrim(cast(:complement AS text)))
                  AND warehouse_address_id <> :id
            )
            """,
        nativeQuery = true
    )
    boolean existsDuplicateAddressAndIdNot(
        @Param("state") String state,
        @Param("city") String city,
        @Param("street") String street,
        @Param("zipCode") String zipCode,
        @Param("number") String number,
        @Param("complement") String complement,
        @Param("id") Long id
    );
}
