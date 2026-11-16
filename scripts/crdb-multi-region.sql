-- Lab 98: make sovereign_global a multi-region database and pin each stock row to the region of its country.
-- Copy it into crdb1 (docker compose cp), then: cockroach sql --insecure --host=crdb1:26257 -d sovereign_global -f /cockroach/crdb-multi-region.sql
alter database sovereign_global primary region "eu-west";
alter database sovereign_global add region "us-east";
alter database sovereign_global add region "ap-south";

alter table sov_regional_stock add column crdb_region crdb_internal_region as (
    case
        when country_code in ('US', 'CA', 'MX', 'BR') then 'us-east'
        when country_code in ('IN', 'SG', 'AE', 'JP') then 'ap-south'
        else 'eu-west'
    end) stored;

alter table sov_regional_stock set locality regional by row as crdb_region;

insert into sov_regional_stock (id, sku, region, country_code, quantity)
values (gen_random_uuid(), 'Q98-LANTERN', 'eu-west', 'DE', 40);

insert into sov_regional_stock (id, sku, region, country_code, quantity)
values (gen_random_uuid(), 'Q98-LANTERN', 'us-east', 'US', 25);

insert into sov_regional_stock (id, sku, region, country_code, quantity)
values (gen_random_uuid(), 'Q98-LANTERN', 'ap-south', 'IN', 30);
