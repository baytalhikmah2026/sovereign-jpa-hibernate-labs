package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.audit.AuditChainVerifier;
import com.baytalhikmah.sovereign.audit.AuditChainVerifier.ChainReport;
import com.baytalhikmah.sovereign.audit.CurrentActor;
import com.baytalhikmah.sovereign.audit.MaskingMessageConverter;
import com.baytalhikmah.sovereign.audit.TamperEvidentLog;
import com.baytalhikmah.sovereign.audit.WriteRecord;
import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Money;
import com.baytalhikmah.sovereign.domain.Product;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TamperEvidentLogTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    TamperEvidentLog writeLog;

    @Test
    void everyCommittedWriteIsChainedAndAnEditIsDetected() throws Exception {
        UUID[] ids = CurrentActor.callAs("rania.ops", () -> transaction.execute(status -> {
            Customer rania = new Customer("Rania Aziz", "rania@q92.shop.test");
            Product safe = new Product("Q92-SAFE", "Document Safe", Money.of("55.00", "EUR"));
            entityManager.persist(rania);
            entityManager.persist(safe);
            return new UUID[] { rania.getId(), safe.getId() };
        }));
        CurrentActor.runAs("omar.pricing", () -> transaction.executeWithoutResult(status ->
            entityManager.find(Product.class, ids[1]).changePrice(Money.of("59.00", "EUR"))));
        UUID ghost = transaction.execute(status -> {
            Customer abandoned = new Customer("Ghost Buyer", "ghost@q92.shop.test");
            entityManager.persist(abandoned);
            status.setRollbackOnly();
            return abandoned.getId();
        });
        CurrentActor.runAs("rania.ops", () -> transaction.executeWithoutResult(status ->
            entityManager.remove(entityManager.find(Customer.class, ids[0]))));

        List<String> lines = Files.readAllLines(writeLog.segment());
        List<WriteRecord> records = lines.stream().map(line -> JSON.readValue(line, WriteRecord.class)).toList();
        List<WriteRecord> mine = records.stream()
            .filter(r -> r.entityId().equals(ids[0].toString()) || r.entityId().equals(ids[1].toString()))
            .toList();
        ChainReport report = AuditChainVerifier.verify(lines);

        int updateLine = records.indexOf(mine.get(2));
        List<String> edited = new ArrayList<>(lines);
        edited.set(updateLine, edited.get(updateLine).replace("\"omar.pricing\"", "\"someone.else\""));
        ChainReport afterEdit = AuditChainVerifier.verify(edited);

        String masked = MaskingMessageConverter.mask("Refund for rania@q92.shop.test to DE89370400440532013000");

        System.out.println("=== TAMPER-EVIDENT WRITE LOG ===");
        System.out.println("segment         : " + writeLog.segment().getFileName() + " (" + lines.size() + " lines)");
        mine.forEach(r -> System.out.println("#" + r.sequence() + " " + r.action() + " " + r.entity()
                + (r.changed().isEmpty() ? "" : " " + r.changed()) + " by " + r.actor()));
        System.out.println("rolled back     : " + (records.stream().anyMatch(r -> r.entityId().equals(ghost.toString()))
                ? "in the log" : "not in the log"));
        System.out.println("chain           : " + report.reason() + " (" + report.records() + " records)");
        System.out.println("after an edit   : broken at line " + afterEdit.brokenLine() + " (" + afterEdit.reason() + ")");
        System.out.println("masked log text : " + masked);

        assertThat(mine).extracting(r -> r.action() + " " + r.entity() + " " + r.actor()).containsExactly(
            "INSERT Customer rania.ops", "INSERT Product rania.ops", "UPDATE Product omar.pricing",
            "DELETE Customer rania.ops");
        assertThat(mine.get(2).changed()).containsExactly("price");
        assertThat(records).noneMatch(r -> r.entityId().equals(ghost.toString()));
        assertThat(report.intact()).isTrue();
        assertThat(afterEdit.brokenLine()).isEqualTo(updateLine + 1);
        assertThat(masked).isEqualTo("Refund for r***@q92.shop.test to DE89****3000");
    }

}
