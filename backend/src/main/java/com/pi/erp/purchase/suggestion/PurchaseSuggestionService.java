package com.pi.erp.purchase.suggestion;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.purchase.*;
import com.pi.erp.purchase.item.*;
import com.pi.erp.exception.ResourceNotFoundException;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PurchaseSuggestionService {
 @Autowired private JdbcTemplate jdbc;
 @Autowired private PurchaseService purchases;
 @Autowired private PurchaseRepository purchaseRepository;
 @Autowired private PurchaseItemService items;
 @Autowired private jakarta.persistence.EntityManager em;
 @Transactional
 public Map<String,Object> generate(UUID requestId) {
  CurrentUser.require("purchases.suggest"); CurrentUser.require("purchases.create"); CurrentUser.require("purchases.read");
  // Serializes generators across processes; normal commercial edits retain their document locks.
  jdbc.execute("SELECT pg_advisory_xact_lock(714209)");
  var existing=jdbc.queryForList("select purchase_suggestion_id from purchase_suggestions where request_id=?",requestId);
  if(!existing.isEmpty()) return get(((Number)existing.getFirst().get("purchase_suggestion_id")).longValue());
  jdbc.queryForList("select purchase_id from purchases where purchase_suggestion_id is not null and status in ('DRAFT','SENT','CONFIRMED','PARTIALLY_RECEIVED') order by purchase_id for update");
  LocalDateTime end=LocalDateTime.now(),start=end.minusDays(90);
  Long run=jdbc.queryForObject("insert into purchase_suggestions(request_id,generated_by,period_from,period_to,reference_period) values(?,?,?,?,?) returning purchase_suggestion_id",Long.class,requestId,CurrentUser.requireId(),start,end,"90 dias");
  var stocks=jdbc.queryForList("""
   select st.*, pc.unit, p.sku from stock_items st
   join products p using(product_id) join product_catalogs pc on pc.product_catalog_id=p.catalog_id
   join warehouses w using(warehouse_id)
   where p.active and pc.active  order by st.product_id,st.warehouse_id
   """);
  Map<String,Purchase> drafts=new HashMap<>();
  for(var stock:stocks) {
   Long product=id(stock,"product_id"),warehouse=id(stock,"warehouse_id");
   BigDecimal sold=jdbc.queryForObject("""
    select coalesce(sum(m.quantity),0) from stock_movements m join orders o using(order_id)
    where m.stock_item_id=? and m.reason='SALE' and m.type='OUTBOUND' and o.status='INVOICED'
     and m.occurred_at>=? and m.occurred_at<?
    """,BigDecimal.class,id(stock,"stock_item_id"),start,end);
   BigDecimal avg=sold.divide(new BigDecimal("90"),10,RoundingMode.HALF_UP);
   BigDecimal current=number(stock,"quantity"),reserved=number(stock,"reserved_quantity"),available=current.subtract(reserved),minimum=number(stock,"min_quantity");
   var options=jdbc.queryForList("""
    select op.*, ps.supplier_id,ps.preferred,coalesce(op.avg_delivery_days,s.avg_delivery_days,0) lead_days
    from product_supplier_options op join product_suppliers ps using(product_supplier_id)
    join suppliers s using(supplier_id)
    where ps.product_id=? and ps.active and s.active and op.active and op.conversion_confirmed
     and op.base_factor>0 and op.cost>=0
    order by ps.preferred desc,op.cost/op.base_factor,ps.supplier_id,op.product_supplier_option_id
    """,product);
   var option=options.isEmpty()?null:options.getFirst();
   int lead=option==null?0:Math.max(0,((Number)option.get("lead_days")).intValue());
   BigDecimal target=minimum.max(sold.multiply(BigDecimal.valueOf(lead+7L)).divide(new BigDecimal("90"),10,RoundingMode.HALF_UP));
   BigDecimal covered=jdbc.queryForObject("""
    select coalesce(sum(greatest(i.base_quantity-i.received_quantity,0)),0)
    from purchase_items i join purchases p using(purchase_id)
    where i.product_id=? and i.warehouse_id=? and p.purchase_suggestion_id is not null
     and p.status in ('DRAFT','SENT','CONFIRMED','PARTIALLY_RECEIVED')
    """,BigDecimal.class,product,warehouse);
   BigDecimal shortage=target.subtract(available).subtract(covered).max(BigDecimal.ZERO);
   BigDecimal quantity=BigDecimal.ZERO,base=BigDecimal.ZERO;
   Long draftId=null,itemId=null,supplier=option==null?null:id(option,"supplier_id"),optionId=option==null?null:id(option,"product_supplier_option_id");
   String result=shortage.signum()==0?"COVERED":option==null?"NO_SUPPLIER":"GENERATED";
   if(shortage.signum()>0 && option!=null) {
    BigDecimal factor=number(option,"base_factor");
    int scale=Boolean.TRUE.equals(option.get("base_presentation")) && !"UN".equals(stock.get("unit"))?1:0;
    quantity=shortage.divide(factor,scale,RoundingMode.CEILING);base=quantity.multiply(factor);
    String key=supplier+":"+warehouse;
    Purchase draft=drafts.get(key);
    if(draft==null) {
     draft=purchases.register(new RequestPurchaseDTO(supplier,CurrentUser.requireId(),null));
     draft.setPurchaseSuggestionId(run);purchaseRepository.saveAndFlush(draft);drafts.put(key,draft);
    }
    var item=items.register(new RequestPurchaseItemDTO(draft.getId(),product,warehouse,quantity,number(option,"cost"),optionId));
    draftId=draft.getId();itemId=item.getId();
   }
   jdbc.update("""
    insert into purchase_suggestion_items(purchase_suggestion_id,product_id,warehouse_id,supplier_id,
     period_sold_quantity,avg_daily_consumption,current_stock,min_stock,suggested_quantity,
     reserved_stock,available_stock,target_stock,covered_quantity,lead_days,selection_rule,
     supplier_option_id,unit_cost,base_factor,commercial_quantity,purchase_id,purchase_item_id,result,shortage_quantity)
    values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """,run,product,warehouse,supplier,sold,avg,current,minimum,base,reserved,available,target,covered,lead,
    option==null?"NO_ACTIVE_OPTION":Boolean.TRUE.equals(option.get("preferred"))?"PREFERRED":"LOWEST_BASE_COST",
    optionId,option==null?null:option.get("cost"),option==null?null:option.get("base_factor"),quantity,draftId,itemId,result,shortage);
  }
  em.flush();return get(run);
 }
 @Transactional(readOnly=true) public Map<String,Object> get(Long id) {
  CurrentUser.require("purchases.read");
  var rows=jdbc.queryForList("select * from purchase_suggestions where purchase_suggestion_id=?",id);
  if(rows.isEmpty())throw new ResourceNotFoundException("Geração não encontrada.");return rows.getFirst();
 }
 @Transactional(readOnly=true) public Page<Map<String,Object>> search(int page,int size) {
  CurrentUser.require("purchases.read");check(page,size);
  return new PageImpl<>(jdbc.queryForList("select * from purchase_suggestions order by purchase_suggestion_id desc limit ? offset ?",size,(long)page*size),PageRequest.of(page,size),jdbc.queryForObject("select count(*) from purchase_suggestions",Long.class));
 }
 @Transactional(readOnly=true) public Page<Map<String,Object>> details(Long id,int page,int size) {
  get(id);check(page,size);
  var rows=jdbc.queryForList("""
   select i.*,p.sku,pc.name product_name,w.name warehouse_name,s.legal_name supplier_name,
    d.status draft_status from purchase_suggestion_items i join products p using(product_id)
   join product_catalogs pc on pc.product_catalog_id=p.catalog_id
   left join warehouses w using(warehouse_id) left join suppliers s using(supplier_id)
   left join purchases d on d.purchase_id=i.purchase_id
   where i.purchase_suggestion_id=? order by i.purchase_suggestion_item_id limit ? offset ?
   """,id,size,(long)page*size);
  return new PageImpl<>(rows,PageRequest.of(page,size),jdbc.queryForObject("select count(*) from purchase_suggestion_items where purchase_suggestion_id=?",Long.class,id));
 }
 private void check(int page,int size){if(page<0||page>100000||size<1||size>100)throw new IllegalArgumentException("Paginação inválida.");}
 private Long id(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
 private BigDecimal number(Map<String,Object> row,String key){Object value=row.get(key);return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
}
