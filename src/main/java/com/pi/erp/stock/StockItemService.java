package com.pi.erp.stock;

import com.pi.erp.product.ProductRepository;
import com.pi.erp.warehouse.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockItemService {
    @Autowired
    private StockItemRepository repository;

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private ProductRepository productRepository;


}
