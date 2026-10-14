package br.com.loja.pedido;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedido.PedidoDtos.CheckoutCarrinhoRequest;
import br.com.loja.pedido.PedidoDtos.MudancaStatusRequest;
import br.com.loja.pedido.PedidoDtos.PedidoRequest;
import br.com.loja.pedido.PedidoDtos.PedidoResponse;
import jakarta.validation.Valid;

/**
 * Endpoints de pedido.
 *
 * O caminho de uma requisicao POST /api/pedidos ate o banco:
 *   Tomcat (thread do pool) -> filtros do Spring Security (valida o JWT)
 *   -> DispatcherServlet -> este @RestController (Jackson desserializa o JSON,
 *   Bean Validation valida) -> PedidoService (@Transactional)
 *   -> repositorios (JPA/Hibernate) -> commit -> JSON de resposta.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService servico;

    public PedidoController(PedidoService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody PedidoRequest requisicao) {
        Pedido pedido = servico.criar(jwt.getSubject(), requisicao);
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + pedido.getNumero()))
                .body(PedidoResponse.de(pedido));
    }

    @PostMapping("/do-carrinho")
    public ResponseEntity<PedidoResponse> criarDoCarrinho(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody CheckoutCarrinhoRequest requisicao) {
        Pedido pedido = servico.criarDoCarrinho(jwt.getSubject(), requisicao);
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + pedido.getNumero()))
                .body(PedidoResponse.de(pedido));
    }

    @GetMapping
    public List<PedidoResponse> listar(Authentication autenticacao) {
        return servico.listar(autenticacao.getName(), administrador(autenticacao))
                .stream().map(PedidoResponse::de).toList();
    }

    @GetMapping("/{numero}")
    public PedidoResponse porNumero(@PathVariable String numero, Authentication autenticacao) {
        return PedidoResponse.de(
                servico.buscar(numero, autenticacao.getName(), administrador(autenticacao)));
    }

    /** Avanco do fluxo operacional: so o administrador move o pedido na esteira. */
    @PatchMapping("/{numero}/status")
    public PedidoResponse mudarStatus(@PathVariable String numero,
                                      @Valid @RequestBody MudancaStatusRequest requisicao,
                                      Authentication autenticacao) {
        if (!administrador(autenticacao)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas administradores mudam o status do pedido");
        }
        return PedidoResponse.de(servico.mudarStatus(numero, requisicao.novoStatus()));
    }

    /** O cliente pode cancelar o proprio pedido enquanto ele nao foi enviado. */
    @PostMapping("/{numero}/cancelamento")
    public PedidoResponse cancelar(@PathVariable String numero, Authentication autenticacao) {
        servico.buscar(numero, autenticacao.getName(), administrador(autenticacao));
        return PedidoResponse.de(servico.mudarStatus(numero, StatusPedido.CANCELADO));
    }

    private boolean administrador(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
