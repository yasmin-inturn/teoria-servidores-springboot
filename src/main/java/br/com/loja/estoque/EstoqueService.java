package br.com.loja.estoque;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.catalogo.Produto;
import br.com.loja.common.EstoqueInsuficienteException;

/**
 * Validacao e baixa de estoque.
 *
 * Propagation.MANDATORY e proposital: este servico so pode ser chamado de
 * dentro de uma transacao que ja existe (a do pedido). Se alguem tentar baixar
 * estoque fora de transacao, o Spring lanca excecao na hora - o tipo de
 * garantia que vale deixar explicita num fluxo de checkout.
 */
@Service
public class EstoqueService {

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservar(Produto produto, int quantidade) {
        if (!produto.temEstoquePara(quantidade)) {
            throw new EstoqueInsuficienteException(produto.getSku(), quantidade, produto.getQuantidadeEstoque());
        }
        produto.somarEstoque(-quantidade);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void devolver(Produto produto, int quantidade) {
        produto.somarEstoque(quantidade);
    }
}
