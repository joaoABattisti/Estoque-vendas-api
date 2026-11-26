package br.com.portfolio.minierp;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="products",uniqueConstraints=@UniqueConstraint(columnNames="sku"))
public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,length=40) String sku;
 @Column(nullable=false,length=140) String name;
 @Column(nullable=false,precision=12,scale=2) BigDecimal unitPrice;
 @Column(nullable=false) int stockQuantity;
 @Version long version;
 protected Product(){}
 public Product(String sku,String name,BigDecimal price,int qty){this.sku=sku;this.name=name;this.unitPrice=price;this.stockQuantity=qty;}
 void adjust(int delta){if(stockQuantity+delta<0)throw new IllegalArgumentException("Estoque insuficiente.");stockQuantity+=delta;}
}