package com.pi.erp.financial;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
public final class FinancialPages {
    private FinancialPages() {}
    public static PageRequest of(int page,int size) {
        if(page<0 || page>100000 || size<1 || size>100) throw new IllegalArgumentException("Paginação inválida.");
        return PageRequest.of(page,size,Sort.by("id").descending());
    }
}
