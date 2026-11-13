-- The customer tables of one shop. Run with the shop's schema (or database) as the current one.
create table if not exists sov_customer (
    id uuid not null primary key,
    full_name varchar(120) not null,
    email varchar(255) not null,
    signup_channel varchar(255) not null,
    billing_street varchar(120),
    billing_city varchar(80),
    billing_postal_code varchar(20),
    billing_country_code varchar(255),
    shipping_street varchar(120),
    shipping_city varchar(80),
    shipping_postal_code varchar(20),
    shipping_country_code varchar(255)
);

create table if not exists sov_customer_loyalty (
    customer_id uuid not null primary key references sov_customer (id),
    loyalty_tier varchar(10),
    loyalty_points integer check (loyalty_points >= 0)
);
