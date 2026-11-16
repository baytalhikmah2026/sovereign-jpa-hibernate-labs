-- Indexes for the Envers history tables (Lab 87). Runs after Hibernate has created the tables.
-- Every history table points to its revision through the column rev; without an index on it,
-- "everything that changed in revision N" reads the whole table.
create index if not exists sov_product_aud_by_rev on sov_product_aud (rev);
create index if not exists sov_product_image_aud_by_rev on sov_product_image_aud (rev);
