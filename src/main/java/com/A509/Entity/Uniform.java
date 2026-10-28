package com.A509.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "uniforms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Uniform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uniform_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String history;

    private String material;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = true)
    @JsonIgnoreProperties("children")
    private UniformCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "period_category_id")
    @JsonIgnoreProperties("children")
    private UniformCategory periodCategory;

    @Column(name = "period")
    private String period;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "uniform", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference
    @ToString.Exclude
    private Set<Image> images;

    @OneToMany(mappedBy = "uniform", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<Comment> comments;
}
