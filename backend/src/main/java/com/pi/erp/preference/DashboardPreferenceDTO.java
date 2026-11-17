package com.pi.erp.preference;
import java.time.LocalDate;
import java.util.List;
public record DashboardPreferenceDTO(int schemaVersion,long revision,List<Widget> widgets,Filter filter) {
    public record Widget(String id,boolean visible,int x,int y,int w,int h) {}
    public record Filter(String preset,LocalDate referenceDate,LocalDate from,LocalDate to,boolean compare) {}
}
