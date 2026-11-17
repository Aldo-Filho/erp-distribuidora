package com.pi.erp.financial.category;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory,Long>, JpaSpecificationExecutor<FinancialCategory> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FinancialCategory c order by c.id")
    java.util.List<FinancialCategory> lockConfiguration();
    @Query(value="SELECT * FROM financial_categories WHERE type = :type AND default_for_documents = true FOR UPDATE",nativeQuery=true)
    Optional<FinancialCategory> findDefaultForUpdate(@Param("type") String type);
    @Query(value="SELECT * FROM financial_categories WHERE financial_category_id = :id FOR UPDATE",nativeQuery=true)
    Optional<FinancialCategory> findByIdForUpdate(@Param("id") Long id);
}
