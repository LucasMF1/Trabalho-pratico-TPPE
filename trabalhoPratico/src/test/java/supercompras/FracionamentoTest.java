package supercompras;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** RN-07 (fracionamento pelo maior resto) e RN-09 (valores e formato). */
public class FracionamentoTest {

    private static final String AAA = "AAA-0000";
    private static final String BBB = "BBB-0000";

    private SistemaCompras sistema(MercadoFake m) {
        return new SistemaCompras(List.of(m), new RepositorioMemoria());
    }

    private Pedido de(ResultadoRodada r, String comprador) {
        for (Pedido p : r.getPedidos()) {
            if (p.getCompradorId().equals(comprador)) {
                return p;
            }
        }
        throw new AssertionError("pedido ausente: " + comprador);
    }

    private ResultadoRodada anaBiaCaio(String preco, String frete) {
        MercadoFake m = new MercadoFake("a").preco(AAA, preco).frete(frete);
        SistemaCompras s = sistema(m);
        s.receberLista("ana", Map.of(AAA, 1));
        s.receberLista("bia", Map.of(AAA, 2));
        s.receberLista("caio", Map.of(AAA, 4));
        return s.fecharRodada();
    }

    private void assertEscala2(BigDecimal v) {
        assertEquals(2, v.scale());
    }

    // linha do lote = 10.00 (preco*7 arredonda para 10.00)
    private static final String PRECO_LINHA_10 = "1.4285714285714286";

    @Test
    public void apendiceSemFrete() {
        ResultadoRodada r = anaBiaCaio(PRECO_LINHA_10, "0");

        assertEquals(new BigDecimal("1.43"), de(r, "ana").getSubtotal());
        assertEquals(new BigDecimal("2.86"), de(r, "bia").getSubtotal());
        assertEquals(new BigDecimal("5.71"), de(r, "caio").getSubtotal());
        assertEquals(new BigDecimal("1.43"), de(r, "ana").getValoresPorSku().get(AAA));
        assertEquals(new BigDecimal("2.86"), de(r, "bia").getValoresPorSku().get(AAA));
        assertEquals(new BigDecimal("5.71"), de(r, "caio").getValoresPorSku().get(AAA));
        assertEquals(new BigDecimal("0.00"), de(r, "ana").getFrete());
        assertEquals(new BigDecimal("1.43"), de(r, "ana").getTotal());
        assertEquals(new BigDecimal("2.86"), de(r, "bia").getTotal());
        assertEquals(new BigDecimal("5.71"), de(r, "caio").getTotal());
        assertEquals(new BigDecimal("10.00"), r.getTotalLote());
    }

    @Test
    public void apendiceComFrete() {
        ResultadoRodada r = anaBiaCaio(PRECO_LINHA_10, "1.00");

        // frete de 100 centavos sobre subtotais 143/286/571: 14,29,57 pelo maior resto
        assertEquals(new BigDecimal("0.14"), de(r, "ana").getFrete());
        assertEquals(new BigDecimal("0.29"), de(r, "bia").getFrete());
        assertEquals(new BigDecimal("0.57"), de(r, "caio").getFrete());
        assertEquals(new BigDecimal("1.57"), de(r, "ana").getTotal());
        assertEquals(new BigDecimal("3.15"), de(r, "bia").getTotal());
        assertEquals(new BigDecimal("6.28"), de(r, "caio").getTotal());
        assertEquals(new BigDecimal("11.00"), r.getTotalLote());
    }

    @Test
    public void empateNoRestoFavoreceMenorCompradorId() {
        MercadoFake m = new MercadoFake("a").preco(AAA, "0.3333333333333333");
        SistemaCompras s = sistema(m);
        s.receberLista("caio", Map.of(AAA, 1));
        s.receberLista("bia", Map.of(AAA, 1));
        s.receberLista("ana", Map.of(AAA, 1));

        ResultadoRodada r = s.fecharRodada();

        // linha 1.00 em 3 partes iguais: o centavo extra vai para ana
        assertEquals(new BigDecimal("0.34"), de(r, "ana").getSubtotal());
        assertEquals(new BigDecimal("0.33"), de(r, "bia").getSubtotal());
        assertEquals(new BigDecimal("0.33"), de(r, "caio").getSubtotal());
        assertEquals(new BigDecimal("1.00"), r.getTotalLote());
    }

