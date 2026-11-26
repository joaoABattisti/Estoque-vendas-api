package br.com.portfolio.minierp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long> {
 boolean existsBySku(String sku);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id=:id")
 Optional<Product> lockById(@Param("id") Long id);
}