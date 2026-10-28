package com.A509.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="content_translations", uniqueConstraints=@UniqueConstraint(name="uq_translation_field",columnNames={"resource_type","resource_id","field_name","language"}))
@Getter @Setter @NoArgsConstructor
public class ContentTranslation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="resource_type",nullable=false,length=30) private String resourceType;
 @Column(name="resource_id",nullable=false) private Long resourceId;
 @Column(name="field_name",nullable=false,length=40) private String fieldName;
 @Column(nullable=false,length=5) private String language;
 @Column(name="translated_text",nullable=false,columnDefinition="LONGTEXT") private String translatedText;
 @Column(name="source_hash",nullable=false,length=64) private String sourceHash;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
