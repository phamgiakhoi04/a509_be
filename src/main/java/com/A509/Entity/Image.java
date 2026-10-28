package com.A509.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /**
     * Sensitive images are hidden from anonymous API responses.  The original
     * Cloudinary URL is still available to authenticated users (including
     * administrators) so the frontend can show a warning/blurred preview to
     * visitors without leaking the source URL.
     */
    @Column(name = "is_sensitive", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean sensitive = false;

    public enum DisplayMode { GRID, FULL }

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", nullable = false, length = 10)
    @Builder.Default
    private DisplayMode displayMode = DisplayMode.GRID;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uniform_id", nullable = false)
    @JsonBackReference
    private Uniform uniform;

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return sensitive && !authenticated ? null : imageUrl;
    }
}
