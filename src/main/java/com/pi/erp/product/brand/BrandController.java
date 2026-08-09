package com.pi.erp.product.brand;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/brand")
public class BrandController {
    @Autowired
    private BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity getAllBrands() {
        var allBrands = brandRepository.findAll();
        return ResponseEntity.ok(allBrands);
    }

    @PostMapping
    public ResponseEntity registerBrand(@RequestBody @Valid RequestBrandDTO data) {
        Brand newBrand = new Brand(data);
        brandRepository.save(newBrand);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity deleteBrand(@RequestBody @Valid RequestBrandDTO data) {
        Brand brand = brandRepository.findById(data.id()).get();
        brandRepository.delete(brand);
        return ResponseEntity.ok().build();
    }

}
