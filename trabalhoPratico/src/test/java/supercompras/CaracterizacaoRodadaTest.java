package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Caracterizacao do fechamento de rodada no sistema legado. Registra o
 * comportamento atual, inclusive os defeitos, sem corrigir nada.
 */
public class CaracterizacaoRodadaTest {

    private Pedido doComprador(List<Pedido> pedidos, String id) {
        for (Pedido p : pedidos) {
            if (p.getCompradorId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Test
    public void rodadaSimplesRetornaResultado() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00").frete("0.50");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 2));

        ResultadoRodada r = s.fecharRodada();

        assertEquals("mercado", r.getMercadoVencedor());
        assertEquals("PROTO", r.getProtocolo());
        assertEquals("ana", r.getPedidos().get(0).getCompradorId());
    }

    // Hoje o frete e dividido igualmente, ignorando a quantidade de cada comprador.
    @Test
    public void dividiFreteIgualmenteEntreCompradores() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00").frete("3.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 1));
        s.receberLista("bia", Map.of("LEI-0001", 3));

        List<Pedido> pedidos = s.fecharRodada().getPedidos();

        assertEquals(0, doComprador(pedidos, "ana").getFrete().compareTo(doComprador(pedidos, "bia").getFrete()));
    }

    // Hoje a escolha do mercado olha so o preco dos itens e ignora o frete.
    @Test
    public void escolhaDeMercadoIgnoraOFrete() {
        MercadoFake barato = new MercadoFake("barato").preco("LEI-0001", "1.00").frete("100.00");
        MercadoFake caro = new MercadoFake("caro").preco("LEI-0001", "2.00").frete("0.00");
        SistemaCompras s = new SistemaCompras(List.of(barato, caro), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 1));

        assertEquals("barato", s.fecharRodada().getMercadoVencedor());
    }

    // Hoje nao ha fail-over: se a compra falha, o protocolo fica nulo.
    @Test
    public void naoFazFailoverEDeixaProtocoloNulo() {
        MercadoFake barato = new MercadoFake("a").preco("LEI-0001", "1.00");
        barato.erroCompra = new MercadoIndisponivelException("fora do ar");
        MercadoFake caro = new MercadoFake("b").preco("LEI-0001", "2.00");
        SistemaCompras s = new SistemaCompras(List.of(barato, caro), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 1));

        ResultadoRodada r = s.fecharRodada();

        assertEquals("a", r.getMercadoVencedor());
        assertNull(r.getProtocolo());
    }

    // Hoje o valor monetario sai de double, com escala 1 em vez de 2.
    @Test
    public void valorMonetarioSaiComEscalaUm() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 2));

        BigDecimal subtotal = s.fecharRodada().getPedidos().get(0).getSubtotal();

        assertEquals(1, subtotal.scale());
    }

    // Hoje o lote soma as quantidades do mesmo SKU de varios compradores.
    @Test
    public void loteSomaQuantidadesDeVariosCompradores() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 2));
        s.receberLista("bia", Map.of("LEI-0001", 3));

        ResultadoRodada r = s.fecharRodada();

        assertEquals(0, doComprador(r.getPedidos(), "ana").getSubtotal().compareTo(new BigDecimal("2.00")));
        assertEquals(0, doComprador(r.getPedidos(), "bia").getSubtotal().compareTo(new BigDecimal("3.00")));
        assertEquals(0, r.getTotalLote().compareTo(new BigDecimal("5.00")));
    }
}
