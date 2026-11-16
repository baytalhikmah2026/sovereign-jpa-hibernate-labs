package com.baytalhikmah.sovereign.persistence;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;

/**
 * Encrypts a String attribute on its way to the database and decrypts it on its way back. Hibernate asks Spring
 * for this converter, so it receives the FieldCipher with the configured key.
 */
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldCipher cipher;

    public EncryptedStringConverter(FieldCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : cipher.decrypt(dbData);
    }

}
