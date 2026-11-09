package com.baytalhikmah.sovereign.domain;

import java.util.List;

public record ProductSpecs(String material, Integer capacityMl, Integer weightGrams, List<String> features) {

    public ProductSpecs {
        features = features == null ? List.of() : List.copyOf(features);
    }

}
