package com.A509.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="page_views", indexes={@Index(name="idx_views_time",columnList="viewed_at"),@Index(name="idx_views_event",columnList="event_id",unique=true)})
@Getter @Setter @NoArgsConstructor
public class PageView {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="event_id",nullable=false,length=36) private String eventId;
    @Column(name="viewed_at",nullable=false) private LocalDateTime viewedAt;
    @Column(name="visitor_hash",nullable=false,length=64) private String visitorHash;
    @Column(name="session_hash",nullable=false,length=64) private String sessionHash;
    @Column(nullable=false,length=500) private String path;
    @Column(nullable=false,length=253) private String referrer;
    @Column(nullable=false,length=12) private String device;
    @Column(nullable=false,length=2) private String country;
    @Column(nullable=false,length=100) private String region;
}
