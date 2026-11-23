package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** RN-04 (cotacao), RN-05 (total) e RN-06 (desempate). */
public class CotacaoMercadoTest {

    private static final String A = "AAA-0000";
    private static final String B = "AAA-0001";

    private MercadoFake bom() {
        return new MercadoFake("bom").preco(A, "5.00").preco(B, "5.00");
    }

    // mercado barato, que so perde por ser desqualificado
    private MercadoFake barato() {
        return new MercadoFake("barato").preco(A, "0.50").preco(B, "0.50");
    }

    private ResultadoRodada fechar(Map<String, Integer> lista, MercadoFake... mercados) {
        SistemaCompras s = new SistemaCompras(List.<Mercado>of(mercados), new RepositorioMemoria());
        s.receberLista("ana", lista);
        return s.fecharRodada();
    }

    private ResultadoRodada fechar(MercadoFake... mercados) {
        return fechar(Map.of(A, 1, B, 1), mercados);
    }

    private void assertDesqualificado(MercadoFake ruim) {
        MercadoFake bom = bom();
        ResultadoRodada r = fechar(ruim, bom);
        assertEquals("bom", r.getMercadoVencedor());
        assertEquals(0, ruim.comprasChamadas);
    }

    // RN-04: desqualificacao

    @Test
    public void nomeNuloDesqualifica() {
        MercadoFake ruim = barato();
        ruim.nome = null;
        assertDesqualificado(ruim);
    }

    @Test
    public void nomeBrancoDesqualifica() {
        MercadoFake ruim = barato();
        ruim.nome = "   ";
        assertDesqualificado(ruim);
    }

    @Test
    public void cotacaoComMercadoIndisponivelDesqualifica() {
        MercadoFake ruim = barato();
        ruim.erroCotacao = new MercadoIndisponivelException("fora do ar");
        assertDesqualificado(ruim);
    }

    @Test
    public void cotacaoComRuntimeExceptionDesqualifica() {
        MercadoFake ruim = barato();
        ruim.erroCotacao = new IllegalStateException("bug do mercado");
        assertDesqualificado(ruim);
    }

    @Test
    public void mapaDePrecosNuloDesqualifica() {
        MercadoFake ruim = barato();
        ruim.precos = null;
        assertDesqualificado(ruim);
    }

    @Test
    public void faltaPrecoDeUmSkuDesqualifica() {
        MercadoFake ruim = new MercadoFake("barato").preco(A, "0.50");
        assertDesqualificado(ruim);
    }

    @Test
    public void precoNuloDesqualifica() {
        MercadoFake ruim = barato();
        ruim.precos.put(B, null);
        assertDesqualificado(ruim);
    }

    @Test
    public void precoAbaixoDeUmCentavoDesqualifica() {
        MercadoFake ruim = barato().preco(B, "0.009");
        assertDesqualificado(ruim);
    }

    @Test
    public void precoZeroDesqualifica() {
        MercadoFake ruim = barato().preco(B, "0.00");
        assertDesqualificado(ruim);
    }

    @Test
    public void precoNegativoDesqualifica() {
        MercadoFake ruim = barato().preco(B, "-1.00");
        assertDesqualificado(ruim);
    }

    @Test
    public void precoDeUmCentavoEhValido() {
        MercadoFake m = new MercadoFake("centavo").preco(A, "0.01").preco(B, "0.01");
        ResultadoRodada r = fechar(m, bom());
        assertEquals("centavo", r.getMercadoVencedor());
        assertEquals(new BigDecimal("0.02"), r.getTotalLote());
    }

    @Test
    public void freteComMercadoIndisponivelDesqualifica() {
        MercadoFake ruim = barato();
        ruim.erroFrete = new MercadoIndisponivelException("sem frete");
        assertDesqualificado(ruim);
    }

    @Test
    public void freteComRuntimeExceptionDesqualifica() {
        MercadoFake ruim = barato();
        ruim.erroFrete = new IllegalArgumentException("bug");
        assertDesqualificado(ruim);
    }

    @Test
    public void freteNuloDesqualifica() {
        MercadoFake ruim = barato();
        ruim.frete = null;
        assertDesqualificado(ruim);
    }

