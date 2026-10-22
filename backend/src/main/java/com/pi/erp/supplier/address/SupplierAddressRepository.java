package com.pi.erp.supplier.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierAddressRepository
    extends JpaRepository<SupplierAddress, Long>, JpaSpecificationExecutor<SupplierAddress>
{
    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM supplier_addresses address
                WHERE address.supplier_id = :supplierId
                    AND lower(btrim(coalesce(address.address_type, ''))) =
                        lower(btrim(coalesce(cast(:addressType AS text), '')))
                    AND lower(btrim(coalesce(address.country, ''))) =
                        lower(btrim(coalesce(cast(:country AS text), '')))
                    AND lower(btrim(coalesce(address.state, ''))) =
                        lower(btrim(coalesce(cast(:state AS text), '')))
                    AND lower(btrim(coalesce(address.city, ''))) =
                        lower(btrim(coalesce(cast(:city AS text), '')))
                    AND lower(btrim(coalesce(address.street, ''))) =
                        lower(btrim(coalesce(cast(:street AS text), '')))
                    AND lower(btrim(coalesce(address.zip_code, ''))) =
                        lower(btrim(coalesce(cast(:zipCode AS text), '')))
                    AND lower(btrim(coalesce(address.number, ''))) =
                        lower(btrim(coalesce(cast(:number AS text), '')))
                    AND lower(btrim(coalesce(address.complement, ''))) =
                        lower(btrim(coalesce(cast(:complement AS text), '')))
            )
            """,
        nativeQuery = true
    )
    boolean existsDuplicateAddress(
        @Param("supplierId") Long supplierId,
        @Param("addressType") String addressType,
        @Param("country") String country,
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
                FROM supplier_addresses address
                WHERE address.supplier_id = :supplierId
                    AND address.supplier_address_id <> :id
                    AND lower(btrim(coalesce(address.address_type, ''))) =
                        lower(btrim(coalesce(cast(:addressType AS text), '')))
                    AND lower(btrim(coalesce(address.country, ''))) =
                        lower(btrim(coalesce(cast(:country AS text), '')))
                    AND lower(btrim(coalesce(address.state, ''))) =
                        lower(btrim(coalesce(cast(:state AS text), '')))
                    AND lower(btrim(coalesce(address.city, ''))) =
                        lower(btrim(coalesce(cast(:city AS text), '')))
                    AND lower(btrim(coalesce(address.street, ''))) =
                        lower(btrim(coalesce(cast(:street AS text), '')))
                    AND lower(btrim(coalesce(address.zip_code, ''))) =
                        lower(btrim(coalesce(cast(:zipCode AS text), '')))
                    AND lower(btrim(coalesce(address.number, ''))) =
                        lower(btrim(coalesce(cast(:number AS text), '')))
                    AND lower(btrim(coalesce(address.complement, ''))) =
                        lower(btrim(coalesce(cast(:complement AS text), '')))
            )
            """,
        nativeQuery = true
    )
    boolean existsDuplicateAddressAndIdNot(
        @Param("supplierId") Long supplierId,
        @Param("addressType") String addressType,
        @Param("country") String country,
        @Param("state") String state,
        @Param("city") String city,
        @Param("street") String street,
        @Param("zipCode") String zipCode,
        @Param("number") String number,
        @Param("complement") String complement,
        @Param("id") Long id
    );
}
