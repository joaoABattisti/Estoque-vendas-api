package br.com.portfolio.minierp;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
  public enum Type {
    ENTRADA,
    SAIDA
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  Product product;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  Type type;

  @Column(nullable = false)
  int quantity;

  @Column(length = 240)
  String note;

  @Column(nullable = false, updatable = false)
  Instant createdAt;

  @PrePersist
  void created() {
    createdAt = Instant.now();
  }

  protected StockMovement() {}

  public StockMovement(Product p, Type t, int q, String n) {
    product = p;
    type = t;
    quantity = q;
    note = n;
  }
}
