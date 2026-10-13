-- The API and the frontend accept a 100-char title and a 500-char description,
-- but Hibernate `update` never widened the columns created at 25 / 255.
-- Widening a varchar only changes metadata in Postgres (no table rewrite) and can be re-run safely.
ALTER TABLE projects
    ALTER COLUMN title TYPE varchar(100),
    ALTER COLUMN description TYPE varchar(500);
