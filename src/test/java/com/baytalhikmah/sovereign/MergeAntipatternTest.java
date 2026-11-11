package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.domain.Product;
import com.baytalhikmah.sovereign.service.CatalogueImport;
import com.baytalhikmah.sovereign.service.ProductEditor;
import com.baytalhikmah.sovereign.service.ProductEditor.SaveReport;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MergeAntipatternTest {

    static final int EDITS = 20;

    @Autowired
    CatalogueImport catalogueImport;

    @Autowired
    ProductEditor productEditor;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void fetchAndModifyNeedsOneSelectInsteadOfOnePerProduct() {
        catalogueImport.importStateless("Q57-M-", EDITS);
        catalogueImport.importStateless("Q57-F-", EDITS);

        List<Product> screenA = productEditor.loadForEditing("Q57-M-");
        screenA.forEach(product -> product.rename("Edited " + product.getName()));
        SaveReport merged = productEditor.saveWithMerge(screenA);

        List<Product> screenB = productEditor.loadForEditing("Q57-F-");
        Map<UUID, String> newNames = new LinkedHashMap<>();
        screenB.forEach(product -> newNames.put(product.getId(), "Edited " + product.getName()));
        SaveReport fetched = productEditor.saveWithFetchAndModify(newNames);

        Integer edited = jdbc.queryForObject(
            "select count(*) from sov_product where sku like 'Q57-%' and name like 'Edited %'", Integer.class);

        System.out.println("=== SAVING " + EDITS + " EDITED PRODUCTS ===");
        print("merge()          ", merged);
        print("fetch-and-modify ", fetched);
        System.out.println("Renamed in the database : " + edited);

        assertThat(merged.products()).isEqualTo(EDITS);
        assertThat(merged.statements()).isGreaterThanOrEqualTo(EDITS);
        assertThat(fetched).isEqualTo(new SaveReport(EDITS, 1, 1));
        assertThat(edited).isEqualTo(2 * EDITS);
    }

    private static void print(String label, SaveReport report) {
        System.out.println(label + " : " + report.products() + " products, " + report.statements()
            + " single statements, " + report.batches() + " batches");
    }

}
