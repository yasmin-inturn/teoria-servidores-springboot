# Loja Virtual - servidor HTTP de estudo (Java 25 + Spring Boot 4)

Projeto da disciplina **Teoria de Servidores**: um servidor HTTP que, ao mesmo tempo,

1. expõe a **API REST** de uma plataforma de e-commerce (catálogo, carrinho, pedidos, pagamento, estoque) e
2. serve em `GET /` a **própria documentação do trabalho**, lendo o próprio código-fonte em tempo de execução.

## Como rodar

Só é preciso ter um **JDK 25** instalado. O Maven vem pelo wrapper (`mvnw`), que baixa o próprio Maven na primeira execução.

```bash
./mvnw spring-boot:run
```

Depois abra <http://localhost:8080> — a documentação do trabalho está lá.

Alternativa empacotada (gera o jar autossuficiente, com o Tomcat dentro):

```bash
./mvnw clean package
java -jar target/loja-virtual-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell/cmd), use `mvnw.cmd` em vez de `./mvnw`.

Testes:

```bash
./mvnw test
```

## Usuários da carga inicial

O banco é H2 **em memória**: nasce vazio a cada start e é populado por `config/CargaInicial.java`.
As senhas são de estudo.

| Email              | Senha          | Perfis         |
|--------------------|----------------|----------------|
| `admin@loja.com`   | `admin12345`   | ADMIN, CLIENTE |
| `cliente@loja.com` | `cliente12345` | CLIENTE        |

## Endpoints

Autenticação (JWT no cabeçalho `Authorization: Bearer <token>`):

| Método | Rota                  | Acesso   |
|--------|-----------------------|----------|
| POST   | `/api/auth/registrar` | público  |
| POST   | `/api/auth/login`     | público  |
| GET    | `/api/auth/eu`        | token    |

Catálogo:

| Método | Rota                        | Acesso  |
|--------|-----------------------------|---------|
| GET    | `/api/produtos`             | público |
| GET    | `/api/produtos/{id}`        | público |
| GET    | `/api/produtos/sku/{sku}`   | público |
| POST   | `/api/produtos`             | ADMIN   |
| PUT    | `/api/produtos/{id}`        | ADMIN   |
| POST   | `/api/produtos/{id}/estoque`| ADMIN   |
| DELETE | `/api/produtos/{id}`        | ADMIN   |

Carrinho e pedidos (exigem token):

| Método | Rota                                | Observação                                  |
|--------|-------------------------------------|---------------------------------------------|
| GET    | `/api/carrinho`                     | carrinho do usuário do token                |
| POST   | `/api/carrinho/itens`               | `{"sku":"...","quantidade":1}`              |
| PUT    | `/api/carrinho/itens/{sku}`         | quantidade 0 remove o item                  |
| DELETE | `/api/carrinho/itens/{sku}`         |                                             |
| DELETE | `/api/carrinho`                     | limpa o carrinho                            |
| GET    | `/api/carrinho/simulacao?cep=&cupom=` | prévia de desconto/frete, sem gravar nada |
| POST   | `/api/pedidos`                      | checkout com itens no corpo                 |
| POST   | `/api/pedidos/do-carrinho`          | checkout do carrinho                        |
| GET    | `/api/pedidos`                      | os meus; ADMIN vê todos                     |
| GET    | `/api/pedidos/{numero}`             | o dono ou ADMIN                             |
| PATCH  | `/api/pedidos/{numero}/status`      | ADMIN move o fluxo                          |
| POST   | `/api/pedidos/{numero}/cancelamento`| dono ou ADMIN, antes do envio               |

Apoio: `/doc/rotas`, `/doc/arquivos`, `/doc/fonte/{caminho}`, `/actuator/health`, `/actuator/metrics`, `/h2-console`.

## Regras de negócio implementadas

- **Frete** (`frete/FreteService`): taxa por região derivada do CEP + peso × custo por kg + manuseio;
  grátis acima de R$ 299 ou com cupom `FRETEGRATIS`; prazo varia por região.
- **Descontos** (`desconto/DescontoService`): cupons `PRIMEIRACOMPRA` (10%, teto R$ 50),
  `BLACK20` (20%, teto R$ 200), `FRETEGRATIS`; desconto progressivo por volume
  (5 itens → 3%, 10 → 5%, 20 → 8%); acumulado limitado a 25% do subtotal.
- **Estoque** (`estoque/EstoqueService`): só funciona dentro de transação (`Propagation.MANDATORY`);
  falta de estoque devolve **409** com SKU, solicitado e disponível.
- **Fluxo de status** (`pedido/StatusPedido`): transições válidas declaradas no enum;
  cancelar antes do envio devolve o estoque.
- **Pagamento simulado** (`pagamento/PagamentoService`): PIX autoriza na hora, boleto fica pendente,
  cartão terminado em `0000` é **recusado** — e a recusa derruba a transação inteira.

## O ponto central: atomicidade do checkout

`PedidoService.criar` roda em um único `@Transactional` e faz, nesta ordem:
baixa de estoque → desconto → frete → pagamento → gravação do pedido.
Se o pagamento for recusado, a exceção sobe e o rollback desfaz a baixa de estoque.
Não existe código de compensação escrito à mão.

Para ver isso acontecer:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"cliente@loja.com","senha":"cliente12345"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s http://localhost:8080/api/produtos | grep -o '"sku":"SSD-1TB-NV","[^}]*'   # estoque antes

curl -s -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"itens":[{"sku":"SSD-1TB-NV","quantidade":1}],"cepEntrega":"69900-000",
       "metodoPagamento":"CARTAO_CREDITO","numeroCartao":"4111111111110000"}'

curl -s http://localhost:8080/api/produtos | grep -o '"sku":"SSD-1TB-NV","[^}]*'   # estoque depois: igual
```

## Estrutura

```
src/main/java/br/com/loja/
├── LojaVirtualApplication.java   main() que sobe o Tomcat embarcado
├── auth/        usuários, perfis, login, emissão de JWT
├── carrinho/    carrinho persistido por usuário
├── catalogo/    produto, repositório, serviço e controller
├── common/      exceções de domínio e tradução para HTTP (ProblemDetail)
├── config/      Spring Security e carga inicial de dados
├── desconto/    regras de cupom e desconto por volume
├── doc/         a página de documentação e a leitura do próprio código
├── estoque/     validação e baixa de estoque
├── frete/       cálculo de frete por região e peso
├── pagamento/   gateway simulado
└── pedido/      checkout transacional e fluxo de status
```

A documentação em `GET /` é renderizada por `templates/documentacao.html` (Thymeleaf) com:

- texto editorial vindo de `application.yml` (seção `documentacao:` — é lá que se editam
  participantes, pontos fortes e conclusão);
- rotas lidas do `RequestMappingHandlerMapping` em tempo de execução;
- código-fonte lido do classpath: o `maven-resources-plugin` copia `src/main/java` para
  `target/classes/fonte` durante o build, então a página mostra o fonte real mesmo rodando como `java -jar`.

## Configuração relevante (`application.yml`)

| Chave | Para quê |
|-------|----------|
| `loja.frete.*` | parâmetros do cálculo de frete |
| `loja.seguranca.jwt-segredo` | chave HMAC do JWT (em produção, variável de ambiente `LOJA_SEGURANCA_JWT_SEGREDO`) |
| `loja.seguranca.expiracao-minutos` | validade do token |
| `documentacao.*` | todo o conteúdo da página servida em `/` |
| `spring.jpa.show-sql` | `true` mostra no console o SQL de cada pedido |
