package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.service.CustomerLifecycleService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EntityLifecycleTest {

    @Autowired
    CustomerLifecycleService service;

    @Test
    void customerTravelsThroughAllFourStates() {
        Customer amina = new Customer("Amina Yusuf", "amina@example.com");

        Customer saved = service.register(amina);
        assertThat(saved.getId()).isNotNull();

        assertThat(service.loadTwiceFromSameContext(saved.getId())).isTrue();

        Customer renamed = service.rename(saved, "Amina Rahman");
        assertThat(service.findById(saved.getId()).getFullName()).isEqualTo("Amina Rahman");
        assertThat(renamed).isNotSameAs(saved);

        service.remove(saved.getId());
        assertThat(service.findById(saved.getId())).isNull();
    }

}
