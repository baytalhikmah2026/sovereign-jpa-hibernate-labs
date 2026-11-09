package com.baytalhikmah.sovereign.query;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class CustomerExport {

    @PersistenceContext
    private EntityManager entityManager;

    /** Entities, one at a time: a stream fed fetchSize rows per round trip, cleared every clearEvery rows. */
    @Transactional(readOnly = true)
    public ExportSummary streamCustomers(String emailDomain, int fetchSize, int clearEvery) {
        Session session = entityManager.unwrap(Session.class);
        Map<String, Long> perChannel = new TreeMap<>();
        long rows = 0;
        int mostTracked = 0;

        try (Stream<Customer> customers = entityManager.createQuery("""
                    select c from Customer c
                    where c.email like :domain
                    order by c.id""", Customer.class)
                .setParameter("domain", "%" + emailDomain)
                .setHint(HibernateHints.HINT_FETCH_SIZE, fetchSize)
                .setHint(HibernateHints.HINT_READ_ONLY, true)
                .getResultStream()) {
            Iterator<Customer> iterator = customers.iterator();
            while (iterator.hasNext()) {
                Customer customer = iterator.next();
                perChannel.merge(customer.getSignupChannel(), 1L, Long::sum);
                rows++;
                if (rows % clearEvery == 0) {
                    mostTracked = Math.max(mostTracked, session.getStatistics().getEntityCount());
                    session.clear();
                }
            }
        }
        mostTracked = Math.max(mostTracked, session.getStatistics().getEntityCount());
        return new ExportSummary(rows, perChannel, mostTracked);
    }

    /** Plain values, one at a time: a forward-only scroll over a projection; nothing is tracked at all. */
    @Transactional(readOnly = true)
    public ExportSummary scrollChannels(String emailDomain, int fetchSize) {
        Session session = entityManager.unwrap(Session.class);
        Map<String, Long> perChannel = new TreeMap<>();
        long rows = 0;

        try (ScrollableResults<String> channels = session.createSelectionQuery("""
                    select c.signupChannel from Customer c
                    where c.email like :domain""", String.class)
                .setParameter("domain", "%" + emailDomain)
                .setFetchSize(fetchSize)
                .scroll(ScrollMode.FORWARD_ONLY)) {
            while (channels.next()) {
                perChannel.merge(channels.get(), 1L, Long::sum);
                rows++;
            }
        }
        return new ExportSummary(rows, perChannel, session.getStatistics().getEntityCount());
    }

}
