package com.pi.erp.company;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyProfileRepository {
    @Autowired private JdbcTemplate jdbc;

    public record Link(Long companyId, long version) {}

    public Link link(boolean lock) {
        return jdbc.queryForObject(
            "SELECT company_id, profile_version FROM company_appearance WHERE id=1" +
                (lock ? " FOR UPDATE" : ""),
            (rs, n) -> new Link(rs.getObject("company_id", Long.class), rs.getLong("profile_version"))
        );
    }

    public CompanyProfile find() {
        // One statement keeps the company, address and version from the same committed snapshot.
        return jdbc.queryForObject("""
            SELECT c.*, a.profile_version, d.country, d.state, d.city, d.street,
                   d.neighborhood, d.number, d.complement, d.zip_code, d.company_address_id
            FROM company_appearance a
            LEFT JOIN companies c ON c.company_id=a.company_id
            LEFT JOIN LATERAL (
                SELECT * FROM company_addresses WHERE company_id=c.company_id
                ORDER BY company_address_id LIMIT 1
            ) d ON true
            WHERE a.id=1
            """, (rs, n) -> new CompanyProfile(
                rs.getObject("company_id", Long.class), rs.getString("legal_name"),
                rs.getString("trade_name"), rs.getString("tax_id"), rs.getString("state_reg"),
                rs.getString("municipal_reg"), rs.getString("tax_regime") == null ? null :
                    CompanyProfile.TaxRegime.valueOf(rs.getString("tax_regime")),
                rs.getObject("company_address_id") == null ? null : new CompanyProfile.Address(
                    rs.getString("country"), rs.getString("state"), rs.getString("city"),
                    rs.getString("street"), rs.getString("neighborhood"), rs.getString("number"),
                    rs.getString("complement"), rs.getString("zip_code")),
                rs.getLong("profile_version")
            ));
    }

    public boolean taxIdExists(String taxId, Long exceptId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM companies
            WHERE upper(regexp_replace(tax_id, '[./[:space:]-]', '', 'g'))=?
            AND (?::bigint IS NULL OR company_id<>?))
            """, Boolean.class, taxId, exceptId, exceptId));
    }

    public void save(Long id, CompanyProfile data, Long actor) {
        if (id == null) {
            id = jdbc.queryForObject("""
                INSERT INTO companies(legal_name,trade_name,tax_id,state_reg,municipal_reg,tax_regime)
                VALUES (?,?,?,?,?,?) RETURNING company_id
                """, Long.class, data.legalName(), data.tradeName(), data.taxId(), data.stateReg(),
                data.municipalReg(), data.taxRegime().name());
        } else {
            jdbc.update("""
                UPDATE companies SET legal_name=?,trade_name=?,tax_id=?,state_reg=?,municipal_reg=?,tax_regime=?
                WHERE company_id=?
                """, data.legalName(), data.tradeName(), data.taxId(), data.stateReg(),
                data.municipalReg(), data.taxRegime().name(), id);
        }
        var address = data.address();
        Long addressId = jdbc.query("""
            SELECT company_address_id FROM company_addresses WHERE company_id=?
            ORDER BY company_address_id LIMIT 1
            """, (rs, n) -> rs.getLong(1), id).stream().findFirst().orElse(null);
        if (addressId == null) {
            jdbc.update("""
                INSERT INTO company_addresses(country,state,city,street,neighborhood,number,complement,zip_code,company_id)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, address.country(), address.state(), address.city(), address.street(),
                address.neighborhood(), address.number(), address.complement(), address.zipCode(), id);
        } else {
            jdbc.update("""
                UPDATE company_addresses SET country=?,state=?,city=?,street=?,neighborhood=?,number=?,complement=?,zip_code=?
                WHERE company_address_id=?
                """, address.country(), address.state(), address.city(), address.street(),
                address.neighborhood(), address.number(), address.complement(), address.zipCode(), addressId);
        }
        jdbc.update("""
            UPDATE company_appearance SET company_id=?, profile_version=profile_version+1,
                updated_at=now(), updated_by=? WHERE id=1
            """, id, actor);
    }
}
