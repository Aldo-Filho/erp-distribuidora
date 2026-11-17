package com.pi.erp.company;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogoUploadConfiguration {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", LogoStorage.MAX_BYTES, 3 * 1024 * 1024, 0);
    }
}
