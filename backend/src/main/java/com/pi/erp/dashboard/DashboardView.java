package com.pi.erp.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardView(LocalDateTime generatedAt, String roleName,
                            List<Profile> profiles, List<Card> cards, int schemaVersion,
                            DashboardPeriodResolver.Selection selection, java.util.Map<String,Object> coverage,
                            List<Widget> widgets, List<DashboardCatalog.Definition> catalog) {
    public DashboardView(LocalDateTime generatedAt,String roleName,List<Profile> profiles,List<Card> cards) {
        this(generatedAt,roleName,profiles,cards,1,null,java.util.Map.of(),List.of(),List.of());
    }
    public record Widget(String id,String profile,String title,String kind,List<String> permissions,
                         Period period,String dateBasis,String unit,State state,BigDecimal metric,
                         List<java.util.Map<String,Object>> series,List<java.util.Map<String,Object>> rows,
                         Comparison comparison,java.util.Map<String,Object> filters,String drillDown) {}
    public record Comparison(BigDecimal previousValue,BigDecimal changePercent) {}
    public record Details(String id,Period period,String dateBasis,java.util.Map<String,Object> filters,
                          List<java.util.Map<String,Object>> content,int page,int size,long totalElements) {}
    public enum Profile { SALES, PURCHASING, STOCK, FINANCE }
    public enum Unit { COUNT, BRL }
    public enum State { READY, EMPTY, UNAVAILABLE }
    public record Period(String label, LocalDate from, LocalDate to) {}
    public record Card(String id, String title, Profile profile, Period period,
                       BigDecimal value, Unit unit, List<String> permissions,
                       String drillDownRoute, State state) {}
}
