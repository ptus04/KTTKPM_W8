-- One-time fix for old databases where bookings.status uses PostgreSQL enum booking_status.
-- Run this on the existing movie_booking_eda database before starting booking-service.
--
-- psql -h localhost -p 5432 -U postgres -d movie_booking_eda -f postgres-fix-legacy-booking-status.sql

BEGIN;

-- 1) bookings.status: drop enum default first, then convert to varchar, then set text default.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'bookings'
          AND column_name = 'status'
          AND udt_name = 'booking_status'
    ) THEN
        ALTER TABLE bookings ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE bookings
            ALTER COLUMN status TYPE VARCHAR(20)
            USING status::text;
        ALTER TABLE bookings ALTER COLUMN status SET DEFAULT 'PENDING';
    END IF;
END$$;

-- 1.1) Ensure seat column can store multiple seats (comma-separated)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'bookings'
          AND column_name = 'seat'
          AND data_type = 'character varying'
          AND character_maximum_length IS NOT NULL
          AND character_maximum_length < 200
    ) THEN
        ALTER TABLE bookings ALTER COLUMN seat TYPE VARCHAR(200);
    END IF;
END$$;

-- 1.2) Ensure ticket_code exists and is populated for old rows
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'bookings'
          AND column_name = 'ticket_code'
    ) THEN
        ALTER TABLE bookings ADD COLUMN ticket_code VARCHAR(64);
    END IF;

    UPDATE bookings
    SET ticket_code = 'MBE-' || UPPER(SUBSTRING(MD5(id::text || '-' || COALESCE(created_at::text, NOW()::text)) FOR 12))
    WHERE ticket_code IS NULL OR ticket_code = '';

    ALTER TABLE bookings ALTER COLUMN ticket_code SET NOT NULL;
END$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_bookings_ticket_code ON bookings(ticket_code);

-- 2) booking_status_history.old_status
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'booking_status_history'
          AND column_name = 'old_status'
          AND udt_name = 'booking_status'
    ) THEN
        ALTER TABLE booking_status_history
            ALTER COLUMN old_status TYPE VARCHAR(30)
            USING old_status::text;
    END IF;
END$$;

-- 3) booking_status_history.new_status
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'booking_status_history'
          AND column_name = 'new_status'
          AND udt_name = 'booking_status'
    ) THEN
        ALTER TABLE booking_status_history
            ALTER COLUMN new_status TYPE VARCHAR(30)
            USING new_status::text;
    END IF;
END$$;

-- 4) Optional cleanup: drop enum type only if truly unused (ignore error if still referenced).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status')
       AND NOT EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE udt_name = 'booking_status'
       ) THEN
        BEGIN
            DROP TYPE booking_status;
        EXCEPTION WHEN dependent_objects_still_exist THEN
            -- Keep type if any hidden dependency remains.
            NULL;
        END;
    END IF;
END$$;

COMMIT;
