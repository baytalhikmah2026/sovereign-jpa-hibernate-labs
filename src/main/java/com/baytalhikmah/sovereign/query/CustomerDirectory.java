package com.baytalhikmah.sovereign.query;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.baytalhikmah.sovereign.domain.Customer;
import com.baytalhikmah.sovereign.domain.Customer_;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.SingularAttribute;

@Repository
public class CustomerDirectory {

    @PersistenceContext
    private EntityManager entityManager;

    /** Constructor expression: the shape is fixed when you write the code, checked by the record's constructor. */
    public List<CustomerContact> contacts(String emailDomain) {
        return entityManager.createQuery("""
                select new com.baytalhikmah.sovereign.query.CustomerContact(c.fullName, c.email, c.signupChannel)
                from Customer c
                where c.email like :domain
                order by c.fullName""", CustomerContact.class)
            .setParameter("domain", "%" + emailDomain)
            .getResultList();
    }

    /** Tuple: the caller chooses the columns at run time ("name", "email", "channel", "tier") and reads them by alias. */
    public List<Tuple> contactColumns(String emailDomain, List<String> columns) {
        Map<String, SingularAttribute<Customer, String>> available = Map.of(
            "name", Customer_.fullName,
            "email", Customer_.email,
            "channel", Customer_.signupChannel,
            "tier", Customer_.loyaltyTier);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Customer> customer = query.from(Customer.class);
        query.multiselect(columns.stream()
                .<Selection<?>>map(column -> customer.get(available.get(column)).alias(column))
                .toList())
            .where(cb.like(customer.get(Customer_.email), "%" + emailDomain))
            .orderBy(cb.asc(customer.get(Customer_.fullName)));
        return entityManager.createQuery(query).getResultList();
    }

}
