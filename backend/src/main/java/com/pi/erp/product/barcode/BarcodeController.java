package com.pi.erp.product.barcode;
import com.pi.erp.product.Product;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping("/product-barcodes")
public class BarcodeController {
 @Autowired private BarcodeRepository repository;
 @Autowired private BarcodeService service;
 @GetMapping public Page<Barcode> search(@RequestParam Long productId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
  if(page<0||page>100000||size<1||size>100) throw new IllegalArgumentException("Paginação inválida.");
  return repository.findAll((r,q,c)->c.equal(r.get("productId"),productId),PageRequest.of(page,size,Sort.by("id")));
 }
 @GetMapping("/lookup") public Product lookup(@RequestParam String code) { return service.resolve(code); }
 @PostMapping public ResponseEntity<Barcode> register(@RequestBody @Valid RequestBarcodeDTO data) { return ResponseEntity.status(201).body(service.register(data)); }
 @PatchMapping("/{id}") public Barcode update(@PathVariable Long id,@RequestBody PatchBarcodeDTO data) { return service.update(id,data); }
}
