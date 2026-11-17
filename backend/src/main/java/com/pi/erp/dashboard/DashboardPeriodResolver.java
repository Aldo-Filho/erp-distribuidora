package com.pi.erp.dashboard;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
@Component
public class DashboardPeriodResolver {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    public LocalDate today() { return LocalDate.now(ZONE); }
    public record Selection(LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo,
                            String timezone, String granularity, boolean compare) {}
    public Selection resolve(LocalDate from, LocalDate to, boolean compare) {
        LocalDate today = today();
        if ((from == null) != (to == null)) throw new IllegalArgumentException("Informe início e fim juntos.");
        if (from == null) { from = today.withDayOfMonth(1); to = today; }
        long days = ChronoUnit.DAYS.between(from,to)+1;
        if (days < 1 || days > 366 || to.isAfter(today)) throw new IllegalArgumentException("Período inválido: até 366 dias, sem datas futuras.");
        return new Selection(from,to,compare ? from.minusDays(days) : null,compare ? from.minusDays(1) : null,
            ZONE.getId(),days <= 31 ? "DAY" : days <= 120 ? "WEEK" : "MONTH",compare);
    }
}
