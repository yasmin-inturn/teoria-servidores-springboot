package br.com.loja.config;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.auth.Perfil;
import br.com.loja.auth.Usuario;
import br.com.loja.auth.UsuarioRepository;
import br.com.loja.catalogo.Produto;
import br.com.loja.catalogo.ProdutoRepository;

/**
 * Carga inicial para o banco H2 em memoria, que nasce vazio a cada start.
 *
 * Um ApplicationRunner roda uma vez, depois que o contexto subiu e antes de o
 * servidor comecar a aceitar trafego - exatamente onde cabe um seed de
 * demonstracao. As senhas sao de estudo e estao no README de proposito.
 */
@Configuration
public class CargaInicial {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    @Bean
    ApplicationRunner carregarDadosDeDemonstracao(ProdutoRepository produtos,
                                                  UsuarioRepository usuarios,
                                                  PasswordEncoder encoder) {
        return argumentos -> popular(produtos, usuarios, encoder);
    }

    @Transactional
    void popular(ProdutoRepository produtos, UsuarioRepository usuarios, PasswordEncoder encoder) {
        if (produtos.count() > 0) {
            return;
        }

        produtos.saveAll(List.of(
                new Produto("TEC-MEC-01", "Teclado mecanico 75%", "Switches lineares, ABNT2, USB-C",
                        "perifericos", new BigDecimal("389.90"), 12, new BigDecimal("0.850")),
                new Produto("MOU-LOG-02", "Mouse sem fio 2.4G", "6 botoes programaveis, 12000 DPI",
                        "perifericos", new BigDecimal("159.90"), 30, new BigDecimal("0.120")),
                new Produto("MON-27-4K", "Monitor 27 polegadas 4K", "IPS, 60Hz, HDMI e DisplayPort",
                        "monitores", new BigDecimal("1899.00"), 5, new BigDecimal("6.400")),
                new Produto("SSD-1TB-NV", "SSD NVMe 1TB", "Leitura 3500 MB/s, PCIe 3.0",
                        "armazenamento", new BigDecimal("449.00"), 20, new BigDecimal("0.060")),
                new Produto("HEA-BT-05", "Headset bluetooth", "Cancelamento de ruido, 30h de bateria",
                        "audio", new BigDecimal("599.00"), 8, new BigDecimal("0.320")),
                new Produto("CAD-ERG-07", "Cadeira ergonomica", "Apoio lombar ajustavel, rodinhas silenciosas",
                        "mobiliario", new BigDecimal("1249.00"), 3, new BigDecimal("18.500")),
                new Produto("CAB-USBC-2M", "Cabo USB-C 2m", "Trancado, 100W, dados 480Mbps",
                        "acessorios", new BigDecimal("59.90"), 100, new BigDecimal("0.090")),
                new Produto("WEB-FHD-03", "Webcam Full HD", "1080p 30fps, microfone estereo",
                        "perifericos", new BigDecimal("279.00"), 1, new BigDecimal("0.180"))));

        if (!usuarios.existsByEmailIgnoreCase("admin@loja.com")) {
            usuarios.save(new Usuario("admin@loja.com", encoder.encode("admin12345"),
                    "Administradora da loja", EnumSet.of(Perfil.ADMIN, Perfil.CLIENTE)));
        }
        if (!usuarios.existsByEmailIgnoreCase("cliente@loja.com")) {
            usuarios.save(new Usuario("cliente@loja.com", encoder.encode("cliente12345"),
                    "Cliente de demonstracao", EnumSet.of(Perfil.CLIENTE)));
        }

        log.info("Carga inicial: {} produtos, {} usuarios", produtos.count(), usuarios.count());
    }
}
