package supercompras;

import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** RN-03 (lote imutavel), RN-12 (objetos devolvidos imutaveis) e propagacao de Error. */
public class ImutabilidadeTest {

    private MercadoFake mercado;
    private SistemaCompras sistema;

    private ResultadoRodada fechar() {
        mercado = new MercadoFake("a").preco("LEI-0001", "1.00");
        sistema = new SistemaCompras(List.of(mercado), new RepositorioMemoria());
        sistema.receberLista("ana", Map.of("LEI-0001", 2));
        return sistema.fecharRodada();
    }

    @Test
    public void loteEntregueAoMercadoEImutavel() {
        fechar();

        assertThrows(UnsupportedOperationException.class,
                () -> mercado.loteNaCompra.put("XXX-0000", 1));
        assertThrows(UnsupportedOperationException.class,
                () -> mercado.loteNaCompra.clear());
    }

    @Test
    public void precosEntreguesAoMercadoSaoImutaveis() {
        fechar();

        assertThrows(UnsupportedOperationException.class,
                () -> mercado.precosNaCompra.put("XXX-0000", BigDecimal.ONE));
    }

    @Test
    public void itensDoPedidoSaoImutaveis() {
        Pedido p = fechar().getPedidos().get(0);

        assertThrows(UnsupportedOperationException.class,
                () -> p.getItens().put("XXX-0000", 1));
        assertThrows(UnsupportedOperationException.class,
                () -> p.getItens().clear());
    }

    @Test
    public void valoresPorSkuDoPedidoSaoImutaveis() {
        Pedido p = fechar().getPedidos().get(0);

        assertThrows(UnsupportedOperationException.class,
                () -> p.getValoresPorSku().put("XXX-0000", BigDecimal.ONE));
        assertThrows(UnsupportedOperationException.class,
                () -> p.getValoresPorSku().clear());
    }

    @Test
    public void listaDePedidosDoResultadoEImutavel() {
        ResultadoRodada r = fechar();
        Pedido p = r.getPedidos().get(0);

        assertThrows(UnsupportedOperationException.class, () -> r.getPedidos().add(p));
        assertThrows(UnsupportedOperationException.class, () -> r.getPedidos().clear());
    }

    @Test
    public void errorDoMercadoNaoECapturadoEPropaga() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00");
        m.erroCotacao = new Error("jvm");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 2));

        assertThrows(Error.class, s::fecharRodada);
    }
}
