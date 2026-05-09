-- =============================================================================
-- Full PostgreSQL schema for movie-booking-eda
-- Compatible with current services and extensible for production use.
--
-- Kết nối (PostgreSQL 18 / PSQL Workspace — như trong ảnh cấu hình):
--   Host:     localhost
--   Port:     5432
--   User:     postgres
--   Database: movie_booking_eda   ← tạo trước bằng postgres-00-create-database.sql
--
-- Chạy schema + seed (từ thư mục project):
--
--   psql -h localhost -p 5432 -U postgres -d movie_booking_eda -f postgres-full-schema.sql
--
-- Thứ tự:  (1) postgres-00-create-database.sql  với -d postgres
--          (2) postgres-full-schema.sql        với -d movie_booking_eda
-- =============================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
        CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'showtime_status') THEN
        CREATE TYPE showtime_status AS ENUM ('SCHEDULED', 'CANCELLED', 'COMPLETED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
        CREATE TYPE payment_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'seat_type') THEN
        CREATE TYPE seat_type AS ENUM ('STANDARD', 'VIP', 'COUPLE', 'WHEELCHAIR');
    END IF;
END$$;

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(150),
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Movies (metadata + poster path phục vụ UI; poster file đặt tại frontend/public/posters/)
CREATE TABLE IF NOT EXISTS movies (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    tagline TEXT,
    description TEXT,
    duration_minutes INTEGER CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    age_rating VARCHAR(20),
    genre VARCHAR(100),
    language VARCHAR(50) DEFAULT 'vi',
    director VARCHAR(200),
    producer VARCHAR(200),
    release_date DATE,
    poster_url TEXT,
    trailer_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'NOW_SHOWING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_movies_status ON movies(status);
CREATE INDEX IF NOT EXISTS idx_movies_release_date ON movies(release_date);

CREATE TABLE IF NOT EXISTS movie_casts (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    person_name VARCHAR(150) NOT NULL,
    billing_order SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (movie_id, person_name)
);

CREATE INDEX IF NOT EXISTS idx_movie_casts_movie_id ON movie_casts(movie_id);

CREATE TABLE IF NOT EXISTS movie_promotions (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    promo_price NUMERIC(12,2) NOT NULL CHECK (promo_price >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    marketing_tagline TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (valid_to >= valid_from)
);

CREATE INDEX IF NOT EXISTS idx_movie_promotions_movie_id ON movie_promotions(movie_id);

-- Cinema hierarchy
CREATE TABLE IF NOT EXISTS cinemas (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS screens (
    id BIGSERIAL PRIMARY KEY,
    cinema_id BIGINT NOT NULL REFERENCES cinemas(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    seat_capacity INTEGER NOT NULL CHECK (seat_capacity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (cinema_id, name)
);

CREATE INDEX IF NOT EXISTS idx_screens_cinema_id ON screens(cinema_id);

CREATE TABLE IF NOT EXISTS screen_seats (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    seat_row VARCHAR(5) NOT NULL,
    seat_number INTEGER NOT NULL CHECK (seat_number > 0),
    seat_code VARCHAR(20) GENERATED ALWAYS AS (seat_row || seat_number) STORED,
    seat_type seat_type NOT NULL DEFAULT 'STANDARD',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (screen_id, seat_row, seat_number),
    UNIQUE (screen_id, seat_code)
);

CREATE INDEX IF NOT EXISTS idx_screen_seats_screen_id ON screen_seats(screen_id);

-- Showtimes
CREATE TABLE IF NOT EXISTS showtimes (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE RESTRICT,
    screen_id BIGINT NOT NULL REFERENCES screens(id) ON DELETE RESTRICT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    base_price NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),
    status showtime_status NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (end_time > start_time),
    UNIQUE (screen_id, start_time)
);

CREATE INDEX IF NOT EXISTS idx_showtimes_movie_id ON showtimes(movie_id);
CREATE INDEX IF NOT EXISTS idx_showtimes_screen_id ON showtimes(screen_id);
CREATE INDEX IF NOT EXISTS idx_showtimes_start_time ON showtimes(start_time);

-- Booking header
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    movie_id BIGINT NOT NULL,
    showtime_id BIGINT REFERENCES showtimes(id) ON DELETE SET NULL,
    seat VARCHAR(200), -- supports multi-seat booking, e.g. A1,A2,A3
    ticket_code VARCHAR(64) NOT NULL UNIQUE,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_movie_id ON bookings(movie_id);
CREATE INDEX IF NOT EXISTS idx_bookings_showtime_id ON bookings(showtime_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookings_ticket_code ON bookings(ticket_code);

-- Booking seat details (supports multi-seat booking)
CREATE TABLE IF NOT EXISTS booking_items (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    showtime_id BIGINT NOT NULL REFERENCES showtimes(id) ON DELETE CASCADE,
    screen_seat_id BIGINT NOT NULL REFERENCES screen_seats(id) ON DELETE RESTRICT,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (showtime_id, screen_seat_id),
    UNIQUE (booking_id, screen_seat_id)
);

CREATE INDEX IF NOT EXISTS idx_booking_items_booking_id ON booking_items(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_items_showtime_id ON booking_items(showtime_id);

-- Payments
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL DEFAULT 'MOCK',
    provider_txn_id VARCHAR(120),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status payment_status NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_txn_id)
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- Event outbox for reliable event publishing
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_unpublished
    ON outbox_events (occurred_at)
    WHERE published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

-- Audit table for important state transitions
CREATE TABLE IF NOT EXISTS booking_status_history (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_booking_status_history_booking_id ON booking_status_history(booking_id);

-- ---------------------------------------------------------------------------
-- Dữ liệu phim Việt (poster: /posters/*.png — copy từ assets vào frontend/public/posters)
-- Idempotent: upsert theo slug, cast & promotion làm mới theo từng slug.
-- ---------------------------------------------------------------------------

INSERT INTO movies (
    slug, title, subtitle, tagline, description, age_rating, genre, language,
    director, producer, release_date, poster_url, status
) VALUES
(
    'bay-tien',
    'Bẫy Tiền',
    'Cú Lừa Tiền Tỷ',
    'ĐỔI ĐỜI HAY ĐỔI MẠNG?',
    'Bộ phim tội phạm giật gân xoay quanh những âm mưu tiền bạc và đánh đổi sinh mệnh.',
    'T16',
    'Crime',
    'vi',
    'Oscar Dương',
    'Hằng Trịnh',
    '2026-04-10',
    '/posters/bay-tien.png',
    'NOW_SHOWING'
),
(
    'hen-em-ngay-nhat-thuc',
    'Hẹn Em Ngày Nhật Thực',
    NULL,
    'Em là năm tháng tươi đẹp mà anh dành cả đời thương nhớ',
    'Câu chuyện tình lãng mạn, hoài niệm, nhiều nhân vật phụ quanh đôi chính.',
    'C13',
    'Romance',
    'vi',
    'Lê Thiện Viễn',
    'Lý Minh Thắng',
    '2026-04-03',
    '/posters/hen-em-ngay-nhat-thuc.png',
    'NOW_SHOWING'
),
(
    'phi-phong',
    'Phí Phông: Quỷ Máu Rừng Thiêng',
    NULL,
    'Truyền thuyết ghê rợn về loài quỷ hút máu nơi rừng sâu núi thẳm',
    'Phim kinh dị siêu nhiên: nghi lễ, rừng thiêng và những thực thể đáng sợ trong đêm trăng.',
    'C18',
    'Horror',
    'vi',
    'Đỗ Quốc Trung',
    NULL,
    '2024-04-24',
    '/posters/phi-phong.png',
    'NOW_SHOWING'
),
(
    'dai-tiec-trang-mau-8',
    'Đại Tiệc Trăng Máu 8',
    NULL,
    'TRẢI NGHIỆM LẠI SIÊU PHẨM',
    'Suất chiếu đặc biệt — tái hiện bữa tiệc đầy căng thẳng và bí mật giữa nhóm bạn thân.',
    'C16',
    'Drama',
    'vi',
    NULL,
    NULL,
    '2026-04-17',
    '/posters/dai-tiec-trang-mau-8.png',
    'NOW_SHOWING'
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    subtitle = EXCLUDED.subtitle,
    tagline = EXCLUDED.tagline,
    description = EXCLUDED.description,
    age_rating = EXCLUDED.age_rating,
    genre = EXCLUDED.genre,
    language = EXCLUDED.language,
    director = EXCLUDED.director,
    producer = EXCLUDED.producer,
    release_date = EXCLUDED.release_date,
    poster_url = EXCLUDED.poster_url,
    status = EXCLUDED.status,
    updated_at = NOW();

DELETE FROM movie_casts
WHERE movie_id IN (SELECT id FROM movies WHERE slug IN ('bay-tien', 'hen-em-ngay-nhat-thuc', 'phi-phong', 'dai-tiec-trang-mau-8'));

INSERT INTO movie_casts (movie_id, person_name, billing_order)
SELECT m.id, v.name, v.ord
FROM movies m
JOIN (
    VALUES
        ('bay-tien', 'Liên Bỉnh Phát', 1),
        ('bay-tien', 'Tam Triều Dâng', 2),
        ('bay-tien', 'Kiều Oanh', 3),
        ('bay-tien', 'Lê Hải', 4),
        ('bay-tien', 'Thừa Tuấn Anh', 5),
        ('bay-tien', 'Mai Cát Vi', 6),
        ('hen-em-ngay-nhat-thuc', 'Hứa Vĩ Văn', 1),
        ('hen-em-ngay-nhat-thuc', 'Đoàn Thiên Ân', 2),
        ('hen-em-ngay-nhat-thuc', 'Huỳnh Phương', 3),
        ('hen-em-ngay-nhat-thuc', 'Khương Lê', 4),
        ('hen-em-ngay-nhat-thuc', 'Nguyễn Thảo', 5),
        ('hen-em-ngay-nhat-thuc', 'NSND Lê Khanh', 6),
        ('hen-em-ngay-nhat-thuc', 'Thanh Sơn', 7),
        ('phi-phong', 'NSƯT Hạnh Thúy', 1),
        ('phi-phong', 'Kiều Minh Tuấn', 2),
        ('phi-phong', 'Nina Nutthacha Padovan', 3),
        ('phi-phong', 'Diệp Bảo Ngọc', 4),
        ('phi-phong', 'Doãn Minh Anh', 5)
) AS v(slug, name, ord) ON m.slug = v.slug;

DELETE FROM movie_promotions
WHERE movie_id = (SELECT id FROM movies WHERE slug = 'dai-tiec-trang-mau-8');

INSERT INTO movie_promotions (movie_id, label, promo_price, currency, valid_from, valid_to, marketing_tagline)
SELECT id,
       'Suất chiếu đặc biệt',
       69000.00,
       'VND',
       '2026-04-17',
       '2026-04-23',
       'CHỈ 69K — Từ 17–23.04.2026'
FROM movies WHERE slug = 'dai-tiec-trang-mau-8';

COMMIT;
