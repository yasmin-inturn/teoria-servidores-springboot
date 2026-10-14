package br.com.loja;

import static org.assertj.core.api.Assertions.assertThat;
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

import tools.jackson.databind.ObjectMapper;

/**
 * Testes de ponta a ponta pela camada HTTP, com MockMvc.
 *
 * O MockMvc executa a pilha do Spring MVC (filtros de seguranca, conversao de
 * JSON, validacao, controllers) sem abrir porta TCP de verdade. E o teste que
 * verifica o que o cliente realmente recebe: codigo de status, corpo e regras
 * de acesso.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiHttpTest {

    @Autowired
    private MockMvc http;

    @Autowired
    private ObjectMapper json;

    @Test
    @DisplayName("GET / devolve a pagina de documentacao com o proprio codigo-fonte")
    void paginaDeDocumentacao() throws Exception {
        String html = http.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("Pontos fortes e filosofia")
                .contains("O codigo do servidor")
                .contains("Conclusao")
                .contains("./mvnw spring-boot:run")
                // a pagina mostra o codigo real do checkout
                .contains("class PedidoService")
                .contains("@Transactional");
    }

    @Test
    @DisplayName("as rotas publicadas em /doc/rotas vem do mapeamento real do servidor")
    void rotasIntrospeccionadas() throws Exception {
        http.perform(get("/doc/rotas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.caminho == '/api/pedidos')]").exists());
    }

    @Test
    @DisplayName("catalogo e publico")
    void catalogoPublico() throws Exception {
        http.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").exists());
    }

    @Test
    @DisplayName("sem token, criar pedido devolve 401")
    void semTokenNaoCriaPedido() throws Exception {
        http.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"sku":"CAB-USBC-2M","quantidade":1}],
                                 "cepEntrega":"01310-100","metodoPagamento":"PIX"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login, pedido e consulta: o fluxo completo do cliente")
    void fluxoCompletoDoCliente() throws Exception {
        String token = autenticar("cliente@loja.com", "cliente12345");

        String corpo = http.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"sku":"CAB-USBC-2M","quantidade":2}],
                                 "cepEntrega":"01310-100","metodoPagamento":"PIX"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.emailCliente").value("cliente@loja.com"))
                .andReturn().getResponse().getContentAsString();

        String numero = json.readTree(corpo).get("numero").asText();

        http.perform(get("/api/pedidos/" + numero).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(numero));
    }

    @Test
    @DisplayName("JSON invalido para antes do banco, com 400 e os campos errados")
    void validacaoDeEntrada() throws Exception {
        String token = autenticar("cliente@loja.com", "cliente12345");

        http.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[],"cepEntrega":"123","metodoPagamento":"PIX"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.itens").exists())
                .andExpect(jsonPath("$.campos.cepEntrega").exists());
    }

    @Test
    @DisplayName("estoque insuficiente responde 409 com os numeros do conflito")
    void estoqueInsuficienteViaHttp() throws Exception {
        String token = autenticar("cliente@loja.com", "cliente12345");

        http.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"sku":"WEB-FHD-03","quantidade":9}],
                                 "cepEntrega":"01310-100","metodoPagamento":"PIX"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sku").value("WEB-FHD-03"))
                .andExpect(jsonPath("$.solicitado").value(9));
    }

    @Test
    @DisplayName("cliente nao mexe no catalogo: 403")
    void clienteNaoEhAdmin() throws Exception {
        String token = autenticar("cliente@loja.com", "cliente12345");

        http.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"NOVO-01","nome":"Produto teste","descricao":"x",
                                 "categoria":"teste","preco":10.00,"quantidadeEstoque":1,"pesoKg":0.100}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin cadastra produto e recebe 201 com Location")
    void adminCadastraProduto() throws Exception {
        String token = autenticar("admin@loja.com", "admin12345");

        http.perform(post("/api/produtos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"MIC-USB-09","nome":"Microfone USB","descricao":"cardioide",
                                 "categoria":"audio","preco":349.00,"quantidadeEstoque":4,"pesoKg":0.450}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("MIC-USB-09"));
    }

    @Test
    @DisplayName("senha errada nao diz se o email existe")
    void senhaErrada() throws Exception {
        http.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"cliente@loja.com","senha":"errada-mesmo"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou senha invalidos"));
    }

    @Test
    @DisplayName("health check responde UP para o orquestrador")
    void healthCheck() throws Exception {
        http.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /** Faz login de verdade e devolve o JWT - o mesmo caminho do front-end. */
    private String autenticar(String email, String senha) throws Exception {
        String resposta = http.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return json.readTree(resposta).get("token").asText();
    }
}
