-- =============================================================================
-- Bước 1 — Tạo database cho project movie-booking-eda
--
-- Thông tin PostgreSQL (theo cấu hình trong PSQL Workspace / pgAdmin):
--   Host:     localhost
--   Port:     5432
--   User:     postgres
--   Password: (nhập khi psql hỏi, hoặc dùng PGPASSWORD)
--
-- Trong tool: ô "Database" đang trống — bạn phải kết nối tới database HỆ THỐNG
-- tên  postgres  (mặc định luôn có), rồi chạy file này để tạo DB ứng dụng.
--
-- Ví dụ dòng lệnh (PowerShell / CMD), từ thư mục project:
--
--   psql -h localhost -p 5432 -U postgres -d postgres -f postgres-00-create-database.sql
--
-- Sau khi chạy xong, trong tool chọn Database = movie_booking_eda rồi chạy:
--   postgres-full-schema.sql
-- =============================================================================

CREATE DATABASE movie_booking_eda
    OWNER postgres
    ENCODING 'UTF8'
    TEMPLATE template0;

COMMENT ON DATABASE movie_booking_eda IS 'Movie booking EDA — schema trong postgres-full-schema.sql';