    @Test
    public void freteNegativoDesqualifica() {
        MercadoFake ruim = barato().frete("-0.01");
        assertDesqualificado(ruim);
    }

    @Test
    public void freteZeroEhValido() {
        MercadoFake m = barato().frete("0");
        ResultadoRodada r = fechar(m, bom());
        assertEquals("barato", r.getMercadoVencedor());
    }

    @Test
    public void errorNaCotacaoPropaga() {
        MercadoFake ruim = barato();
        ruim.erroCotacao = new Error("jvm");
        assertThrows(Error.class, () -> fechar(ruim, bom()));
    }

    @Test
    public void errorNoFretePropaga() {
        MercadoFake ruim = barato();
        ruim.erroFrete = new Error("jvm");
        assertThrows(Error.class, () -> fechar(ruim, bom()));
    }

    @Test
    public void errorNoNomePropaga() {
        MercadoFake ruim = barato();
        ruim.erroNome = new Error("jvm");
        assertThrows(Error.class, () -> fechar(ruim, bom()));
    }

    @Test
    public void semMercadoValidoLancaSemCotacaoValida() {
        MercadoFake x = barato();
        x.erroCotacao = new MercadoIndisponivelException("fora");
        MercadoFake y = barato();
        y.frete = null;
        assertThrows(SemCotacaoValidaException.class, () -> fechar(x, y));
    }

    @Test
    public void chavesExtrasNoMapaDePrecosSaoIgnoradas() {
        MercadoFake m = new MercadoFake("extra").preco(A, "1.00").preco(B, "1.00")
                .preco("ZZZ-9999", "0.00").preco("YYY-8888", "-5");
        ResultadoRodada r = fechar(m);
        assertEquals(new BigDecimal("2.00"), r.getTotalLote());
    }

    @Test
    public void compraRecebeApenasPrecosDoLote() {
        MercadoFake m = new MercadoFake("extra").preco(A, "1.00").preco(B, "1.00")
                .preco("ZZZ-9999", "3.00");
        fechar(m);
        assertEquals(Map.of(A, new BigDecimal("1.00"), B, new BigDecimal("1.00")), m.precosNaCompra);
        assertFalse(m.precosNaCompra.containsKey("ZZZ-9999"));
    }

    // RN-05: total

    @Test
    public void totalSimplesSomaItensMaisFrete() {
        MercadoFake m = new MercadoFake("simples").preco(A, "2.50").frete("1.00");
        ResultadoRodada r = fechar(Map.of(A, 3), m);
        assertEquals(new BigDecimal("8.50"), r.getTotalLote());
        assertEquals(2, r.getTotalLote().scale());
    }

    @Test
    public void totalSomaQuantidadesDeVariosCompradores() {
        MercadoFake m = new MercadoFake("simples").preco(A, "2.00");
        SistemaCompras s = new SistemaCompras(List.<Mercado>of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of(A, 1));
        s.receberLista("bia", Map.of(A, 2));
        assertEquals(new BigDecimal("6.00"), s.fecharRodada().getTotalLote());
    }

    @Test
    public void arredondaLinhaALinhaHalfEven() {
        // linhas: 0.125 -> 0.12 e 0.125 -> 0.12 = 0.24; somar antes daria 0.25
        MercadoFake m = new MercadoFake("par").preco(A, "0.125").preco(B, "0.125");
        ResultadoRodada r = fechar(m);
        assertEquals(new BigDecimal("0.24"), r.getTotalLote());
    }

    @Test
    public void halfEvenArredondaParaParNosDoisSentidos() {
        // 0.125 -> 0.12 (baixo) e 0.135 -> 0.14 (cima)
        MercadoFake m = new MercadoFake("par").preco(A, "0.125").preco(B, "0.135");
        assertEquals(new BigDecimal("0.26"), fechar(m).getTotalLote());
    }

    @Test
    public void arredondaOPrecoVezesQuantidadeNaLinha() {
        // 0.125 * 3 = 0.375 -> 0.38 (HALF_EVEN, 7 impar sobe)
        MercadoFake m = new MercadoFake("par").preco(A, "0.125");
        assertEquals(new BigDecimal("0.38"), fechar(Map.of(A, 3), m).getTotalLote());
    }

