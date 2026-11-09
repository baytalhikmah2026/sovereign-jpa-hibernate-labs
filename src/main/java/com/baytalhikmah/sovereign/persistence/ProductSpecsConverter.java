package com.baytalhikmah.sovereign.persistence;

import com.baytalhikmah.sovereign.domain.ProductSpecs;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class ProductSpecsConverter implements AttributeConverter<ProductSpecs, String> {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(ProductSpecs specs) {
        return specs == null ? null : JSON.writeValueAsString(specs);
    }

    @Override
    public ProductSpecs convertToEntityAttribute(String json) {
        return json == null ? null : JSON.readValue(json, ProductSpecs.class);
    }

}
