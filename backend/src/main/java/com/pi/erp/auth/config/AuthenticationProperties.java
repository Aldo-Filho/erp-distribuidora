package com.pi.erp.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "erp.auth")
public class AuthenticationProperties {

    private String publicBaseUrl;
    private String mailFrom;
    private boolean bootstrapEnabled;
    private Long bootstrapRoleId;
    private int loginPerIpPerMinute = 60;
    private int recoveryPerIpPerHour = 30;
    private int argonConcurrency = 2;
    private int maxBodyBytes = 16384;
}
