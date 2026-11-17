package com.pi.erp.product.barcode;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface BarcodeRepository extends JpaRepository<Barcode,Long>,JpaSpecificationExecutor<Barcode> {
 Optional<Barcode> findByCode(String code);
 List<Barcode> findByProductIdOrderById(Long productId);
}
