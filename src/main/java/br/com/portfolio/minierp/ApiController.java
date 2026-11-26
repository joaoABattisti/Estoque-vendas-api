package br.com.portfolio.minierp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api") @Transactional
public class ApiController {
 private final ProductRepository products; private final StockMovementRepository movements; private final SalesOrderRepository orders;
 public ApiController(ProductRepository p,StockMovementRepository m,SalesOrderRepository o){products=p;movements=m;orders=o;}
 public record ProductInput(@NotBlank @Size(max=40) String sku,@NotBlank @Size(max=140) String name,@NotNull @DecimalMin("0.01") BigDecimal unitPrice,@NotNull @Min(0) Integer initialStock){}
 public record ProductUpdate(@NotBlank @Size(max=140) String name,@NotNull @DecimalMin("0.01") BigDecimal unitPrice){}
 public record MovementInput(@NotNull StockMovement.Type type,@NotNull @Min(1) Integer quantity,@Size(max=240) String note){}
 public record OrderLine(@NotNull Long productId,@NotNull @Min(1) Integer quantity){}
 public record OrderInput(@NotEmpty List<@Valid OrderLine> items){}
 private Product getProduct(Long id){return products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Produto não encontrado."));}
 private Product locked(Long id){return products.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Produto não encontrado."));}
 private Map<String,Object> productJson(Product p){return Map.of("id",p.id,"sku",p.sku,"name",p.name,"unitPrice",p.unitPrice,"stockQuantity",p.stockQuantity);}
 private Map<String,Object> movementJson(StockMovement m){return Map.of("id",m.id,"productId",m.product.id,"type",m.type,"quantity",m.quantity,"note",m.note==null?"":m.note,"createdAt",m.createdAt);}
 private Map<String,Object> orderJson(SalesOrder o){
  List<Map<String,Object>> lines=o.items.stream().map(i->Map.<String,Object>of("productId",i.product.id,"productName",i.product.name,"quantity",i.quantity,"unitPrice",i.unitPrice,"subtotal",i.unitPrice.multiply(BigDecimal.valueOf(i.quantity)))).toList();
  return Map.of("id",o.id,"items",lines,"total",o.total,"createdAt",o.createdAt);
 }
 @PostMapping("/products") @Transactional public ResponseEntity<?> create(@Valid @RequestBody ProductInput r){
  if(products.existsBySku(r.sku()))throw new ResponseStatusException(HttpStatus.CONFLICT,"SKU já cadastrado.");
  Product p=products.save(new Product(r.sku(),r.name(),r.unitPrice(),r.initialStock()));
  if(r.initialStock()>0)movements.save(new StockMovement(p,StockMovement.Type.ENTRADA,r.initialStock(),"Estoque inicial"));
  return ResponseEntity.status(201).body(productJson(p));
 }
 @GetMapping("/products") public List<?> list(){return products.findAll(Sort.by("name")).stream().map(this::productJson).toList();}
 @GetMapping("/products/{id}") public Map<String,Object> get(@PathVariable Long id){return productJson(getProduct(id));}
 @PutMapping("/products/{id}") public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody ProductUpdate r){Product p=getProduct(id);p.name=r.name();p.unitPrice=r.unitPrice();return productJson(p);}
 @DeleteMapping("/products/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){products.delete(getProduct(id));return ResponseEntity.noContent().build();}

 @PostMapping("/products/{id}/movements") @Transactional public ResponseEntity<?> movement(@PathVariable Long id,@Valid @RequestBody MovementInput r){
  Product p=locked(id);int delta=r.type()==StockMovement.Type.ENTRADA?r.quantity():-r.quantity();
  if(p.stockQuantity+delta<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Estoque insuficiente.");
  p.adjust(delta);StockMovement m=movements.save(new StockMovement(p,r.type(),r.quantity(),r.note()));
  return ResponseEntity.status(201).body(movementJson(m));
 }
 @GetMapping("/products/{id}/movements") public List<?> history(@PathVariable Long id){getProduct(id);return movements.findByProductIdOrderByCreatedAtDesc(id).stream().map(this::movementJson).toList();}

 @PostMapping("/orders") @Transactional public ResponseEntity<?> order(@Valid @RequestBody OrderInput r){
  Map<Long,Integer> quantities=new TreeMap<>();for(OrderLine l:r.items())quantities.merge(l.productId(),l.quantity(),Integer::sum);
  Map<Long,Product> lockedProducts=new LinkedHashMap<>();for(Long id:quantities.keySet())lockedProducts.put(id,locked(id));
  for(var e:quantities.entrySet())if(lockedProducts.get(e.getKey()).stockQuantity<e.getValue())throw new ResponseStatusException(HttpStatus.CONFLICT,"Estoque insuficiente para o produto "+e.getKey()+".");
  SalesOrder o=new SalesOrder();
  for(var e:quantities.entrySet()){Product p=lockedProducts.get(e.getKey());int q=e.getValue();p.adjust(-q);o.add(p,q);movements.save(new StockMovement(p,StockMovement.Type.SAIDA,q,"Venda do pedido"));}
  return ResponseEntity.status(201).body(orderJson(orders.save(o)));
 }
 @GetMapping("/orders") public List<?> orders(){return orders.findAll(Sort.by(Sort.Direction.DESC,"createdAt")).stream().map(this::orderJson).toList();}
 @GetMapping("/orders/{id}") public Map<String,Object> order(@PathVariable Long id){return orderJson(orders.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pedido não encontrado.")));}
}