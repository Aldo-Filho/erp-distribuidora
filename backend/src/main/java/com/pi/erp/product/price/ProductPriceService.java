package com.pi.erp.product.price;

import com.pi.erp.price.item.PriceTableItemRepository;
import com.pi.erp.product.Product;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.product.presentation.ProductPresentation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductPriceService {

    @Autowired
    private PriceTableItemRepository priceTableItemRepository;

    public BigDecimal resolve(
        Long customerId,
        Product product,
        ProductPresentation presentation,
        LocalDate date
    ) {
        Optional<BigDecimal> specific = priceTableItemRepository.findApplicablePresentationPrice(
            customerId,
            product.getId(),
            presentation.getId(),
            date
        );
        BigDecimal price;
        if (specific.isPresent()) {
            price = specific.get();
        } else {
            Optional<BigDecimal> base = priceTableItemRepository.findApplicablePrice(
                customerId,
                product.getId(),
                date
            );
            if (base.isPresent()) {
                price = base.get().multiply(presentation.getBaseFactor());
            } else if (presentation.getPrice() != null) {
                price = presentation.getPrice();
            } else {
                price = product.getPrice().multiply(presentation.getBaseFactor());
            }
        }
        price = price.setScale(2, RoundingMode.HALF_UP);
        ProductCatalogService.validateMoney(price);
        return price;
    }
}
