package com.pi.erp.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/company/appearance")
public class AppearanceController {

    @Autowired
    private AppearanceService service;

    public record PatchAppearance(@NotNull String palette, @NotNull @Min(0) Long version) {}

    @GetMapping
    public Appearance find() {
        return service.find();
    }

    @PatchMapping
    public Appearance update(@RequestBody @Valid PatchAppearance data) {
        return service.palette(data.palette(), data.version());
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        var logo = service.logo();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(logo.contentType()))
            .cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .body(logo.bytes());
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Appearance logo(@RequestParam("file") MultipartFile file, @RequestParam long version) {
        return service.logo(file, version);
    }

    @DeleteMapping("/logo")
    public Appearance removeLogo(@RequestParam long version) {
        return service.removeLogo(version);
    }
}
