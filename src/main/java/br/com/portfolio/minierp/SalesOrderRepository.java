package br.com.portfolio.minierp;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

  @EntityGraph(attributePaths = {"items", "items.product"})
  @Query("select distinct salesOrder from SalesOrder salesOrder where salesOrder.id = :id")
  Optional<SalesOrder> findWithItemsById(@Param("id") Long id);

  @Override
  @EntityGraph(attributePaths = {"items", "items.product"})
  List<SalesOrder> findAll(Sort sort);

  boolean existsByItems_Product_Id(Long productId);
}
