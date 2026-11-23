package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Caracterizacao da entrada no sistema legado. Registra o comportamento atual,
 * inclusive onde esta errado, sem corrigir nada.
 */
public class CaracterizacaoEntradaTest {

    // Hoje o legado aceita qualquer identificador de comprador, sem validar.
    @Test
    public void aceitaCompradorForaDoFormato() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("AB", Map.of("LEI-0001", 1));

        ResultadoRodada r = s.fecharRodada();

        assertEquals("AB", r.getPedidos().get(0).getCompradorId());
    }

    // Hoje uma lista nula e ignorada em silencio, sem lancar excecao.
    @Test
    public void ignoraListaNulaSemLancar() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", null);

        ResultadoRodada r = s.fecharRodada();

        assertTrue(r.getPedidos().isEmpty());
    }

    // Hoje a segunda lista do mesmo comprador na rodada sobrescreve a primeira.
    @Test
    public void listaDuplicadaSobrescreveAPrimeira() {
        MercadoFake m = new MercadoFake("mercado").preco("LEI-0001", "1.00").preco("ARR-0002", "1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        s.receberLista("ana", Map.of("LEI-0001", 1));
        s.receberLista("ana", Map.of("ARR-0002", 5));

        Map<String, Integer> itens = s.fecharRodada().getPedidos().get(0).getItens();

        assertTrue(itens.containsKey("ARR-0002"));
        assertFalse(itens.containsKey("LEI-0001"));
    }
}
