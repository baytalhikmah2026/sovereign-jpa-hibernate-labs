package com.baytalhikmah.sovereign.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.hibernate.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baytalhikmah.sovereign.domain.Customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CustomerSeedService {

    public static final long SEED = 2026L;
    public static final String SEED_DOMAIN = "@seed.sovereign.test";

    private static final String[] FIRST_NAMES = {
        "Amina", "Bilal", "Zara", "Omar", "Layla", "Yusuf", "Maryam", "Ibrahim", "Sara", "Hamza" };
    private static final String[] LAST_NAMES = {
        "Yusuf", "Ahmed", "Malik", "Khan", "Rahman", "Hassan", "Ali", "Siddiqui", "Farooq", "Aziz" };
    private static final String[] CHANNELS = { "web", "mobile", "store", "agent:shop-assistant" };

    private static final int FLUSH_EVERY = 500;

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbc;

    public CustomerSeedService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static List<Customer> generate(int count, long seed) {
        Random random = new Random(seed);
        List<Customer> customers = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String channel = CHANNELS[random.nextInt(CHANNELS.length)];
            String email = (first + "." + last + "." + i).toLowerCase(Locale.ROOT) + SEED_DOMAIN;
            customers.add(new Customer(first + " " + last, email, channel));
        }
        return customers;
    }

    @Transactional
    public long insert(List<Customer> customers, int jdbcBatchSize) {
        entityManager.unwrap(Session.class).setJdbcBatchSize(jdbcBatchSize);
        long start = System.nanoTime();
        for (int i = 0; i < customers.size(); i++) {
            entityManager.persist(customers.get(i));
            if ((i + 1) % FLUSH_EVERY == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return (System.nanoTime() - start) / 1_000_000;
    }

    public Map<String, Object> verify() {
        return jdbc.queryForMap("""
            select count(*) as rows,
                   count(distinct email) as unique_emails,
                   md5(string_agg(full_name || '|' || email || '|' || signup_channel, ',' order by email)) as fingerprint
            from sov_customer
            where email like ?
            """, "%" + SEED_DOMAIN);
    }

    @Transactional
    public int deleteSeedCustomers() {
        return jdbc.update("delete from sov_customer where email like ?", "%" + SEED_DOMAIN);
    }

}
