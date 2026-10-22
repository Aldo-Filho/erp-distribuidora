package com.pi.erp.product;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SkuGenerator {

    public String generate(long number) {
        return String.format(Locale.ROOT, "PRD-%06d", number);
    }
}
