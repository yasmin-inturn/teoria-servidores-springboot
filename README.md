# Atividade Prática de Servidores — Java + Spring Boot

Grupo: Yasmin Inturn, Kleyton, Maria Isabel Henke · Programação Web (UNIVALI)

Servidor HTTP em Java + Spring Boot. A rota `/` serve a documentação da
atividade (pontos fortes, setup, código-fonte e conclusão); o resto é uma API
REST mínima de exemplo, só para provar que o servidor funciona de verdade.

## Rodar

```bash
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

Abra <http://localhost:8080>. Testes: `./mvnw test`.

## Rotas

| Rota                         | O que faz                 |
|-------------------------------|----------------------------|
| `GET /`                       | a documentação             |
| `GET /api/produtos`           | lista produtos             |
| `GET /api/produtos/{id}`      | busca um produto           |
| `POST /api/produtos`          | cria um produto             |
| `DELETE /api/produtos/{id}`   | remove um produto          |
| `GET /actuator/health`        | health check                |

## Estrutura

```
src/main/java/br/com/loja/
├── catalogo/   API de exemplo (em memória, sem banco)
└── doc/        a página de documentação
```

Todo o texto da página (pontos fortes, setup, conclusão) vem de
`application.yml` → seção `documentacao:`. O código mostrado na Seção 3 é lido
do próprio projeto em tempo real, não é colado à mão — por isso, se apagar ou
renomear um arquivo, rode `./mvnw clean` antes do próximo `run`.
