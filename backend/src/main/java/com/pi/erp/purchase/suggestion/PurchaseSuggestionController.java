package com.pi.erp.purchase.suggestion;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/purchase-suggestions")
public class PurchaseSuggestionController {
 @Autowired private PurchaseSuggestionService service;
 public record Generation(@NotNull UUID requestId) {}
 @PostMapping public Map<String,Object> generate(@RequestBody @Valid Generation body){return service.generate(body.requestId());}
 @GetMapping public Page<Map<String,Object>> search(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return service.search(page,size);}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable Long id){return service.get(id);}
 @GetMapping("/{id}/items") public Page<Map<String,Object>> items(@PathVariable Long id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){return service.details(id,page,size);}
}
