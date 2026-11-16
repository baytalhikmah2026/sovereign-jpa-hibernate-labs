-- Release 3 of 3 (contract): only after every running copy uses dpo_email.
-- Switched on with the location classpath:db/contract (see application-contract.yaml).
drop trigger sov_sync_dpo_email on sov_processing_activity;
drop function sov_sync_dpo_email();
alter table sov_processing_activity drop column controller_contact;
alter table sov_processing_activity alter column dpo_email set not null;
