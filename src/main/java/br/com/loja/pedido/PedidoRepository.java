package br.com.loja.pedido;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumero(String numero);

    List<Pedido> findByEmailClienteIgnoreCaseOrderByCriadoEmDesc(String emailCliente);

    List<Pedido> findAllByOrderByCriadoEmDesc();

    long countByStatus(StatusPedido status);
}
