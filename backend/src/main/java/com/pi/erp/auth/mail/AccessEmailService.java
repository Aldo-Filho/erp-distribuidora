package com.pi.erp.auth.mail;

import com.pi.erp.auth.password.AccessToken;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessEmailService {

    @Autowired
    private Validator validator;

    private record EmailValue(@Email String email) {}

    public String normalize(String email) {
        String normalized = AccessToken.email(email);
        if (!validator.validate(new EmailValue(normalized)).isEmpty()) {
            throw new IllegalArgumentException("Email is invalid.");
        }
        return normalized;
    }
}
