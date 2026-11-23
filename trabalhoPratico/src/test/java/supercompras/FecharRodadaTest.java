package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** RN-03 a RN-10: fechamento de rodada. */
public class FecharRodadaTest {

    private Map<String, Integer> lista(String sku, int qtd) {
        return Map.of(sku, qtd);
    }

    @Test
    public void somaDosTotaisBateComOTotalDoLote() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00").frete("0.50");
        RepositorioMemoria repo = new RepositorioMemoria();
        SistemaCompras s = new SistemaCompras(List.of(m), repo);
        s.receberLista("ana", lista("LEI-0001", 1));
        s.receberLista("bia", lista("LEI-0001", 2));
        s.receberLista("caio", lista("LEI-0001", 4));

        ResultadoRodada r = s.fecharRodada();

        assertEquals(new BigDecimal("7.50"), r.getTotalLote());
        BigDecimal soma = BigDecimal.ZERO;
        for (Pedido p : r.getPedidos()) {
            soma = soma.add(p.getTotal());
        }
        assertEquals(r.getTotalLote(), soma);
    }

    @Test
    public void pedidosVemOrdenadosPorComprador() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("caio", lista("LEI-0001", 1));
        s.receberLista("ana", lista("LEI-0001", 1));
        s.receberLista("bia", lista("LEI-0001", 1));

        List<Pedido> pedidos = s.fecharRodada().getPedidos();

        assertEquals("ana", pedidos.get(0).getCompradorId());
        assertEquals("bia", pedidos.get(1).getCompradorId());
        assertEquals("caio", pedidos.get(2).getCompradorId());
    }

    @Test
    public void desempateEscolheMenorNome() {
        MercadoFake zebra = new MercadoFake("zebra").preco("LEI-0001", "1.00");
        MercadoFake alfa = new MercadoFake("alfa").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(zebra, alfa), new RepositorioMemoria());
        s.receberLista("ana", lista("LEI-0001", 1));

        assertEquals("alfa", s.fecharRodada().getMercadoVencedor());
    }

    @Test
    public void failoverCompraNoProximoQuandoOMaisBaratoFalha() {
        MercadoFake barato = new MercadoFake("a").preco("LEI-0001", "1.00");
        barato.erroCompra = new MercadoIndisponivelException("fora do ar");
        MercadoFake caro = new MercadoFake("b").preco("LEI-0001", "2.00");
        SistemaCompras s = new SistemaCompras(List.of(barato, caro), new RepositorioMemoria());
        s.receberLista("ana", lista("LEI-0001", 1));

        ResultadoRodada r = s.fecharRodada();

        assertEquals("b", r.getMercadoVencedor());
        assertEquals(1, barato.comprasChamadas);
        assertEquals(1, caro.comprasChamadas);
    }

    @Test
    public void semCotacaoValidaMantemListasPendentes() {
        MercadoFake m = new MercadoFake("a");
        m.erroCotacao = new MercadoIndisponivelException("fora");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", lista("LEI-0001", 1));

        assertThrows(SemCotacaoValidaException.class, s::fecharRodada);
        assertThrows(SemCotacaoValidaException.class, s::fecharRodada);
    }

    @Test
    public void nenhumaCompraAceitaMantemListasPendentes() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00");
        m.erroCompra = new RuntimeException("recusou");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", lista("LEI-0001", 1));

        assertThrows(CompraNaoRealizadaException.class, s::fecharRodada);
        assertThrows(CompraNaoRealizadaException.class, s::fecharRodada);
    }

    @Test
    public void fecharSemListasLancaIllegalState() {
        SistemaCompras s = new SistemaCompras(List.of(new MercadoFake("a")), new RepositorioMemoria());
        assertThrows(IllegalStateException.class, s::fecharRodada);
    }

    @Test
    public void loteEntregueAoMercadoEhImutavel() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", lista("LEI-0001", 1));
        s.fecharRodada();

        assertThrows(UnsupportedOperationException.class, () -> m.loteNaCompra.put("XXX-0000", 1));
        assertThrows(UnsupportedOperationException.class, () -> m.precosNaCompra.put("XXX-0000", BigDecimal.ONE));
    }

    @Test
    public void falhaDePersistenciaNaoImpedeOsDemaisEConsomeAsListas() {
        MercadoFake m = new MercadoFake("a").preco("LEI-0001", "1.00");
        RepositorioMemoria repo = new RepositorioMemoria();
        repo.compradoresQueFalham.add("ana");
        SistemaCompras s = new SistemaCompras(List.of(m), repo);
        s.receberLista("ana", lista("LEI-0001", 1));
        s.receberLista("bia", lista("LEI-0001", 1));

        assertThrows(PersistenciaException.class, s::fecharRodada);
        assertEquals(1, repo.salvos.size());
        assertEquals("bia", repo.salvos.get(0).getCompradorId());
        assertThrows(IllegalStateException.class, s::fecharRodada);
    }
}
