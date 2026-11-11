package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baytalhikmah.sovereign.service.StockLedger;
import com.baytalhikmah.sovereign.service.StockLedger.IdReport;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.id.optimizer.pooled.preferred=pooled")
class SequenceOptimizerTest {

    @Autowired
    StockLedger stockLedger;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void oneSequenceCallServesFiftyRows() {
        IdReport first = stockLedger.receiveDelivery("Q54-", 500);

        Long otherWriter = jdbc.queryForObject("select nextval('sov_stock_movement_seq')", Long.class);
        jdbc.update("insert into sov_stock_movement (id, sku, quantity_change, reason) values (?, 'Q54-X', -1, 'stock check')",
            otherWriter);

        IdReport second = stockLedger.receiveDelivery("Q54-", 100);

        Long sequenceNow = jdbc.queryForObject("select last_value from sov_stock_movement_seq", Long.class);
        Integer rows = jdbc.queryForObject("select count(*) from sov_stock_movement", Integer.class);
        Integer distinctIds = jdbc.queryForObject("select count(distinct id) from sov_stock_movement", Integer.class);

        System.out.println("=== SEQUENCE OPTIMIZER (pooled, allocation 50) ===");
        print("First delivery ", first);
        System.out.println("Other writer    : nextval gave " + otherWriter);
        print("Second delivery", second);
        System.out.println("Sequence now    : " + sequenceNow + ", rows " + rows + ", distinct IDs " + distinctIds);

        assertThat(first).isEqualTo(new IdReport(500, 11, 10, 1, 500));
        assertThat(otherWriter).isEqualTo(551);
        assertThat(second).isEqualTo(new IdReport(100, 2, 2, 501, 650));
        assertThat(sequenceNow).isEqualTo(651);
        assertThat(rows).isEqualTo(601);
        assertThat(distinctIds).isEqualTo(601);
    }

    private static void print(String label, IdReport report) {
        System.out.println(label + " : " + report.rows() + " rows, IDs " + report.firstId() + " to " + report.lastId()
            + ", " + report.sequenceCalls() + " sequence calls, " + report.insertBatches() + " insert batches");
    }

}
