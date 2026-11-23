package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertNotEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Regressao dos defeitos do sistema legado (ver SMELLS.md). Cada teste descreve
 * o que o legado fazia de errado e fixa o comportamento corrigido na entrega.
 */
public class RegressaoLegadoTest {

    private Pedido doComprador(List<Pedido> pedidos, String id) {
        for (Pedido p : pedidos) {
            if (p.getCompradorId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    // Legado aceitava qualquer comprador. Agora rejeita fora do formato RN-01.
    @Test
    public void rejeitaCompradorInvalido() {
        SistemaCompras s = new SistemaCompras(List.of(new MercadoFake("m").preco("LEI-0001", "1.00")),
                new RepositorioMemoria());
        assertThrows(EntradaInvalidaException.class, () -> s.receberLista("AB", Map.of("LEI-0001", 1)));
    }

    // Legado ignorava lista nula em silencio. Agora e entrada invalida.
    @Test
    public void rejeitaListaNula() {
        SistemaCompras s = new SistemaCompras(List.of(new MercadoFake("m").preco("LEI-0001", "1.00")),
                new RepositorioMemoria());
        assertThrows(EntradaInvalidaException.class, () -> s.receberLista("ana", null));
    }

    // Legado nao fazia fail-over e deixava o protocolo nulo. Agora compra no proximo.
    @Test
    public void fazFailoverQuandoOMaisBaratoFalha() {
        MercadoFake barato = new MercadoFake("a").preco("LEI-0001", "1.00");
        barato.erroCompra = new MercadoIndisponivelException("fora do ar");
        MercadoFake caro = new MercadoFake("b").preco("LEI-0001", "2.00");
        SistemaCompras s = new SistemaCompras(List.of(barato, caro), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 1));

        ResultadoRodada r = s.fecharRodada();

        assertEquals("b", r.getMercadoVencedor());
        assertNotNull(r.getProtocolo());
    }

    // Legado dividia o frete igualmente. Agora e proporcional ao subtotal.
    @Test
    public void dividiFreteProporcionalAoSubtotal() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00").frete("3.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 1));
        s.receberLista("bia", Map.of("LEI-0001", 3));

        List<Pedido> pedidos = s.fecharRodada().getPedidos();

        assertEquals(new BigDecimal("0.75"), doComprador(pedidos, "ana").getFrete());
        assertEquals(new BigDecimal("2.25"), doComprador(pedidos, "bia").getFrete());
        assertNotEquals(doComprador(pedidos, "ana").getFrete(), doComprador(pedidos, "bia").getFrete());
    }
}
