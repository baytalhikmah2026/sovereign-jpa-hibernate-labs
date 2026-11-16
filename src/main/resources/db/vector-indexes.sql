-- Vector index for semantic product search (Lab 96). Runs after Hibernate has created the tables.
-- HNSW builds a graph of near neighbours, so "the 5 closest products" is found without reading every row.
-- vector_cosine_ops matches the cosine_distance function (operator <=>) used by the queries.
create index if not exists sov_product_embedding_hnsw on sov_product using hnsw (embedding vector_cosine_ops);
