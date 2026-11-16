package com.baytalhikmah.sovereign.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.baytalhikmah.sovereign.domain.Customer;

/** Customers. Spring Data writes the query of each method from its name. */
public interface CustomerRepository extends SovereignRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    List<Customer> findBySignupChannelOrderByFullName(String signupChannel);

}
