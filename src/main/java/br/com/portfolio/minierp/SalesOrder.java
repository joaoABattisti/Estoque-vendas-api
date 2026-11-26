package br.com.portfolio.minierp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "sales_orders")
public class SalesOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, precision = 12, scale = 2)
  BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false, updatable = false)
  Instant createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  List<OrderItem> items = new ArrayList<>();

  @PrePersist
  void created() {
    createdAt = Instant.now();
  }

  protected SalesOrder() {}

  void add(Product p, int q) {
    OrderItem i = new OrderItem(this, p, q, p.unitPrice);
    items.add(i);
    total = total.add(i.unitPrice.multiply(BigDecimal.valueOf(q)));
  }
}
