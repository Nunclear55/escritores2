package com.nunclear.escritores.converter;

import com.nunclear.escritores.enums.AccountState;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AccountStateConverter implements AttributeConverter<AccountState, String> {

    @Override
    public String convertToDatabaseColumn(AccountState attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AccountState convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AccountState.fromValue(dbData);
    }
}
