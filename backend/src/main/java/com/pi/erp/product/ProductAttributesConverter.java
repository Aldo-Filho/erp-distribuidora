package com.pi.erp.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Converter
public class ProductAttributesConverter
    implements AttributeConverter<Map<ProductCharacteristic, String>, String>
{

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<ProductCharacteristic, String> value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid attribute snapshot.", exception);
        }
    }

    @Override
    public Map<ProductCharacteristic, String> convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.readValue(
                value,
                new TypeReference<Map<ProductCharacteristic, String>>() {}
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid persisted attribute snapshot.", exception);
        }
    }
}
