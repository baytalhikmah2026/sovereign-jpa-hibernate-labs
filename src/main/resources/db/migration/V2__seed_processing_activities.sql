insert into sov_processing_activity (code, purpose, legal_basis, data_categories, retention_days, controller_contact)
values ('ORDER_FULFILMENT', 'Deliver and invoice orders', 'CONTRACT',
        'name, e-mail, addresses, order lines', 3650, 'privacy@sovereign-shop.test');

insert into sov_processing_activity (code, purpose, legal_basis, data_categories, retention_days, controller_contact)
values ('MARKETING_EMAIL', 'Send newsletters to subscribers', 'CONSENT',
        'name, e-mail', 730, 'privacy@sovereign-shop.test');

insert into sov_processing_activity (code, purpose, legal_basis, data_categories, retention_days, controller_contact)
values ('FRAUD_SCREENING', 'Detect payment fraud', 'LEGITIMATE_INTEREST',
        'payment method, client IP, order history', 1825, 'security@sovereign-shop.test');
