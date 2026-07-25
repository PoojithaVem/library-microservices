-- Creates one database per microservice (database-per-service pattern)
-- inside a single Postgres container - lighter weight than 3 separate
-- containers for a demo, while still keeping each service's schema isolated
-- (no cross-service foreign keys or joins - loan-service references
-- book_id/member_id as plain values, resolved via Feign at runtime, not SQL joins).

CREATE DATABASE book_db;
CREATE DATABASE member_db;
CREATE DATABASE loan_db;

GRANT ALL PRIVILEGES ON DATABASE book_db TO library_user;
GRANT ALL PRIVILEGES ON DATABASE member_db TO library_user;
GRANT ALL PRIVILEGES ON DATABASE loan_db TO library_user;
