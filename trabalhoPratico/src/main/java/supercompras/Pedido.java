package supercompras;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pedido de um comprador em uma rodada. Imutavel: os mapas devolvidos nao podem
 * ser alterados por quem os recebe.
 */
public final class Pedido {

    private final String id;
    private final String compradorId;
    private final String mercado;
    private final String protocolo;
    private final Map<String, Integer> itens;
    private final Map<String, BigDecimal> valoresPorSku;
    private final BigDecimal subtotal;
    private final BigDecimal frete;
    private final BigDecimal total;

    Pedido(String compradorId, String mercado, String protocolo, Map<String, Integer> itens,
            Map<String, BigDecimal> valoresPorSku, BigDecimal subtotal, BigDecimal frete, BigDecimal total) {
        this.id = protocolo + "-" + compradorId;
        this.compradorId = compradorId;
        this.mercado = mercado;
        this.protocolo = protocolo;
        this.itens = Map.copyOf(itens);
        this.valoresPorSku = Map.copyOf(valoresPorSku);
        this.subtotal = subtotal;
        this.frete = frete;
        this.total = total;
    }

    public String getId() {
        return id;
    }

    public String getCompradorId() {
        return compradorId;
    }

    public String getMercado() {
        return mercado;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public Map<String, Integer> getItens() {
        return itens;
    }

    public Map<String, BigDecimal> getValoresPorSku() {
        return valoresPorSku;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
