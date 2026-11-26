package br.com.portfolio.minierp;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  SalesOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  Product product;

  @Column(nullable = false)
  int quantity;

  @Column(nullable = false, precision = 12, scale = 2)
  BigDecimal unitPrice;

  protected OrderItem() {}

  OrderItem(SalesOrder o, Product p, int q, BigDecimal price) {
    order = o;
    product = p;
    quantity = q;
    unitPrice = price;
  }
}
