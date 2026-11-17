package com.pi.erp.dashboard;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.dashboard.DashboardView.*;
import com.pi.erp.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;

    @Autowired private DashboardCatalog catalog;
    @Autowired private DashboardPeriodResolver periods;
    @Autowired private DashboardMetricRepository metrics;

    private void validate(DashboardFilter f) {
        CurrentUser.requireId();
        periods.resolve(f.from(),f.to(),Boolean.TRUE.equals(f.compare()));
        if (f.warehouseId()!=null) CurrentUser.require("stock.read");
        if (!f.metric().equals("QUANTITY")) { CurrentUser.require("orders.read"); CurrentUser.require("finance.read"); }
    }
    private Map<String,Object> effective(DashboardCatalog.Definition d,DashboardFilter f) {
        Map<String,Object> result=new LinkedHashMap<>();
        if (d.profile().equals("STOCK") || d.id().equals("purchases.suggestions")) {
            if (f.warehouseId()!=null) result.put("warehouseId",f.warehouseId());
        }
        if (d.profile().equals("STOCK") || d.kind().equals("RANKING")) {
            if (f.unit()!=null && !(d.id().equals("sales.products") && f.metric().equals("REVENUE"))) result.put("unit",f.unit());
        }
        if (d.kind().equals("RANKING")) {
            result.put("rankingMetric",d.id().equals("sales.prices")?"AVERAGE_PRICE":f.metric());
            if(!d.id().equals("sales.products") || !f.metric().equals("REVENUE")) result.put("unit",f.unit()==null?"UN":f.unit());
        }
        return result;
    }
    private Period period(DashboardCatalog.Definition d,DashboardPeriodResolver.Selection p) {
        return d.dateBasis().equals("SNAPSHOT") ? new Period("Situação atual",periods.today(),d.id().equals("finance.agenda")?periods.today().plusDays(6):periods.today())
            : new Period("Período selecionado",p.from(),p.to());
    }
    public DashboardView current(DashboardFilter f) {
        validate(f);
        var selection=periods.resolve(f.from(),f.to(),Boolean.TRUE.equals(f.compare()));
        List<Widget> widgets=new ArrayList<>();
        Map<String,Object> coverage=new LinkedHashMap<>();
        if (CurrentUser.has("orders.read")) {
            try { coverage.put("undatedEligibleSales",metrics.missingConfirmations());
                  coverage.put("incompleteProductItems",metrics.incompleteProducts(f,selection.from(),selection.to())); }
            catch (DataAccessException e) { coverage.put("unavailable",true); }
        }
        for (var d:catalog.allowed()) {
            d.require();
            try {
                var q=metrics.query(d.id(),f,selection.from(),selection.to(),periods.today());
                boolean multiUnit=d.id().equals("purchases.suggestions") || d.id().equals("stock.composition") || d.id().equals("stock.movements") || d.kind().equals("RANKING");
                BigDecimal value=multiUnit?null:metrics.total(q);
                List<Map<String,Object>> rows=d.kind().equals("BAR") && !d.id().equals("stock.composition") ? metrics.distribution(q):metrics.rows(q,0,d.kind().equals("RANKING")?10:8);
                List<Map<String,Object>> series=d.kind().equals("LINE")?metrics.series(q,selection.granularity(),selection.from(),selection.to()):List.of();
                Comparison comparison=null;
                if (selection.compare() && !q.snapshot() && !multiUnit) {
                    var previousQuery=metrics.query(d.id(),f,selection.previousFrom(),selection.previousTo(),periods.today());
                    BigDecimal previous=metrics.total(previousQuery);
                    comparison=new Comparison(previous,previous.signum()==0?null:value.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous.abs(),2,java.math.RoundingMode.HALF_UP));
                    if (d.kind().equals("LINE")) {
                        // Align by day offset, not calendar bucket boundaries of the previous interval.
                        var shifted=new DashboardMetricRepository.Query("SELECT id,label,day + "+java.time.temporal.ChronoUnit.DAYS.between(selection.previousFrom(),selection.from())+" AS day,value,unit FROM ("+previousQuery.sql()+") prev",previousQuery.params(),false,false);
                        var pastSeries=metrics.series(shifted,selection.granularity(),selection.from(),selection.to());
                        List<Map<String,Object>> aligned=new ArrayList<>();
                        for(int i=0;i<series.size();i++) { var point=new LinkedHashMap<>(series.get(i)); point.put("previous",pastSeries.get(i).get("value")); aligned.add(point); }
                        series=aligned;
                    }
                }
                widgets.add(new Widget(d.id(),d.profile(),d.title(),d.kind(),d.permissions().contains("finance.read")||!d.id().equals("sales.products")||f.metric().equals("QUANTITY")?d.permissions():List.of("orders.read","finance.read"),period(d,selection),d.dateBasis(),
                    d.id().equals("sales.products")&&!f.metric().equals("QUANTITY") ? (f.metric().equals("REVENUE")?"BRL":"BRL/BASE") : d.unit(),
                    metrics.count(q)==0?State.EMPTY:State.READY,value,series,rows,comparison,effective(d,f),"/dashboard/widgets/"+d.id()+"/details"));
            } catch (DataAccessException issue) {
                log.warn("Dashboard widget unavailable: {}",d.id());
                widgets.add(new Widget(d.id(),d.profile(),d.title(),d.kind(),d.permissions(),period(d,selection),d.dateBasis(),d.unit(),State.UNAVAILABLE,null,List.of(),List.of(),null,effective(d,f),null));
            }
        }
        DashboardView legacy=current(); // Preserve the existing public cards/profiles contract.
        return new DashboardView(LocalDateTime.now(DashboardPeriodResolver.ZONE),legacy.roleName(),
            catalog.allowed().stream().map(d->Profile.valueOf(d.profile())).distinct().toList(),legacy.cards(),1,selection,coverage,widgets,catalog.allowed());
    }
    public Details details(String id,DashboardFilter f,int page,int size) {
        var d=catalog.require(id); validate(f);
        if(page<0 || size<1 || size>50 || page>1000000) throw new IllegalArgumentException("Paginação inválida.");
        var selection=periods.resolve(f.from(),f.to(),Boolean.TRUE.equals(f.compare()));
        var q=metrics.detailQuery(id,f,selection.from(),selection.to(),periods.today());
        return new Details(id,period(d,selection),d.dateBasis(),effective(d,f),metrics.rows(q,page*size,size),page,size,metrics.count(q));
    }

    // Roles are editable. Their effective permission grants select fixed profiles,
    // rather than assuming role names, IDs, or giving a role an authorization bypass.
    public DashboardView current() {
        Long userId = CurrentUser.requireId();
        String roleName = users.findById(userId).map(u -> u.getRole().getName()).orElse("");
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate(), first = today.withDayOfMonth(1);
        Period snapshot = new Period("Situação atual", null, today);
        Period month = new Period("Mês atual · data de criação da venda", first, today);
        Period cashMonth = new Period("Mês atual · data do movimento", first, today);
        List<Card> cards = new ArrayList<>();
        if (CurrentUser.has("orders.read")) {
            for (String status : List.of("PENDING", "CONFIRMED", "INVOICED", "CANCELED")) {
                String title = switch(status) {
                    case "PENDING" -> "Vendas pendentes";
                    case "CONFIRMED" -> "Vendas confirmadas";
                    case "INVOICED" -> "Vendas despachadas";
                    default -> "Vendas canceladas";
                };
                add(cards,"orders."+status,title,Profile.SALES,snapshot,Unit.COUNT,List.of("orders.read"),
                    "/orders?status="+status,"select count(*) from orders where status=?",status);
            }
            String route = "/orders?status=INVOICED&from="+first+"&to="+today;
            add(cards,"sales.month.count","Vendas criadas no mês e despachadas",Profile.SALES,month,Unit.COUNT,
                List.of("orders.read"),route,"select count(*) from orders where status='INVOICED' and created_at>=? and created_at<?",
                first.atStartOfDay(),today.plusDays(1).atStartOfDay());
            add(cards,"sales.month.amount","Valor das vendas criadas no mês e despachadas",Profile.SALES,month,Unit.BRL,
                List.of("orders.read","finance.read"),route,"select coalesce(sum(total_amount),0) from orders where status='INVOICED' and created_at>=? and created_at<?",
                first.atStartOfDay(),today.plusDays(1).atStartOfDay());
        }
        if (CurrentUser.has("finance.read")) {
            add(cards,"receivables.open","Contas a receber abertas · valor nominal",Profile.FINANCE,snapshot,Unit.BRL,
                List.of("finance.read"),"/finance/receivables?status=OPEN",
                "select coalesce(sum(amount),0) from accounts_receivable where status='OPEN'");
            add(cards,"payables.open","Contas a pagar abertas · valor nominal",Profile.FINANCE,snapshot,Unit.BRL,
                List.of("finance.read"),"/finance/payables?status=OPEN",
                "select coalesce(sum(amount),0) from accounts_payable where status='OPEN'");
            add(cards,"cash.month","Caixa realizado no mês · entradas menos saídas",Profile.FINANCE,cashMonth,Unit.BRL,
                List.of("finance.read"),"/finance/cash-flow?kind=ACTUAL&from="+first+"&to="+today,
                "select coalesce(sum(amount),0) from financial_cash_flow where kind='ACTUAL' and occurred_on>=? and occurred_on<=?",first,today);
        }
        add(cards,"stock.low","Itens de estoque abaixo do mínimo disponível",Profile.STOCK,snapshot,Unit.COUNT,
            List.of("stock.read"),"/stock?lowStock=true",
            "select count(*) from stock_items where quantity-reserved_quantity<min_quantity");
        add(cards,"suggestions.runs","Gerações de sugestões de compra",Profile.PURCHASING,snapshot,Unit.COUNT,
            List.of("purchases.read"),"/purchase-suggestions","select count(*) from purchase_suggestions");
        List<Profile> profiles = cards.stream().map(Card::profile).distinct().toList();
        return new DashboardView(now,roleName,profiles,List.copyOf(cards));
    }

    private void add(List<Card> cards, String id, String title, Profile profile, Period period,
                     Unit unit, List<String> permissions, String route, String sql, Object... args) {
        // Gate before executing each aggregate, even for callers inside this service.
        if (!permissions.stream().allMatch(CurrentUser::has)) return;
        permissions.forEach(CurrentUser::require);
        try {
            BigDecimal value = jdbc.queryForObject(sql,BigDecimal.class,args);
            cards.add(new Card(id,title,profile,period,value,unit,permissions,route,
                value == null ? State.UNAVAILABLE : value.signum() == 0 ? State.EMPTY : State.READY));
        } catch (DataAccessException issue) {
            // Do not expose SQL, database errors, or values through the unavailable state.
            log.warn("Dashboard aggregate unavailable: {}",id);
            cards.add(new Card(id,title,profile,period,null,unit,permissions,route,State.UNAVAILABLE));
        }
    }
}
