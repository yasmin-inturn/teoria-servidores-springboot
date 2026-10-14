package br.com.loja.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * O Spring Data gera a implementacao desta interface em tempo de execucao (um
 * proxy dinamico). Os nomes dos metodos viram consultas JPQL: nao existe SQL
 * escrito a mao aqui, mas da para ver o SQL gerado no log com
 * spring.jpa.show-sql=true.
 */
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findBySku(String sku);

    List<Produto> findByAtivoTrueOrderByNomeAsc();

    List<Produto> findByCategoriaIgnoreCaseAndAtivoTrueOrderByNomeAsc(String categoria);

    boolean existsBySku(String sku);
}
