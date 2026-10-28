package com.A509.Repository;

import com.A509.Entity.Uniform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniformRepository extends JpaRepository<Uniform, Long> {

    interface ReferenceName { Long getId(); String getName(); }
    @Query("select u.id as id, u.name as name from Uniform u")
    List<ReferenceName> findReferenceNames();

    @EntityGraph(attributePaths = {"country", "category", "periodCategory", "images"})
    @Query("select distinct u from Uniform u left join u.periodCategory p where u.country.id = :countryId "
            + "and u.category.id = :categoryId and (p.id = :periodId or (p is null and u.period = :slug)) order by u.id")
    List<Uniform> findShowcase(Long countryId, Long categoryId, Long periodId, String slug);

    @Query("select distinct u from Uniform u left join fetch u.images where u.id in :ids")
    List<Uniform> fetchImagesForIds(List<Long> ids);

    @EntityGraph(attributePaths = {"country", "images"})
    Optional<Uniform> findById(Long id);

    List<Uniform> findByNameContainingIgnoreCase(String name);

    List<Uniform> findByCountryId(Long countryId);

    Page<Uniform> findByCategoryId(Long categoryId, Pageable pageable);

    boolean existsByName(String name);
}
