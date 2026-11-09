package com.baytalhikmah.sovereign.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProductCategoryLinkId(UUID productId, UUID categoryId) implements Serializable {
}
