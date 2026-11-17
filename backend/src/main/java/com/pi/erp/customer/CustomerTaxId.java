package com.pi.erp.customer;

import java.util.Locale;

/** Canonical document identity shared by writes and uniqueness queries. */
public final class CustomerTaxId {
    private CustomerTaxId() {}

    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
    }

    public static String requireValid(String value, PersonType personType) {
        String document = normalize(value);
        boolean cpf = personType == PersonType.PF;
        int length = cpf ? 11 : 14;
        String pattern = cpf ? "[0-9]{11}" : "[A-Z0-9]{12}[0-9]{2}";
        if (personType == null || !document.matches(pattern) ||
            document.chars().distinct().count() == 1 ||
            digit(document, length - 2, cpf) != document.charAt(length - 2) - '0' ||
            digit(document, length - 1, cpf) != document.charAt(length - 1) - '0') {
            throw new IllegalArgumentException(cpf ? "Informe um CPF válido." : "Informe um CNPJ válido.");
        }
        return document;
    }

    private static int digit(String value, int end, boolean cpf) {
        int sum = 0;
        for (int i = 0; i < end; i++) {
            int weight = cpf ? end + 1 - i : (end - 1 - i) % 8 + 2;
            sum += (value.charAt(i) - '0') * weight;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
