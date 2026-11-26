package br.com.portfolio.minierp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:minierp-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class MiniErpApiTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private SalesOrderRepository orderRepository;

  @Autowired private StockMovementRepository movementRepository;

  @Autowired private ProductRepository productRepository;

  @BeforeEach
  void clearDatabase() {
    orderRepository.deleteAll();
    movementRepository.deleteAll();
    productRepository.deleteAll();
  }

  @Test
  void productCanBeCreatedListedUpdatedAndDeleted() throws Exception {
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("TECLADO-001", 0)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stockQuantity").value(0));
    long productId = productRepository.findAll().get(0).id;

    mockMvc
        .perform(get("/api/products/{id}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value("TECLADO-001"));
    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Teclado"));

    mockMvc
        .perform(
            put("/api/products/{id}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Teclado mecânico\",\"unitPrice\":199.90}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Teclado mecânico"));

    mockMvc.perform(delete("/api/products/{id}", productId)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/products/{id}", productId)).andExpect(status().isNotFound());
  }

  @Test
  void orderCalculatesTotalsAndUpdatesStockAndMovementHistory() throws Exception {
    long productId = createProduct("MOUSE-001", 10);

    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJsonRepeatedLine(productId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total").value(159.8))
        .andExpect(jsonPath("$.items[0].subtotal").value(159.8));
    long orderId = orderRepository.findAll().get(0).id;

    mockMvc
        .perform(get("/api/products/{id}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQuantity").value(8));
    mockMvc
        .perform(get("/api/products/{id}/movements", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$[0].type").value("SAIDA"));
    mockMvc
        .perform(get("/api/orders/{id}", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(159.8));
    mockMvc
        .perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    mockMvc.perform(delete("/api/products/{id}", productId)).andExpect(status().isConflict());
  }

  @Test
  void insufficientStockIsRejectedWithoutChangingTheOrderOrBalance() throws Exception {
    long productId = createProduct("MOUSE-002", 3);
    long secondProductId = createProduct("MOUSE-004", 1);

    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(productId, 2, secondProductId, 2)))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            post("/api/products/{id}/movements", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"SAIDA\",\"quantity\":4,\"note\":\"Teste de bloqueio\"}"))
        .andExpect(status().isConflict());

    mockMvc
        .perform(get("/api/products/{id}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQuantity").value(3));
    mockMvc
        .perform(get("/api/products/{id}", secondProductId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQuantity").value(1));
    mockMvc
        .perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  void stockCanBeReceivedAndDuplicateOrInvalidProductsAreRejected() throws Exception {
    long productId = createProduct("MOUSE-003", 0);

    mockMvc
        .perform(
            post("/api/products/{id}/movements", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ENTRADA\",\"quantity\":6,\"note\":\"Reposição\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("ENTRADA"));
    mockMvc
        .perform(get("/api/products/{id}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stockQuantity").value(6));

    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("MOUSE-003", 1)))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"sku\":\"INVALIDO\",\"name\":\"\",\"unitPrice\":0,\"initialStock\":-1}"))
        .andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/products/9999")).andExpect(status().isNotFound());
  }

  private long createProduct(String sku, int stock) throws Exception {
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson(sku, stock)))
        .andExpect(status().isCreated());
    return productRepository.findAll().stream()
        .filter(product -> product.sku.equals(sku))
        .findFirst()
        .orElseThrow()
        .id;
  }

  private String productJson(String sku, int stock) {
    return """
                {"sku":"%s","name":"Teclado","unitPrice":79.90,"initialStock":%d}
                """
        .formatted(sku, stock);
  }

  private String orderJson(long productId, int quantity) {
    return """
                {"items":[{"productId":%d,"quantity":%d}]}
                """
        .formatted(productId, quantity);
  }

  private String orderJson(
      long firstProductId, int firstQuantity, long secondProductId, int secondQuantity) {
    return """
                {"items":[{"productId":%d,"quantity":%d},{"productId":%d,"quantity":%d}]}
                """
        .formatted(firstProductId, firstQuantity, secondProductId, secondQuantity);
  }

  private String orderJsonRepeatedLine(long productId) {
    return """
                {"items":[{"productId":%d,"quantity":1},{"productId":%d,"quantity":1}]}
                """
        .formatted(productId, productId);
  }
}
