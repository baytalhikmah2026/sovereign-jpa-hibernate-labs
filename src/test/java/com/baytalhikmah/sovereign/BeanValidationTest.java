package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.TransactionSystemException;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.service.CustomerLifecycleService;

import jakarta.validation.ConstraintViolationException;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BeanValidationTest {

    @Autowired
    CustomerLifecycleService service;

    @Test
    void validCustomersPassTheGate() {
        Customer human = service.register(new Customer("Bilal Ahmed", "bilal@example.com", "mobile"));
        Customer agent = service.register(new Customer("Shop Assistant", "assistant@example.com", "agent:shop-assistant"));

        assertThat(service.findById(human.getId()).getSignupChannel()).isEqualTo("mobile");
        assertThat(service.findById(agent.getId()).getSignupChannel()).isEqualTo("agent:shop-assistant");
    }

    @Test
    void invalidCustomerIsStoppedAtTheGate() {
        Customer broken = new Customer("", "not-an-email", "fax");

        assertThatThrownBy(() -> service.register(broken))
            .isInstanceOf(TransactionSystemException.class)
            .rootCause()
            .isInstanceOf(ConstraintViolationException.class)
            .satisfies(error -> {
                ConstraintViolationException violations = (ConstraintViolationException) error;
                System.out.println("=== GATEKEEPER REPORT ===");
                violations.getConstraintViolations().forEach(v ->
                    System.out.println("Rejected " + v.getPropertyPath() + " = '" + v.getInvalidValue() + "': " + v.getMessage()));
                assertThat(violations.getConstraintViolations()).hasSize(1);
            });
    }

}
