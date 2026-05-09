package edu.arch.movie;

import jakarta.persistence.*;

@Entity
@Table(name = "movie_casts")
public class MovieCastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "person_name", nullable = false, length = 150)
    private String personName;

    @Column(name = "billing_order", nullable = false)
    private int billingOrder;

    public Long getId() {
        return id;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public int getBillingOrder() {
        return billingOrder;
    }

    public void setBillingOrder(int billingOrder) {
        this.billingOrder = billingOrder;
    }
}
