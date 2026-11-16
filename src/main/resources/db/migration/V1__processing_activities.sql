-- GDPR Article 30: the record of processing activities. Owned by Flyway, never created by Hibernate.
create table sov_processing_activity (
    code               varchar(40)  primary key,
    purpose            varchar(200) not null,
    legal_basis        varchar(30)  not null
        check (legal_basis in ('CONTRACT', 'LEGAL_OBLIGATION', 'CONSENT', 'LEGITIMATE_INTEREST')),
    data_categories    varchar(300) not null,
    retention_days     integer      not null check (retention_days > 0),
    controller_contact varchar(120) not null
);
