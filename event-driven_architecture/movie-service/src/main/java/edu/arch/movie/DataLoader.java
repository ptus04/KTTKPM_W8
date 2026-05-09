package edu.arch.movie;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final MovieCastRepository movieCastRepository;
    private final MoviePromotionRepository moviePromotionRepository;

    public DataLoader(
            MovieRepository movieRepository,
            MovieCastRepository movieCastRepository,
            MoviePromotionRepository moviePromotionRepository) {
        this.movieRepository = movieRepository;
        this.movieCastRepository = movieCastRepository;
        this.moviePromotionRepository = moviePromotionRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.existsBySlug("bay-tien")) {
            return;
        }
        movieCastRepository.deleteAll();
        moviePromotionRepository.deleteAll();
        movieRepository.deleteAll();

        MovieEntity bay = movie("bay-tien", "Bẫy Tiền", "Cú Lừa Tiền Tỷ", "ĐỔI ĐỜI HAY ĐỔI MẠNG?",
                "Bộ phim tội phạm giật gân xoay quanh những âm mưu tiền bạc và đánh đổi sinh mệnh.",
                "T16", "Crime", "Oscar Dương", "Hằng Trịnh", LocalDate.of(2026, 4, 10), "/posters/bay-tien.png");
        MovieEntity hen = movie("hen-em-ngay-nhat-thuc", "Hẹn Em Ngày Nhật Thực", null,
                "Em là năm tháng tươi đẹp mà anh dành cả đời thương nhớ",
                "Câu chuyện tình lãng mạn, hoài niệm, nhiều nhân vật phụ quanh đôi chính.",
                "C13", "Romance", "Lê Thiện Viễn", "Lý Minh Thắng", LocalDate.of(2026, 4, 3), "/posters/hen-em-ngay-nhat-thuc.png");
        MovieEntity phi = movie("phi-phong", "Phí Phông: Quỷ Máu Rừng Thiêng", null,
                "Truyền thuyết ghê rợn về loài quỷ hút máu nơi rừng sâu núi thẳm",
                "Phim kinh dị siêu nhiên: nghi lễ, rừng thiêng và những thực thể đáng sợ trong đêm trăng.",
                "C18", "Horror", "Đỗ Quốc Trung", null, LocalDate.of(2024, 4, 24), "/posters/phi-phong.png");
        MovieEntity tiec = movie("dai-tiec-trang-mau-8", "Đại Tiệc Trăng Máu 8", null, "TRẢI NGHIỆM LẠI SIÊU PHẨM",
                "Suất chiếu đặc biệt — tái hiện bữa tiệc đầy căng thẳng và bí mật giữa nhóm bạn thân.",
                "C16", "Drama", null, null, LocalDate.of(2026, 4, 17), "/posters/dai-tiec-trang-mau-8.png");

        movieRepository.saveAll(List.of(bay, hen, phi, tiec));

        saveCasts(bay.getId(), List.of(
                "Liên Bỉnh Phát", "Tam Triều Dâng", "Kiều Oanh", "Lê Hải", "Thừa Tuấn Anh", "Mai Cát Vi"));
        saveCasts(hen.getId(), List.of(
                "Hứa Vĩ Văn", "Đoàn Thiên Ân", "Huỳnh Phương", "Khương Lê", "Nguyễn Thảo", "NSND Lê Khanh", "Thanh Sơn"));
        saveCasts(phi.getId(), List.of(
                "NSƯT Hạnh Thúy", "Kiều Minh Tuấn", "Nina Nutthacha Padovan", "Diệp Bảo Ngọc", "Doãn Minh Anh"));

        MoviePromotionEntity promo = new MoviePromotionEntity();
        promo.setMovieId(tiec.getId());
        promo.setLabel("Suất chiếu đặc biệt");
        promo.setPromoPrice(new BigDecimal("69000"));
        promo.setCurrency("VND");
        promo.setValidFrom(LocalDate.of(2026, 4, 17));
        promo.setValidTo(LocalDate.of(2026, 4, 23));
        promo.setMarketingTagline("CHỈ 69K — Từ 17–23.04.2026");
        moviePromotionRepository.save(promo);
    }

    private static MovieEntity movie(
            String slug,
            String title,
            String subtitle,
            String tagline,
            String description,
            String ageRating,
            String genre,
            String director,
            String producer,
            LocalDate releaseDate,
            String posterUrl) {
        MovieEntity m = new MovieEntity();
        m.setSlug(slug);
        m.setTitle(title);
        m.setSubtitle(subtitle);
        m.setTagline(tagline);
        m.setDescription(description);
        m.setAgeRating(ageRating);
        m.setGenre(genre);
        m.setDirector(director);
        m.setProducer(producer);
        m.setReleaseDate(releaseDate);
        m.setPosterUrl(posterUrl);
        return m;
    }

    private void saveCasts(Long movieId, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            MovieCastEntity c = new MovieCastEntity();
            c.setMovieId(movieId);
            c.setPersonName(names.get(i));
            c.setBillingOrder(i + 1);
            movieCastRepository.save(c);
        }
    }
}
