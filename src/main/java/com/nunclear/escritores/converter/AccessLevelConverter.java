package com.nunclear.escritores.converter;

import com.nunclear.escritores.enums.AccessLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AccessLevelConverter implements AttributeConverter<AccessLevel, String> {

    @Override
    public String convertToDatabaseColumn(AccessLevel attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AccessLevel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AccessLevel.fromValue(dbData);
    }
}
