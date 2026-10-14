package br.com.loja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicacao.
 *
 * Esta classe e literalmente o servidor: o metodo main sobe um Tomcat
 * EMBARCADO (nao existe servidor externo para instalar), varre o pacote
 * br.com.loja procurando beans (@RestController, @Service, @Repository,
 * @Configuration), monta o grafo de dependencias e passa a escutar na porta
 * configurada. O jar gerado e autossuficiente: java -jar sobe o servidor.
 *
 * O @SpringBootApplication e a soma de tres anotacoes:
 *   @Configuration        - esta classe pode declarar beans;
 *   @EnableAutoConfiguration - liga o que estiver no classpath (Tomcat, Jackson,
 *                              Thymeleaf, Actuator) com padroes sensatos;
 *   @ComponentScan        - procura componentes a partir deste pacote.
 *
 * Comando para rodar: ./mvnw spring-boot:run   (depois abra http://localhost:8080)
 */
@SpringBootApplication
@ConfigurationPropertiesScan // registra os records @ConfigurationProperties
public class LojaVirtualApplication {

    public static void main(String[] args) {
        SpringApplication.run(LojaVirtualApplication.class, args);
    }
}
