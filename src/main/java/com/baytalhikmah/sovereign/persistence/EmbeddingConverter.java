package com.baytalhikmah.sovereign.persistence;

import java.util.StringJoiner;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmbeddingConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] embedding) {
        if (embedding == null) {
            return null;
        }
        StringJoiner text = new StringJoiner(",", "[", "]");
        for (float value : embedding) {
            text.add(Float.toString(value));
        }
        return text.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.substring(1, text.length() - 1).split(",");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Float.parseFloat(parts[i].trim());
        }
        return embedding;
    }

}
