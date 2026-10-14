# Atividade Prática de Servidores — Java + Spring Boot

**Disciplina:** Programação Web (UNIVALI) · **Grupo:** Yasmin Inturn, Kleyton, Maria Isabel Henke

Servidor HTTP em Java com Spring Boot que responde, na rota principal (`/`), a
documentação pedida pela atividade: pontos fortes da tecnologia, pré-requisitos
e setup, o próprio código-fonte do servidor e a conclusão do grupo.

## Como rodar

Só é preciso ter um **JDK 17+** instalado (o projeto usa Java 25 LTS). O Maven
vem pelo wrapper (`mvnw`), que baixa o próprio Maven na primeira execução.

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell/cmd), use `mvnw.cmd` em vez de `./mvnw`. Depois abra
<http://localhost:8080> — a documentação está lá.

Testes: `./mvnw test`

> Se você apagar ou renomear algum arquivo em `src/main/java`, rode
> `./mvnw clean` antes do próximo `run` — sem isso, o arquivo antigo continua
> aparecendo na Seção 3, porque a cópia do fonte só adiciona, não remove.

## O que tem no servidor

A rota principal não se limita a um "Hello World": além da página de
documentação, há uma pequena API REST de exemplo (um catálogo de produtos em
memória, sem banco de dados) só para provar que o servidor é funcional de
verdade.

| Método | Rota                | O que faz                          |
|--------|---------------------|-------------------------------------|
| GET    | `/`                 | a documentação da atividade         |
| GET    | `/api/produtos`     | lista os produtos                   |
| GET    | `/api/produtos/{id}`| busca um produto (404 se não existe)|
| POST   | `/api/produtos`     | cria um produto (valida o JSON)     |
| DELETE | `/api/produtos/{id}`| remove um produto                   |
| GET    | `/actuator/health`  | health check                        |

Exemplo:

```bash
curl http://localhost:8080/api/produtos
curl -X POST http://localhost:8080/api/produtos \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Webcam","preco":199.90,"estoque":3}'
```

## Estrutura

```
src/main/java/br/com/loja/
├── LojaVirtualApplication.java   main() que sobe o Tomcat embarcado
├── catalogo/   a API de exemplo (Produto, Service, Controller em memória)
└── doc/        a página de documentação e a leitura do próprio código
```

A página em `GET /` é renderizada por `templates/documentacao.html`
(Thymeleaf). Todo o texto editorial (pontos fortes, caso de uso, setup,
conclusão) vem de `application.yml`, seção `documentacao:` — editar o texto
não exige recompilar nem mexer no HTML. O código-fonte mostrado na página é
lido do próprio classpath em tempo de execução (`CodigoFonteService`), e a
tabela de rotas é lida do `RequestMappingHandlerMapping` do Spring MVC — nada
disso é copiado à mão, então a página nunca fica desatualizada em relação ao
código.
