package com.pi.erp.stock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stockItem")
public class StockItemController {
    @Autowired
    private StockItemRepository repository;
    @Autowired
    private StockItemService service;


}
