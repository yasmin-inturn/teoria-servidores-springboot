package br.com.loja.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.carrinho.Carrinho;
import br.com.loja.carrinho.CarrinhoService;
import br.com.loja.catalogo.Produto;
import br.com.loja.catalogo.ProdutoRepository;
import br.com.loja.common.RecursoNaoEncontradoException;
import br.com.loja.common.RegraDeNegocioException;
import br.com.loja.desconto.DescontoService;
import br.com.loja.estoque.EstoqueService;
import br.com.loja.frete.FreteService;
import br.com.loja.pagamento.Pagamento;
import br.com.loja.pagamento.PagamentoService;
import br.com.loja.pagamento.StatusPagamento;
import br.com.loja.pedido.PedidoDtos.CheckoutCarrinhoRequest;
import br.com.loja.pedido.PedidoDtos.ItemRequest;
import br.com.loja.pedido.PedidoDtos.PedidoRequest;

/**
 * O coracao do projeto: o checkout.
 *
 * Finalizar um pedido significa fazer QUATRO coisas que precisam valer juntas:
 *   1. validar e baixar o estoque de cada item;
 *   2. calcular desconto e frete sobre o subtotal;
 *   3. registrar o pagamento;
 *   4. gravar o pedido.
 *
 * Tudo acontece dentro de um unico @Transactional. O Spring abre a transacao
 * antes do metodo (via proxy AOP) e decide no fim: commit se o metodo retornou,
 * rollback se escapou uma RuntimeException. Entao, se o pagamento for recusado
 * no passo 3, a baixa de estoque do passo 1 simplesmente nunca e confirmada -
 * nao existe compensacao manual no codigo, e isso e o ponto.
 *
 * Detalhe que costuma passar batido: @Transactional so funciona na chamada que
 * vem de FORA do bean. Se este metodo fosse chamado por outro metodo desta
 * mesma classe, a chamada nao passaria pelo proxy e rodaria sem transacao.
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final EstoqueService estoque;
    private final DescontoService descontos;
    private final FreteService fretes;
    private final PagamentoService pagamentos;
    private final CarrinhoService carrinhos;

    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos, EstoqueService estoque,
                         DescontoService descontos, FreteService fretes, PagamentoService pagamentos,
                         CarrinhoService carrinhos) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.estoque = estoque;
        this.descontos = descontos;
        this.fretes = fretes;
        this.pagamentos = pagamentos;
        this.carrinhos = carrinhos;
    }

    @Transactional
    public Pedido criar(String emailCliente, PedidoRequest requisicao) {
        // 1. Agrupa SKUs repetidos: dois itens do mesmo produto viram um item com
        //    a soma das quantidades (senao a validacao de estoque passaria duas vezes).
        Map<String, Integer> quantidadePorSku = new LinkedHashMap<>();
        for (ItemRequest item : requisicao.itens()) {
            quantidadePorSku.merge(item.sku().toUpperCase(), item.quantidade(), Integer::sum);
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entrada : quantidadePorSku.entrySet()) {
            Produto produto = produtos.findBySku(entrada.getKey())
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", entrada.getKey()));

            // Lanca EstoqueInsuficienteException (-> HTTP 409) e derruba a transacao.
            estoque.reservar(produto, entrada.getValue());

            ItemPedido item = new ItemPedido(produto, entrada.getValue());
            itens.add(item);
            subtotal = subtotal.add(item.subtotal());
            pesoTotal = pesoTotal.add(item.pesoTotal());
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        int quantidadeTotal = itens.stream().mapToInt(ItemPedido::getQuantidade).sum();

        // 2. Regras comerciais: desconto primeiro, frete sobre o subtotal com desconto.
        var desconto = descontos.calcular(subtotal, quantidadeTotal, requisicao.cupom());
        BigDecimal baseFrete = subtotal.subtract(desconto.valor());
        var frete = fretes.calcular(requisicao.cepEntrega(), pesoTotal, baseFrete, desconto.freteGratis());

        BigDecimal total = subtotal.subtract(desconto.valor()).add(frete.valor())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 3. Pagamento: se recusar, lanca excecao e nada acima e confirmado.
        Pagamento pagamento = pagamentos.processar(
                requisicao.metodoPagamento(), total, requisicao.numeroCartao());

        StatusPedido status = pagamento.getStatus() == StatusPagamento.AUTORIZADO
                ? StatusPedido.PAGO
                : StatusPedido.AGUARDANDO_PAGAMENTO;

        // 4. Gravacao do agregado (pedido + itens + pagamento) em cascata.
        Pedido pedido = new Pedido(gerarNumero(), emailCliente, status, itens, pagamento,
                requisicao.cepEntrega(), subtotal, desconto.valor(), frete.valor(), total,
                frete.prazoDias(),
                "desconto: %s | frete: %s".formatted(desconto.descricao(), frete.descricao()));

        Pedido salvo = pedidos.save(pedido);
        log.info("Pedido {} criado para {} total={} status={}",
                salvo.getNumero(), emailCliente, total, status);
        return salvo;
    }

    /**
     * Checkout a partir do carrinho: converte os itens e reaproveita criar().
     *
     * A chamada interna a criar() NAO passa pelo proxy do Spring (self-invocation),
     * ou seja, a anotacao @Transactional dele e ignorada aqui. Funciona porque
     * ESTE metodo ja abriu a transacao e ela vale para toda a chamada - mesmo
     * efeito de Propagation.REQUIRED. Se este metodo nao fosse transacional, o
     * EstoqueService (MANDATORY) falharia na hora, de proposito.
     */
    @Transactional
    public Pedido criarDoCarrinho(String emailCliente, CheckoutCarrinhoRequest requisicao) {
        Carrinho carrinho = carrinhos.obterOuCriar(emailCliente);
        if (carrinho.vazio()) {
            throw new RegraDeNegocioException("Carrinho vazio");
        }

        List<ItemRequest> itens = carrinho.getItens().stream()
                .map(item -> new ItemRequest(item.getProduto().getSku(), item.getQuantidade()))
                .toList();

        Pedido pedido = criar(emailCliente, new PedidoRequest(itens, requisicao.cepEntrega(),
                requisicao.metodoPagamento(), requisicao.numeroCartao(), requisicao.cupom()));

        carrinho.limpar();
        return pedido;
    }

    @Transactional(readOnly = true)
    public Pedido buscar(String numero, String emailSolicitante, boolean administrador) {
        Pedido pedido = pedidos.findByNumero(numero)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pedido", numero));

        if (!administrador && !pedido.getEmailCliente().equalsIgnoreCase(emailSolicitante)) {
            throw new AccessDeniedException("Pedido de outro cliente");
        }
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar(String emailSolicitante, boolean administrador) {
        return administrador
                ? pedidos.findAllByOrderByCriadoEmDesc()
                : pedidos.findByEmailClienteIgnoreCaseOrderByCriadoEmDesc(emailSolicitante);
    }

    /**
     * Avanca o fluxo de status. Cancelamento antes do envio devolve os itens ao
     * estoque - na mesma transacao, claro.
     */
    @Transactional
    public Pedido mudarStatus(String numero, StatusPedido novoStatus) {
        Pedido pedido = pedidos.findByNumero(numero)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pedido", numero));

        boolean devolverEstoque = novoStatus == StatusPedido.CANCELADO
                && pedido.getStatus().devolveEstoqueAoCancelar();

        pedido.mudarStatus(novoStatus);

        if (devolverEstoque) {
            pedido.getItens().forEach(item -> estoque.devolver(item.getProduto(), item.getQuantidade()));
            log.info("Pedido {} cancelado, estoque devolvido", numero);
        }
        return pedido;
    }

    /** Numero legivel para o cliente, unico o suficiente para o escopo do estudo. */
    private String gerarNumero() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
