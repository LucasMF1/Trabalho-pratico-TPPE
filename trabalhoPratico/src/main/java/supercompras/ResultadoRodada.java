package supercompras;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de uma rodada fechada. Imutavel. */
public final class ResultadoRodada {

    private final String mercadoVencedor;
    private final String protocolo;
    private final BigDecimal totalLote;
    private final List<Pedido> pedidos;

    ResultadoRodada(String mercadoVencedor, String protocolo, BigDecimal totalLote, List<Pedido> pedidos) {
        this.mercadoVencedor = mercadoVencedor;
        this.protocolo = protocolo;
        this.totalLote = totalLote;
        this.pedidos = List.copyOf(pedidos);
    }

    public String getMercadoVencedor() {
        return mercadoVencedor;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public BigDecimal getTotalLote() {
        return totalLote;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }
}
