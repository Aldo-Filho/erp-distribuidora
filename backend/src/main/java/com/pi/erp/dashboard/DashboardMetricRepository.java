package com.pi.erp.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

/** Closed SQL catalog. All request values are bound; no client identifier enters SQL. */
@Repository
public class DashboardMetricRepository {
    @Autowired private NamedParameterJdbcTemplate jdbc;
    public record Query(String sql, MapSqlParameterSource params, boolean snapshot, boolean ranking) {}
    private static final String ELIGIBLE = "o.status IN ('CONFIRMED','INVOICED')";
    private static final String NET = "ROUND(i.quantity*i.unit_price,2)-CASE i.discount_type WHEN 'PERCENTAGE' THEN ROUND(ROUND(i.quantity*i.unit_price,2)*i.discount/100,2) ELSE i.discount END";
    public Query query(String id, DashboardFilter f, LocalDate from, LocalDate to, LocalDate today) {
        var p = new MapSqlParameterSource().addValue("start",from.atStartOfDay()).addValue("end",to.plusDays(1).atStartOfDay())
            .addValue("from",from).addValue("to",to).addValue("today",today).addValue("agendaEnd",today.plusDays(6))
            .addValue("unit",f.unit()).addValue("warehouse",f.warehouseId()).addValue("product",f.productId());
        String sql;
        boolean snapshot = false, ranking = false;
        switch(id) {
            case "sales.revenue", "sales.count", "sales.dispatch", "sales.status" -> {
                snapshot = id.equals("sales.dispatch") || id.equals("sales.status");
                String value = id.equals("sales.revenue") ? "o.total_amount" : "1::numeric";
                String where = snapshot ? (id.equals("sales.dispatch") ? "o.status='CONFIRMED'" : "TRUE")
                    : ELIGIBLE+" AND o.confirmed_at>=:start AND o.confirmed_at<:end";
                sql = "SELECT o.order_id::text id, o.status label, o.confirmed_at::date AS day, "+value+" value, '"+(id.equals("sales.revenue")?"BRL":"COUNT")+"' unit FROM orders o WHERE "+where;
            }
            case "sales.products", "sales.prices" -> {
                ranking = true;
                String metric = id.equals("sales.prices") ? "AVERAGE_PRICE" : f.metric();
                String conversion = "i.unit_snapshot IS NOT NULL AND i.base_factor_snapshot>0 AND i.base_quantity>0";
                String reliable = metric.equals("REVENUE") ? "TRUE" : conversion;
                // Unknown conversion stays visible as incomplete coverage, never enters a weighted price.
                String value = metric.equals("REVENUE") ? "sum(net)" : metric.equals("AVERAGE_PRICE") ? "sum(net)/NULLIF(sum(qty),0)" : "sum(qty)";
                String unit = metric.equals("REVENUE") ? "'BRL'" : metric.equals("AVERAGE_PRICE") ? "'BRL/'||base_unit" : "base_unit";
                String unitFilter = metric.equals("REVENUE") ? "" : " AND i.unit_snapshot=:rankUnit";
                p.addValue("rankUnit",f.unit()==null?"UN":f.unit());
                sql = "WITH items AS (SELECT i.order_item_id item_id, i.order_id, o.confirmed_at::date AS day, i.product_id, p.sku, coalesce(i.product_name_snapshot,c.name) name, CASE WHEN "+conversion+" THEN i.unit_snapshot ELSE 'DADOS_INCOMPLETOS' END base_unit, CASE WHEN "+conversion+" THEN i.base_quantity END qty, ("+NET+") net FROM order_items i JOIN orders o USING(order_id) JOIN products p USING(product_id) JOIN product_catalogs c ON c.product_catalog_id=p.catalog_id WHERE "+ELIGIBLE+" AND o.confirmed_at>=:start AND o.confirmed_at<:end AND "+reliable+unitFilter+" AND (CAST(:product AS bigint) IS NULL OR i.product_id=:product)) SELECT product_id::text id, sku||' · '||max(name) label, NULL::date AS day, "+value+" value, "+unit+" unit, sum(qty) quantity, sum(net) revenue, sku, base_unit FROM items GROUP BY product_id,sku,base_unit";
                // Quantity-only callers must never receive monetary aggregates in rows.
                if (metric.equals("QUANTITY")) sql=sql.replace(", sum(net) revenue", "").replace("("+NET+") net", "0::numeric net");
            }
            case "cash.in", "cash.out", "cash.net" -> {
                String value=id.equals("cash.out")?"-amount":"amount";
                String sign=id.equals("cash.in")?" AND amount>0":id.equals("cash.out")?" AND amount<0":"";
                sql="SELECT id, direction label, occurred_on AS day, "+value+" value, 'BRL' unit FROM financial_cash_flow WHERE kind='ACTUAL' AND occurred_on BETWEEN :from AND :to"+sign;
            }
            case "receivables.balance", "payables.balance", "finance.overdue", "finance.agenda" -> {
                snapshot=true;
                String where=id.equals("receivables.balance")?" AND direction='RECEIVABLE'":id.equals("payables.balance")?" AND direction='PAYABLE'":id.equals("finance.overdue")?" AND occurred_on<:today":" AND occurred_on BETWEEN :today AND :agendaEnd";
                // Existing financial projection subtracts non-reversed settlements, including legacy partials.
                sql="SELECT id, direction||' · documento '||document_id label, occurred_on AS day, abs(amount) value, 'BRL' unit FROM financial_cash_flow WHERE kind='PROJECTED' AND abs(amount)>0"+where;
            }
            case "purchases.status", "purchases.receiving", "purchases.created" -> {
                snapshot=!id.equals("purchases.created");
                String where=id.equals("purchases.receiving")?"status IN ('CONFIRMED','PARTIALLY_RECEIVED')":snapshot?"TRUE":"created_at>=:start AND created_at<:end";
                sql="SELECT purchase_id::text id,status label,created_at::date AS day,1::numeric value,'COUNT' unit FROM purchases WHERE "+where;
            }
            case "purchases.suggestions" -> {
                snapshot=true;
                // DISTINCT generation/group avoids multiplying generations by their item count.
                sql="SELECT DISTINCT s.purchase_suggestion_id::text||':'||coalesce(i.supplier_id::text,'?')||':'||coalesce(i.warehouse_id::text,'?') id, 'Fornecedor '||coalesce(i.supplier_id::text,'indefinido')||' · depósito '||coalesce(i.warehouse_id::text,'indefinido') label, s.generated_at::date AS day,1::numeric value,'COUNT' unit FROM purchase_suggestions s LEFT JOIN purchase_suggestion_items i USING(purchase_suggestion_id) WHERE (CAST(:warehouse AS bigint) IS NULL OR i.warehouse_id=:warehouse)";
            }
            case "stock.low", "stock.empty", "stock.composition", "stock.movements" -> {
                snapshot=!id.equals("stock.movements");
                String joins=" FROM stock_items s JOIN products p USING(product_id) JOIN product_catalogs c ON c.product_catalog_id=p.catalog_id";
                String where=" WHERE (CAST(:warehouse AS bigint) IS NULL OR s.warehouse_id=:warehouse) AND (CAST(:unit AS varchar) IS NULL OR c.unit=:unit)";
                if (id.equals("stock.movements")) {
                    sql="SELECT m.stock_movement_id::text id,m.type label,m.occurred_at::date AS day,m.quantity value,c.unit unit"+joins+" JOIN stock_movements m USING(stock_item_id)"+where+" AND m.occurred_at>=:start AND m.occurred_at<:end";
                } else {
                    String value=id.equals("stock.composition")?"s.quantity-s.reserved_quantity":"1::numeric";
                    sql="SELECT s.stock_item_id::text id,p.sku||' · depósito '||s.warehouse_id label,NULL::date AS day,"+value+" value,"+(id.equals("stock.composition")?"c.unit":"'COUNT'")+" unit,s.quantity physical,s.reserved_quantity reserved,s.quantity-s.reserved_quantity available,s.min_quantity minimum,c.unit base_unit"+joins+where;
                    if (id.equals("stock.low")) sql+=" AND s.quantity-s.reserved_quantity<s.min_quantity";
                    if (id.equals("stock.empty")) sql+=" AND s.quantity-s.reserved_quantity<=0";
                }
            }
            default -> throw new IllegalArgumentException("Bloco inválido.");
        }
        return new Query(sql,p,snapshot,ranking);
    }
    public Query detailQuery(String id,DashboardFilter f,LocalDate from,LocalDate to,LocalDate today) {
        Query q=query(id,f,from,to,today);
        if(!q.ranking()) return q;
        int marker=q.sql().indexOf(") SELECT product_id::text");
        String cte=q.sql().substring(0,marker+1);
        boolean money=id.equals("sales.prices")||!f.metric().equals("QUANTITY");
        return new Query(cte+" SELECT item_id::text id,order_id,product_id,sku||' · '||name label,day,qty quantity,"+
            (money?"net value,'BRL' unit,":"qty value,base_unit unit,")+"base_unit FROM items",q.params(),false,false);
    }
    public BigDecimal total(Query q) { return jdbc.queryForObject("SELECT coalesce(sum(value),0) FROM ("+q.sql()+") source",q.params(),BigDecimal.class); }
    public long count(Query q) { return jdbc.queryForObject("SELECT count(*) FROM ("+q.sql()+") source",q.params(),Long.class); }
    public List<Map<String,Object>> rows(Query q,int offset,int limit) {
        q.params().addValue("limit",limit).addValue("offset",offset);
        return jdbc.queryForList("SELECT * FROM ("+q.sql()+") source ORDER BY "+(q.ranking()?"value DESC, sku, id, unit":"day NULLS LAST,label,id")+" LIMIT :limit OFFSET :offset",q.params());
    }
    public List<Map<String,Object>> distribution(Query q) {
        return jdbc.queryForList("SELECT label,unit,sum(value) value FROM ("+q.sql()+") source GROUP BY label,unit ORDER BY label,unit LIMIT 50",q.params());
    }
    public List<Map<String,Object>> series(Query q,String granularity,LocalDate from,LocalDate to) {
        String bucket=switch(granularity){case "WEEK"->"week";case "MONTH"->"month";default->"day";};
        // Bound calendar grid includes empty buckets and clips partial first/last intervals.
        q.params().addValue("seriesFrom",from).addValue("seriesTo",to);
        return jdbc.queryForList("WITH source AS ("+q.sql()+"), days AS (SELECT generate_series(CAST(:seriesFrom AS date),CAST(:seriesTo AS date),interval '1 day')::date AS day) SELECT min(days.day)::text label, coalesce(sum(source.value),0) value FROM days LEFT JOIN source USING(day) GROUP BY date_trunc('"+bucket+"',days.day) ORDER BY min(days.day)",q.params());
    }
    public long missingConfirmations() {
        return jdbc.queryForObject("SELECT count(*) FROM orders o WHERE "+ELIGIBLE+" AND confirmed_at IS NULL",new MapSqlParameterSource(),Long.class);
    }
    public long incompleteProducts(DashboardFilter f,LocalDate from,LocalDate to) {
        return jdbc.queryForObject("SELECT count(*) FROM order_items i JOIN orders o USING(order_id) WHERE "+ELIGIBLE+" AND o.confirmed_at>=:start AND o.confirmed_at<:end AND (i.unit_snapshot IS NULL OR i.base_factor_snapshot<=0 OR i.base_quantity<=0)",new MapSqlParameterSource().addValue("start",from.atStartOfDay()).addValue("end",to.plusDays(1).atStartOfDay()),Long.class);
    }
}