    @Test
    public void arredondaOFreteSeparadamente() {
        // itens 0.125 -> 0.12; frete 0.125 -> 0.12; total 0.24
        MercadoFake m = new MercadoFake("par").preco(A, "0.125").frete("0.125");
        assertEquals(new BigDecimal("0.24"), fechar(Map.of(A, 1), m).getTotalLote());
    }

    @Test
    public void freteInfluenciaNoTotalEnaEscolha() {
        MercadoFake semFrete = new MercadoFake("sem-frete").preco(A, "3.00").preco(B, "3.00");
        MercadoFake comFrete = new MercadoFake("com-frete").preco(A, "2.00").preco(B, "2.00").frete("5.00");
        ResultadoRodada r = fechar(comFrete, semFrete);
        assertEquals("sem-frete", r.getMercadoVencedor());
        assertEquals(new BigDecimal("6.00"), r.getTotalLote());
    }

    // RN-06: vencedor e desempate

    @Test
    public void escolheMenorTotal() {
        MercadoFake caro = new MercadoFake("caro").preco(A, "9.00").preco(B, "9.00");
        MercadoFake medio = new MercadoFake("medio").preco(A, "4.00").preco(B, "4.00");
        MercadoFake barato = new MercadoFake("zbarato").preco(A, "1.00").preco(B, "1.00");
        ResultadoRodada r = fechar(caro, barato, medio);
        assertEquals("zbarato", r.getMercadoVencedor());
        assertEquals(new BigDecimal("2.00"), r.getTotalLote());
        assertEquals(1, barato.comprasChamadas);
        assertEquals(0, caro.comprasChamadas);
        assertEquals(0, medio.comprasChamadas);
    }

    @Test
    public void empateDesempataPeloMenorNome() {
        MercadoFake alfa = new MercadoFake("alfa").preco(A, "1.00").preco(B, "1.00");
        MercadoFake beta = new MercadoFake("beta").preco(A, "1.00").preco(B, "1.00");
        assertEquals("alfa", fechar(alfa, beta).getMercadoVencedor());
    }

    @Test
    public void empateDesempataPeloMenorNomeComOrdemInvertida() {
        MercadoFake alfa = new MercadoFake("alfa").preco(A, "1.00").preco(B, "1.00");
        MercadoFake beta = new MercadoFake("beta").preco(A, "1.00").preco(B, "1.00");
        assertEquals("alfa", fechar(beta, alfa).getMercadoVencedor());
    }

    @Test
    public void empateEhNoTotalArredondado() {
        // 0.995 -> 1.00 (HALF_EVEN) empata com 1.00; vence o menor nome
        MercadoFake zeta = new MercadoFake("a-zeta").preco(A, "0.995").preco(B, "0.01");
        MercadoFake outro = new MercadoFake("b-outro").preco(A, "1.00").preco(B, "0.01");
        ResultadoRodada r = fechar(outro, zeta);
        assertEquals(new BigDecimal("1.01"), r.getTotalLote());
        assertEquals("a-zeta", r.getMercadoVencedor());
    }

    @Test
    public void desempateUsaCompareToDeString() {
        // maiusculas vem antes de minusculas em String.compareTo
        MercadoFake maiusc = new MercadoFake("Zeta").preco(A, "1.00").preco(B, "1.00");
        MercadoFake minusc = new MercadoFake("alfa").preco(A, "1.00").preco(B, "1.00");
        assertEquals("Zeta", fechar(minusc, maiusc).getMercadoVencedor());
    }

    @Test
    public void menorTotalVenceMesmoComNomeMaior() {
        MercadoFake alfa = new MercadoFake("alfa").preco(A, "2.00").preco(B, "2.00");
        MercadoFake zeta = new MercadoFake("zeta").preco(A, "1.00").preco(B, "1.00");
        ResultadoRodada r = fechar(alfa, zeta);
        assertTrue(r.getMercadoVencedor().equals("zeta"));
    }
}
