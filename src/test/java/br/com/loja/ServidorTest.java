package br.com.loja;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes pela camada HTTP, com MockMvc: executam a pilha real do Spring MVC
 * sem abrir porta TCP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServidorTest {

    @Autowired
    private MockMvc http;

    @Test
    @DisplayName("GET / devolve a página de documentação com as 4 seções pedidas")
    void paginaDeDocumentacao() throws Exception {
        http.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Pontos fortes e filosofia"),
                        org.hamcrest.Matchers.containsString("Pré-requisitos e setup"),
                        org.hamcrest.Matchers.containsString("O código do servidor"),
                        org.hamcrest.Matchers.containsString("Conclusão"),
                        org.hamcrest.Matchers.containsString("class ProdutoController"))));
    }

    @Test
    @DisplayName("a API de exemplo lista produtos em JSON")
    void listaProdutos() throws Exception {
        http.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").exists());
    }

    @Test
    @DisplayName("criar produto valida a entrada e devolve 201")
    void criaProduto() throws Exception {
        http.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Webcam\",\"preco\":199.90,\"estoque\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Webcam"));
    }

    @Test
    @DisplayName("produto inexistente devolve 404")
    void produtoInexistente() throws Exception {
        http.perform(get("/api/produtos/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("health check responde UP")
    void healthCheck() throws Exception {
        http.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
