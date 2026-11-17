package com.pi.erp.fiscal;

import com.pi.erp.order.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import static com.pi.erp.fiscal.DemonstrativeInvoice.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

@Component
public class DemonstrativeInvoiceProvider implements InvoiceProvider {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    @Override
    public Snapshot snapshot(Order order) {
        List<String> warnings = new ArrayList<>();
        var issuers = jdbc.queryForList("""
            SELECT c.company_id, c.legal_name, c.trade_name, c.tax_id, c.state_reg
            FROM company_appearance a LEFT JOIN companies c ON c.company_id=a.company_id
            WHERE a.id=1
            """);
        Map<String,Object> issuer = issuers.isEmpty() ? Map.of() : issuers.getFirst();
        Long companyId = issuer.get("company_id") instanceof Number n ? n.longValue() : null;
        List<Address> issuerAddresses = companyId == null ? List.of() : jdbc.query("""
            SELECT * FROM company_addresses WHERE company_id=? ORDER BY company_address_id
            """, (rs,n) -> new Address("EMISSOR",rs.getString("country"),rs.getString("state"),
                rs.getString("city"),rs.getString("street"),rs.getString("neighborhood"),
                rs.getString("number"),rs.getString("complement"),rs.getString("zip_code")),companyId);
        Party issuerSnapshot = new Party(text(issuer,"legal_name"),text(issuer,"trade_name"),
            text(issuer,"tax_id"),text(issuer,"state_reg"),issuerAddresses);
        if (companyId == null || issuerSnapshot.legalName() == null || issuerSnapshot.taxId() == null)
            warnings.add("Emissor não configurado ou incompleto. Os dados ausentes não foram inventados.");
        if (issuerAddresses.isEmpty()) warnings.add("Endereço do emissor não cadastrado na emissão.");
        else if (issuerAddresses.stream().anyMatch(this::incompleteAddress)) warnings.add("Endereço do emissor incompleto na emissão; confira rua, número, cidade, UF e CEP.");

        Long customerId = order.getCustomer().getId();
        var customers = jdbc.queryForList("SELECT legal_name,trade_name,tax_id,state_reg FROM customers WHERE customer_id=?",customerId);
        if (customers.isEmpty()) throw new IllegalArgumentException("Destinatário não encontrado para o documento demonstrativo.");
        var customer = customers.getFirst();
        List<Address> customerAddresses = jdbc.query("""
            SELECT * FROM customer_addresses WHERE customer_id=?
            ORDER BY CASE address_type WHEN 'MAIN' THEN 0 WHEN 'SHIPPING' THEN 1 ELSE 2 END,customer_address_id
            """, (rs,n) -> new Address(rs.getString("address_type"),null,rs.getString("state"),
                rs.getString("city"),rs.getString("street"),rs.getString("neighborhood"),
                rs.getString("number"),rs.getString("complement"),rs.getString("zip_code")),customerId);
        Party customerSnapshot = new Party(text(customer,"legal_name"),text(customer,"trade_name"),
            text(customer,"tax_id"),text(customer,"state_reg"),customerAddresses);
        if (customerAddresses.isEmpty()) warnings.add("Endereço do destinatário não cadastrado na emissão.");
        else if (customerAddresses.stream().anyMatch(this::incompleteAddress)) warnings.add("Endereço do destinatário incompleto na emissão; confira rua, número, cidade, UF e CEP.");

        List<Item> items = jdbc.query("""
            SELECT i.*, p.sku AS current_sku, c.name AS current_name
            FROM order_items i JOIN products p USING(product_id)
            JOIN product_catalogs c ON c.product_catalog_id=p.catalog_id
            WHERE i.order_id=? ORDER BY i.order_item_id
            """, (rs,n) -> {
                BigDecimal quantity=rs.getBigDecimal("quantity"),price=rs.getBigDecimal("unit_price"),discount=rs.getBigDecimal("discount");
                BigDecimal gross=quantity.multiply(price).setScale(2,RoundingMode.HALF_UP);
                String discountType=rs.getString("discount_type");
                BigDecimal deduction="PERCENTAGE".equals(discountType)
                    ? gross.multiply(discount).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP) : discount;
                String name=rs.getString("product_name_snapshot"),sku=rs.getString("sku_snapshot");
                if(name==null || sku==null) warnings.add("Item #"+rs.getLong("order_item_id")+": descrição/SKU ausente no histórico; capturado do cadastro no despacho.");
                String rawAttributes=rs.getString("attributes_snapshot");
                Map<String,String> attributes=rawAttributes==null || rawAttributes.isBlank() ? Map.of()
                    : json.readValue(rawAttributes,new TypeReference<Map<String,String>>() {});
                return new Item(rs.getLong("order_item_id"),rs.getLong("product_id"),
                    sku==null?rs.getString("current_sku"):sku,name==null?rs.getString("current_name"):name,attributes==null?Map.of():Map.copyOf(attributes),
                    rs.getString("presentation_name_snapshot"),rs.getString("unit_snapshot"),quantity,
                    rs.getBigDecimal("base_factor_snapshot"),rs.getBigDecimal("base_quantity"),price,
                    discountType,discount,gross,deduction,gross.subtract(deduction));
            },order.getId());
        if(items.isEmpty()) throw new IllegalArgumentException("Documento demonstrativo exige itens da venda.");
        BigDecimal gross=items.stream().map(Item::grossAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal discounts=items.stream().map(Item::discountAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal total=gross.subtract(discounts);
        if(order.getTotalAmount()==null || total.compareTo(order.getTotalAmount())!=0)
            throw new IllegalArgumentException("Total da venda inconsistente com os itens; o despacho não foi registrado.");
        return new Snapshot(1,"DEMONSTRATIVE",NOTICE,issuerSnapshot,customerSnapshot,
            order.getPaymentMethod()==null?null:order.getPaymentMethod().name(),order.getPaymentDueDate(),
            List.copyOf(items),gross,discounts,total,List.copyOf(warnings));
    }

    private boolean incompleteAddress(Address address) {
        return java.util.stream.Stream.of(address.street(),address.number(),address.city(),address.state(),address.zipCode())
            .anyMatch(value->value==null || value.isBlank());
    }
    private String text(Map<String,Object> row,String key) {
        Object value=row.get(key);
        return value==null || value.toString().isBlank() ? null : value.toString();
    }
}
