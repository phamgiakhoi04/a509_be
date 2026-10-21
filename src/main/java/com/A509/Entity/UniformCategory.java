package com.A509.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "uniform_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniformCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "slug")
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Hierarchical category support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private UniformCategory parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference
    @Builder.Default
    private List<UniformCategory> children = new ArrayList<>();

    @Column(name = "country_id")
    private Long countryId;

    // Category type: REENACTMENT, UNIFORM, DOCUMENT
    @Enumerated(EnumType.STRING)
    @Column(name = "category_type")
    private CategoryType categoryType;

    // Sort order for display
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // Icon for display
    @Column(name = "icon")
    private String icon;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private Set<Uniform> uniforms;

    public enum CategoryType {
        REENACTMENT,  // Phục dựng
        UNIFORM,      // Quân trang
        DOCUMENT      // Tài liệu
    }
}
