-- Release 1 of 3 (expand): add the new column next to the old one and keep both filled.
-- Old application copies keep writing controller_contact, new ones write dpo_email; the trigger copies
-- whichever was written to the other column, so both versions can run side by side.
alter table sov_processing_activity add column dpo_email varchar(120);

update sov_processing_activity set dpo_email = controller_contact where dpo_email is null;

create or replace function sov_sync_dpo_email() returns trigger language plpgsql as $$
begin
    if tg_op = 'INSERT' then
        new.dpo_email := coalesce(new.dpo_email, new.controller_contact);
        new.controller_contact := coalesce(new.controller_contact, new.dpo_email);
    elsif new.dpo_email is distinct from old.dpo_email then
        new.controller_contact := new.dpo_email;
    elsif new.controller_contact is distinct from old.controller_contact then
        new.dpo_email := new.controller_contact;
    end if;
    return new;
end
$$;

create trigger sov_sync_dpo_email before insert or update on sov_processing_activity
    for each row execute function sov_sync_dpo_email();
