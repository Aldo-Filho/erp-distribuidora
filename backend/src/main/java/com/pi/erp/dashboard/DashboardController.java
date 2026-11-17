package com.pi.erp.dashboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/dashboard")
public class DashboardController {
    @Autowired private DashboardService service;
    @GetMapping public ResponseEntity<DashboardView> current(@ModelAttribute DashboardFilter filter) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.current(filter));
    }
    @GetMapping("/widgets/{id}/details") public ResponseEntity<DashboardView.Details> details(
        @PathVariable String id,@ModelAttribute DashboardFilter filter,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.details(id,filter,page,size));
    }
}
