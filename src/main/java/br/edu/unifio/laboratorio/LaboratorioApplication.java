package br.edu.unifio.laboratorio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Um único jar, seis aplicações: o perfil ativo escolhe qual papel este processo assume.
 *   produtor | portal | comunicacao | triagem | auditoria | indicadores
 * Ex.: java -jar target/laboratorio.jar --spring.profiles.active=portal
 */
@SpringBootApplication
public class LaboratorioApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaboratorioApplication.class, args);
    }
}
