package br.com.loja.carrinho;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.carrinho.CarrinhoDtos.AdicionarItemRequest;
import br.com.loja.carrinho.CarrinhoDtos.CarrinhoResponse;
import br.com.loja.carrinho.CarrinhoDtos.QuantidadeRequest;
import br.com.loja.carrinho.CarrinhoDtos.SimulacaoResponse;
import br.com.loja.desconto.DescontoService;
import br.com.loja.frete.FreteService;
import jakarta.validation.Valid;

/**
 * Carrinho do usuario autenticado.
 *
 * Nenhum endpoint recebe o email do cliente: ele vem do token (jwt.getSubject()).
 * Isso fecha a porta para um cliente mexer no carrinho de outro trocando um id
 * na URL - erro classico de API.
 */
@RestController
@RequestMapping("/api/carrinho")
public class CarrinhoController {

    private final CarrinhoService servico;
    private final DescontoService descontos;
    private final FreteService fretes;

    public CarrinhoController(CarrinhoService servico, DescontoService descontos, FreteService fretes) {
        this.servico = servico;
        this.descontos = descontos;
        this.fretes = fretes;
    }

    @GetMapping
    public CarrinhoResponse ver(@AuthenticationPrincipal Jwt jwt) {
        return CarrinhoResponse.de(servico.ver(jwt.getSubject()));
    }

    @PostMapping("/itens")
    public CarrinhoResponse adicionar(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody AdicionarItemRequest requisicao) {
        return CarrinhoResponse.de(
                servico.adicionar(jwt.getSubject(), requisicao.sku(), requisicao.quantidade()));
    }

    @PutMapping("/itens/{sku}")
    public CarrinhoResponse alterar(@AuthenticationPrincipal Jwt jwt, @PathVariable String sku,
                                    @Valid @RequestBody QuantidadeRequest requisicao) {
        return CarrinhoResponse.de(
                servico.alterarQuantidade(jwt.getSubject(), sku, requisicao.quantidade()));
    }

    @DeleteMapping("/itens/{sku}")
    public CarrinhoResponse remover(@AuthenticationPrincipal Jwt jwt, @PathVariable String sku) {
        return CarrinhoResponse.de(servico.remover(jwt.getSubject(), sku));
    }

    @DeleteMapping
    public CarrinhoResponse limpar(@AuthenticationPrincipal Jwt jwt) {
        return CarrinhoResponse.de(servico.limpar(jwt.getSubject()));
    }

    /** Previa de desconto, frete e total - sem criar pedido nem baixar estoque. */
    @GetMapping("/simulacao")
    public SimulacaoResponse simular(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam String cep,
                                     @RequestParam(required = false) String cupom) {
        Carrinho carrinho = servico.ver(jwt.getSubject());
        BigDecimal subtotal = carrinho.subtotal().setScale(2, RoundingMode.HALF_UP);

        var desconto = descontos.calcular(subtotal, carrinho.quantidadeTotal(), cupom);
        var frete = fretes.calcular(cep, carrinho.pesoTotal(),
                subtotal.subtract(desconto.valor()), desconto.freteGratis());

        BigDecimal total = subtotal.subtract(desconto.valor()).add(frete.valor())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new SimulacaoResponse(subtotal, desconto.valor(), desconto.descricao(),
                frete.valor(), frete.regiao(), frete.prazoDias(), total);
    }
}
