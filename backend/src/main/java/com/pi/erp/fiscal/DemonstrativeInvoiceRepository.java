package com.pi.erp.fiscal;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class DemonstrativeInvoiceRepository {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private RowMapper<DemonstrativeInvoice> mapper() {
        return (rs,n) -> {
            long id=rs.getLong("demonstrative_invoice_id");
            return new DemonstrativeInvoice(id,String.format(Locale.ROOT,"DEMO-%06d",id),
                rs.getLong("order_id"),rs.getObject("issued_at",OffsetDateTime.class),DemonstrativeInvoice.NOTICE,
                json.readValue(rs.getString("snapshot"),DemonstrativeInvoice.Snapshot.class));
        };
    }
    public Optional<DemonstrativeInvoice> findByOrder(Long orderId) {
        return jdbc.query("SELECT * FROM demonstrative_invoices WHERE order_id=?",mapper(),orderId).stream().findFirst();
    }
    public DemonstrativeInvoice insert(Long orderId,Long actor,OffsetDateTime issuedAt,DemonstrativeInvoice.Snapshot snapshot) {
        return jdbc.queryForObject("""
            INSERT INTO demonstrative_invoices(order_id,issued_by,issued_at,snapshot)
            VALUES (?,?,?,CAST(? AS jsonb)) RETURNING *
            """,mapper(),orderId,actor,issuedAt,json.writeValueAsString(snapshot));
    }
}
