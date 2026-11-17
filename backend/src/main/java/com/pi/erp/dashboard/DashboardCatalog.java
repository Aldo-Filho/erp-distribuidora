package com.pi.erp.dashboard;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class DashboardCatalog {
    public record Definition(String id,String profile,String title,String kind,String dateBasis,String unit,
                             List<String> permissions,int minW,int minH) {
        public boolean allowed() { return permissions.stream().allMatch(CurrentUser::has); }
        public void require() { permissions.forEach(CurrentUser::require); }
    }
    private static Definition d(String id,String profile,String title,String kind,String basis,String unit,String... permissions) {
        return new Definition(id,profile,title,kind,basis,unit,List.of(permissions),kind.equals("STAT")?3:4,kind.equals("STAT")?4:7);
    }
    public final List<Definition> definitions = List.of(
        d("sales.revenue","FINANCE","Faturamento confirmado","LINE","CONFIRMED_AT","BRL","orders.read","finance.read"),
        d("cash.in","FINANCE","Entradas realizadas","STAT","OCCURRED_ON","BRL","finance.read"),
        d("cash.out","FINANCE","Saídas realizadas","STAT","OCCURRED_ON","BRL","finance.read"),
        d("cash.net","FINANCE","Movimento líquido","LINE","OCCURRED_ON","BRL","finance.read"),
        d("receivables.balance","FINANCE","Saldo a receber","STAT","SNAPSHOT","BRL","finance.read"),
        d("payables.balance","FINANCE","Saldo a pagar","STAT","SNAPSHOT","BRL","finance.read"),
        d("finance.overdue","FINANCE","Saldos vencidos","AGENDA","SNAPSHOT","BRL","finance.read"),
        d("finance.agenda","FINANCE","Agenda dos próximos 7 dias","AGENDA","SNAPSHOT","BRL","finance.read"),
        d("sales.count","SALES","Vendas confirmadas","LINE","CONFIRMED_AT","COUNT","orders.read"),
        d("sales.status","SALES","Vendas por situação","BAR","SNAPSHOT","COUNT","orders.read"),
        d("sales.dispatch","SALES","Aguardando despacho","STAT","SNAPSHOT","COUNT","orders.read"),
        d("sales.products","SALES","Produtos mais vendidos","RANKING","CONFIRMED_AT","BASE","orders.read"),
        d("sales.prices","FINANCE","Preço médio por produto","RANKING","CONFIRMED_AT","BRL/BASE","orders.read","finance.read"),
        d("purchases.status","PURCHASING","Compras por situação","BAR","SNAPSHOT","COUNT","purchases.read"),
        d("purchases.receiving","PURCHASING","Aguardando recebimento","STAT","SNAPSHOT","COUNT","purchases.read"),
        d("purchases.created","PURCHASING","Compras criadas","LINE","CREATED_AT","COUNT","purchases.read"),
        d("purchases.suggestions","PURCHASING","Gerações por fornecedor e depósito","BAR","SNAPSHOT","COUNT","purchases.read"),
        d("stock.low","STOCK","SKU/depósito abaixo do mínimo","STAT","SNAPSHOT","COUNT","stock.read"),
        d("stock.empty","STOCK","SKU/depósito sem disponível","STAT","SNAPSHOT","COUNT","stock.read"),
        d("stock.composition","STOCK","Físico, reservado e disponível","BAR","SNAPSHOT","BASE","stock.read"),
        d("stock.movements","STOCK","Movimentos por tipo e unidade","BAR","OCCURRED_ON","BASE","stock.read")
    );
    public List<Definition> allowed() { return definitions.stream().filter(Definition::allowed).toList(); }
    public Definition require(String id) {
        Definition d = definitions.stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(()->new ResourceNotFoundException("Bloco não encontrado."));
        d.require(); return d;
    }
}
