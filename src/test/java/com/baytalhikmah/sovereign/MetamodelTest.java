package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Money_;
import com.baytalhikmah.sovereign.domain.Product_;
import com.baytalhikmah.sovereign.query.ProductCard;
import com.baytalhikmah.sovereign.query.ProductSearch;
import com.baytalhikmah.sovereign.query.ProductSearchFilter;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MetamodelTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    ProductSearch productSearch;

    @Test
    void theGeneratedMetamodelDescribesTheEntitiesWithRealTypes() {
        System.out.println("=== GENERATED METAMODEL ===");
        System.out.println("Product_.sku    -> name '" + Product_.sku.getName() + "', type " + Product_.sku.getJavaType().getSimpleName());
        System.out.println("Product_.price  -> name '" + Product_.price.getName() + "', type " + Product_.price.getJavaType().getSimpleName());
        System.out.println("Money_.amount   -> name '" + Money_.amount.getName() + "', type " + Money_.amount.getJavaType().getSimpleName());
        System.out.println("Product_.SKU    -> constant \"" + Product_.SKU + "\"");

        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, "q28"));
        List<ProductCard> cheap = productSearch.search(
            new ProductSearchFilter(QueryLabData.codePrefix("q28"), null, null, new BigDecimal("15.00")));
        System.out.println("Products up to 15.00 : " + cheap.stream().map(ProductCard::name).toList());

        assertThat(Product_.sku.getJavaType()).isEqualTo(String.class);
        assertThat(Product_.price.getJavaType()).isEqualTo(Money.class);
        assertThat(Money_.amount.getJavaType()).isEqualTo(BigDecimal.class);
        assertThat(cheap).extracting(ProductCard::name).containsExactly("Loose Leaf Tea", "Stoneware Mug");
    }

}
