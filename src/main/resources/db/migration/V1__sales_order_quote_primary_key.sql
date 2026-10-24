-- Flyway executes this transactionally before Hibernate updates the entity mappings.
-- Existing rows/attachments and legacy IDs are preserved. Invalid/duplicate quotes stop migration.
DO $$
DECLARE
    old_primary_key text;
BEGIN
    -- New databases receive the same constraints without relying on Hibernate to add them.
    IF to_regclass('sales_orders') IS NULL THEN
        CREATE TABLE sales_orders (
            quote_number varchar(255) PRIMARY KEY,
            customer_name varchar(255),
            quotation_name varchar(255), quotation_data text,
            aluminium_colour varchar(255), aluminium_image_name varchar(255), aluminium_image_data text,
            pleated_mesh_available boolean NOT NULL,
            pleated_mesh_colour varchar(255), mesh_image_name varchar(255), mesh_image_data text,
            remarks text, sales_person varchar(255), punch_in_date varchar(255), created_at varchar(255),
            CONSTRAINT sales_orders_quote_canonical
                CHECK (quote_number <> '' AND quote_number = upper(btrim(quote_number)))
        );
        RETURN;
    END IF;
    LOCK TABLE sales_orders IN ACCESS EXCLUSIVE MODE;

    IF EXISTS (SELECT 1 FROM sales_orders WHERE quote_number IS NULL OR btrim(quote_number) = '') THEN
        RAISE EXCEPTION 'Cannot migrate sales orders: fix blank quote numbers before restarting';
    END IF;
    IF EXISTS (SELECT upper(btrim(quote_number)) FROM sales_orders
               GROUP BY upper(btrim(quote_number)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Cannot migrate sales orders: resolve duplicate quote numbers before restarting';
    END IF;

    SELECT conname INTO old_primary_key FROM pg_constraint
        WHERE conrelid = 'sales_orders'::regclass AND contype = 'p';
    IF old_primary_key IS NOT NULL THEN
        -- No CASCADE: unexpected foreign keys must be migrated explicitly, never silently dropped.
        EXECUTE format('ALTER TABLE sales_orders DROP CONSTRAINT %I', old_primary_key);
    END IF;

    UPDATE sales_orders SET quote_number = upper(btrim(quote_number));
    ALTER TABLE sales_orders ALTER COLUMN quote_number SET NOT NULL;
    ALTER TABLE sales_orders ADD CONSTRAINT sales_orders_pkey PRIMARY KEY (quote_number);
    ALTER TABLE sales_orders ADD CONSTRAINT sales_orders_quote_canonical
        CHECK (quote_number <> '' AND quote_number = upper(btrim(quote_number)));

    -- Retain the old ID values for reference; new orders no longer generate an ID.
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'sales_orders' AND column_name = 'id') THEN
        ALTER TABLE sales_orders ALTER COLUMN id DROP NOT NULL;
    END IF;
END $$;
