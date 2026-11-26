# Mini ERP API

API REST para cadastrar produtos com preço e saldo de estoque.
Registra entradas e saídas e bloqueia vendas sem saldo suficiente.
Cria pedidos com itens, subtotais e valor total calculado.

## Tecnologias

Java 17, Spring Boot 3.5.16, Spring Web, Spring Data JPA/Hibernate, PostgreSQL 17, Maven e Docker Compose.

## Requisitos

JDK 17+, Maven 3.6.3+ e Docker Desktop com Docker Compose.

## Como rodar

~~~bash
docker compose up -d
mvn spring-boot:run
~~~

A API fica em http://localhost:8081. O banco usa a porta 5433. Para parar o banco, execute docker compose down.

## Testes

Execute a suíte automatizada com:

~~~bash
mvn test
~~~

Os testes usam um banco H2 temporário e verificam operações de produtos, entradas e saídas, histórico de movimentações, criação e consulta de pedidos, cálculo de subtotal/total, validações e bloqueio de venda ou saída quando o estoque não é suficiente. Produtos com movimentações ou pedidos associados não podem ser removidos, preservando o histórico.

## Exemplo

Cadastre um produto:

~~~http
POST http://localhost:8081/api/products
Content-Type: application/json

{"sku":"MOUSE-001","name":"Mouse USB","unitPrice":79.90,"initialStock":10}
~~~

Use o id devolvido para criar um pedido:

~~~http
POST http://localhost:8081/api/orders
Content-Type: application/json

{"items":[{"productId":1,"quantity":2}]}
~~~

A resposta traz os itens, preço unitário, subtotal e total. A saída de estoque da venda fica registrada no histórico. Movimentações manuais usam POST /api/products/{id}/movements com type ENTRADA ou SAIDA, quantity e note.

## Captura do Postman/Insomnia

Depois de executar a API, adicione uma captura real da requisição e resposta em docs/images/postman-pedido.png e insira a imagem aqui:

<!-- ![Pedido criado no Postman](docs/images/postman-pedido.png) -->
