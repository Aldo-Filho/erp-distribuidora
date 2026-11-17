package com.pi.erp.product.barcode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="product_barcodes") @Getter @Setter
public class Barcode {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="barcode_id") private Long id;
 @Column(name="product_id",nullable=false,updatable=false) private Long productId;
 @Column(nullable=false,updatable=false) private String code;
 @Enumerated(EnumType.STRING) @Column(nullable=false,updatable=false) private BarcodeType type;
 @Column(name="main_code") private boolean mainCode;
 private boolean active=true;
 @Column(name="created_by",updatable=false) private Long createdBy;
 @Column(name="created_at",updatable=false) private java.time.LocalDateTime createdAt;
 @Column(name="updated_by") private Long updatedBy;
 @Column(name="updated_at") private java.time.LocalDateTime updatedAt;
 @PrePersist void create() { createdAt=java.time.LocalDateTime.now(); createdBy=com.pi.erp.auth.CurrentUser.requireId(); }
 @PreUpdate void update() { updatedAt=java.time.LocalDateTime.now(); updatedBy=com.pi.erp.auth.CurrentUser.requireId(); }
}
