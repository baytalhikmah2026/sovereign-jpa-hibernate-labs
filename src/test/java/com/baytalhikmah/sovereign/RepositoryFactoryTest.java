package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.persistence.CustomerRepository;
import com.baytalhikmah.sovereign.persistence.RecordNotFoundException;
import com.baytalhikmah.sovereign.persistence.RepositoryCallLog;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RepositoryFactoryTest {

    @Autowired
    CustomerRepository customers;

    @Autowired
    RepositoryCallLog callLog;

    @Test
    void everyRepositoryIsBuiltByTheShopsFactory() {
        int mark = callLog.size();

        Customer sara = customers.save(new Customer("Sara Ali", "sara@repository.test", "mobile"));
        String foundByEmail = customers.findByEmail("sara@repository.test").map(Customer::getFullName).orElse("-");
        String foundRequired = customers.findRequired(sara.getId()).getFullName();
        UUID unknownId = UUID.fromString("00000000-0000-7000-8000-000000000079");
        Throwable missing = catchThrowable(() -> customers.findRequired(unknownId));
        List<String> calls = callLog.namesSince(mark);

        System.out.println("=== REPOSITORY FACTORY ===");
        System.out.println("implementation   : " + AopProxyUtils.ultimateTargetClass(customers).getSimpleName());
        System.out.println("findByEmail      : " + foundByEmail);
        System.out.println("findRequired     : " + foundRequired);
        System.out.println("unknown customer : " + missing.getClass().getSimpleName() + ": " + missing.getMessage());
        System.out.println("calls recorded   : " + calls);

        assertThat(AopProxyUtils.ultimateTargetClass(customers).getSimpleName()).isEqualTo("SovereignRepositoryImpl");
        assertThat(foundByEmail).isEqualTo("Sara Ali");
        assertThat(foundRequired).isEqualTo("Sara Ali");
        assertThat(missing).isInstanceOf(RecordNotFoundException.class)
            .hasMessage("Customer 00000000-0000-7000-8000-000000000079 not found");
        assertThat(calls).containsExactly("CustomerRepository.save", "CustomerRepository.findByEmail",
            "CustomerRepository.findRequired", "CustomerRepository.findRequired");
    }

}
