package supercompras;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cotacao valida de um mercado. Guarda uma copia imediata dos precos (apenas
 * dos SKUs do lote) e do frete, imune a alteracoes posteriores do mercado.
 */
final class Cotacao {

    private final Mercado mercado;
    private final String nome;
    private final Map<String, BigDecimal> precos;
    private final BigDecimal frete;
    private final BigDecimal total;

    Cotacao(Mercado mercado, String nome, Map<String, BigDecimal> precos, BigDecimal frete, BigDecimal total) {
        this.mercado = mercado;
        this.nome = nome;
        this.precos = Map.copyOf(precos);
        this.frete = frete;
        this.total = total;
    }

    Mercado mercado() {
        return mercado;
    }

    String nome() {
        return nome;
    }

    Map<String, BigDecimal> precos() {
        return precos;
    }

    BigDecimal frete() {
        return frete;
    }

    BigDecimal total() {
        return total;
    }
}