    @Test
    public void multiSkuMantemInvariantesDeSoma() {
        MercadoFake m = new MercadoFake("a").preco(AAA, "2.50").preco(BBB, "1.10").frete("3.33");
        SistemaCompras s = sistema(m);
        s.receberLista("ana", Map.of(AAA, 1, BBB, 3));
        s.receberLista("bia", Map.of(AAA, 2));
        s.receberLista("caio", Map.of(AAA, 4, BBB, 1));

        ResultadoRodada r = s.fecharRodada();

        BigDecimal somaTotais = BigDecimal.ZERO;
        BigDecimal somaSubtotais = BigDecimal.ZERO;
        BigDecimal somaFretes = BigDecimal.ZERO;
        BigDecimal somaAaa = BigDecimal.ZERO;
        BigDecimal somaBbb = BigDecimal.ZERO;
        for (Pedido p : r.getPedidos()) {
            somaTotais = somaTotais.add(p.getTotal());
            somaSubtotais = somaSubtotais.add(p.getSubtotal());
            somaFretes = somaFretes.add(p.getFrete());
            if (p.getValoresPorSku().containsKey(AAA)) {
                somaAaa = somaAaa.add(p.getValoresPorSku().get(AAA));
            }
            if (p.getValoresPorSku().containsKey(BBB)) {
                somaBbb = somaBbb.add(p.getValoresPorSku().get(BBB));
            }
            BigDecimal somaSkus = BigDecimal.ZERO;
            for (BigDecimal v : p.getValoresPorSku().values()) {
                somaSkus = somaSkus.add(v);
            }
            assertEquals(somaSkus, p.getSubtotal());
            assertEquals(p.getSubtotal().add(p.getFrete()), p.getTotal());
        }

        assertEquals(new BigDecimal("17.50"), somaAaa);
        assertEquals(new BigDecimal("4.40"), somaBbb);
        assertEquals(new BigDecimal("21.90"), somaSubtotais);
        assertEquals(new BigDecimal("3.33"), somaFretes);
        assertEquals(new BigDecimal("25.23"), somaTotais);
        assertEquals(r.getTotalLote(), somaTotais);
    }

    @Test
    public void valoresPorSkuSoTemSkusPedidosEItensGuardaQuantidades() {
        MercadoFake m = new MercadoFake("a").preco(AAA, "2.50").preco(BBB, "1.10");
        SistemaCompras s = sistema(m);
        s.receberLista("ana", Map.of(AAA, 1, BBB, 3));
        s.receberLista("bia", Map.of(AAA, 2));

        ResultadoRodada r = s.fecharRodada();

        Pedido ana = de(r, "ana");
        Pedido bia = de(r, "bia");
        assertEquals(Map.of(AAA, 1, BBB, 3), ana.getItens());
        assertEquals(Map.of(AAA, 2), bia.getItens());
        assertEquals(ana.getItens().keySet(), ana.getValoresPorSku().keySet());
        assertEquals(bia.getItens().keySet(), bia.getValoresPorSku().keySet());
    }

    @Test
    public void todosOsValoresTemEscalaDois() {
        // preco e frete informados com escalas diferentes de 2
        ResultadoRodada r = anaBiaCaio("2.5", "1");

        assertEscala2(r.getTotalLote());
        for (Pedido p : r.getPedidos()) {
            assertEscala2(p.getSubtotal());
            assertEscala2(p.getFrete());
            assertEscala2(p.getTotal());
            for (BigDecimal v : p.getValoresPorSku().values()) {
                assertEscala2(v);
            }
        }
    }

    @Test
    public void escalaDoisTambemComFreteZeroEPrecoFracionario() {
        ResultadoRodada r = anaBiaCaio(PRECO_LINHA_10, "0");

        assertEscala2(r.getTotalLote());
        for (Pedido p : r.getPedidos()) {
            assertEscala2(p.getSubtotal());
            assertEscala2(p.getFrete());
            assertEscala2(p.getTotal());
            assertEscala2(p.getValoresPorSku().get(AAA));
        }
    }

    @Test
    public void idEhProtocoloMaisHifenMaisComprador() {
        MercadoFake m = new MercadoFake("a").preco(AAA, "1.00");
        m.protocolo = "P123";
        SistemaCompras s = sistema(m);
        s.receberLista("caio", Map.of(AAA, 1));
        s.receberLista("ana", Map.of(AAA, 1));

        ResultadoRodada r = s.fecharRodada();

        assertEquals("P123", r.getProtocolo());
        assertEquals("ana", r.getPedidos().get(0).getCompradorId());
        assertEquals("caio", r.getPedidos().get(1).getCompradorId());
        for (Pedido p : r.getPedidos()) {
            assertEquals("P123", p.getProtocolo());
            assertEquals("P123-" + p.getCompradorId(), p.getId());
        }
    }
}
