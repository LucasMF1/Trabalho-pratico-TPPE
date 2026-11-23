package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/** RN-01 (identificador de comprador) e RN-02 (lista de compras) via API publica. */
public class ValidacaoEntradaTest {

    private RepositorioMemoria repo;
    private MercadoFake mercado;
    private SistemaCompras sistema;

    @Before
    public void preparar() {
        repo = new RepositorioMemoria();
        mercado = new MercadoFake("a").preco("LEI-0001", "1.00").preco("ARR-0002", "2.00");
        sistema = new SistemaCompras(List.of(mercado), repo);
    }

    private static Map<String, Integer> um(String sku, Integer qtd) {
        Map<String, Integer> m = new HashMap<>();
        m.put(sku, qtd);
        return m;
    }

    private static Map<String, Integer> valida() {
        return um("LEI-0001", 1);
    }

    private EntradaInvalidaException rejeita(String id, Map<String, Integer> itens) {
        try {
            sistema.receberLista(id, itens);
        } catch (EntradaInvalidaException e) {
            return e;
        }
        fail("deveria rejeitar");
        return null;
    }

    // RN-01

    @Test
    public void aceitaIdsValidos() {
        String[] ids = {
            "abc", "joao", "a1b", "0ab", "a_b", "a.b", "a-b", "user_name.01-x",
            "a".repeat(30)
        };
        for (String id : ids) {
            sistema.receberLista(id, valida());
        }
        ResultadoRodada r = sistema.fecharRodada();
        assertEquals(ids.length, r.getPedidos().size());
    }

    @Test
    public void rejeitaIdsInvalidos() {
        String[] ids = {
            null, "", "ab", "a".repeat(31), "Joao", "JOAO", "joaO", "joão", "jo ao", " joao",
            "joao ", "joao\n", "jo\nao", "\njoao", "_joao", ".joao", "-joao", "jo@o", "jo/ao"
        };
        for (String id : ids) {
            rejeita(id, valida());
        }
    }

    @Test
    public void idComTamanhoNoLimite() {
        sistema.receberLista("abc", valida());
        sistema.receberLista("a".repeat(30), valida());
        rejeita("ab", valida());
        rejeita("a".repeat(31), valida());
    }

    // RN-02: SKU

    @Test
    public void aceitaSkuValido() {
        sistema.receberLista("joao", um("LEI-0001", 1));
        sistema.receberLista("maria", um("ABC-9999", 1));
    }

    @Test
    public void rejeitaSkuInvalido() {
        String[] skus = {
            "", "LEI0001", "LEI-001", "LEI-00001", "lei-0001", "Lei-0001", "LE-0001",
            "LEIT-0001", "LEI-ABCD", "123-0001", "LEI_0001", " LEI-0001", "LEI-0001 ",
            "LEI-0001\n", "LÉI-0001", "LEI-000a"
        };
        for (String sku : skus) {
            rejeita("joao", um(sku, 1));
        }
    }

    // RN-02: quantidade

    @Test
    public void aceitaQuantidadesNosLimites() {
        sistema.receberLista("umm", um("LEI-0001", 1));
        sistema.receberLista("max", um("LEI-0001", 999));
        sistema.receberLista("meio", um("LEI-0001", 500));
    }

    @Test
    public void rejeitaQuantidadesForaDosLimites() {
        Integer[] qtds = {0, 1000, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, null};
        for (Integer q : qtds) {
            rejeita("joao", um("LEI-0001", q));
        }
    }

    // RN-02: lista

    @Test
    public void rejeitaListaVazia() {
        rejeita("joao", new HashMap<>());
    }

    @Test
    public void rejeitaMapaNulo() {
        rejeita("joao", null);
    }

    @Test
    public void rejeitaChaveNula() {
        rejeita("joao", um(null, 1));
    }

    @Test
    public void aceitaVinteItensERejeitaVinteEUm() {
        Map<String, Integer> vinte = new LinkedHashMap<>();
        for (int i = 1; i <= 20; i++) {
            vinte.put(String.format("LEI-%04d", i), 1);
        }
        sistema.receberLista("joao", vinte);

        Map<String, Integer> vinteEUm = new LinkedHashMap<>(vinte);
        vinteEUm.put("LEI-0021", 1);
        rejeita("maria", vinteEUm);
    }

    @Test
    public void umItemInvalidoRejeitaListaInteira() {
        Map<String, Integer> itens = new LinkedHashMap<>();
        itens.put("LEI-0001", 1);
        itens.put("ARR-0002", 0);
        rejeita("joao", itens);
    }

    // duplicada

    @Test
    public void rejeitaSegundaListaDoMesmoComprador() {
        sistema.receberLista("joao", valida());
        rejeita("joao", um("ARR-0002", 2));
    }

    @Test
    public void segundaListaRejeitadaNaoAlteraPrimeira() {
        sistema.receberLista("joao", um("LEI-0001", 3));
        rejeita("joao", um("ARR-0002", 2));
        ResultadoRodada r = sistema.fecharRodada();
        assertEquals(1, r.getPedidos().size());
        assertEquals(Map.of("LEI-0001", 3), r.getPedidos().get(0).getItens());
    }

    // sem rastro

    @Test
    public void entradaInvalidaNaoReservaComprador() {
        rejeita("joao", um("LEI-0001", 0));
        sistema.receberLista("joao", um("LEI-0001", 2));
        ResultadoRodada r = sistema.fecharRodada();
        assertEquals(1, r.getPedidos().size());
        assertEquals(Map.of("LEI-0001", 2), r.getPedidos().get(0).getItens());
    }

    @Test
    public void idInvalidoNaoDeixaPendencia() {
        rejeita("Joao", valida());
        rejeita(null, valida());
        sistema.receberLista("maria", valida());
        ResultadoRodada r = sistema.fecharRodada();
        assertEquals(1, r.getPedidos().size());
        assertEquals("maria", r.getPedidos().get(0).getCompradorId());
        assertEquals(1, repo.salvos.size());
    }

    // copia defensiva

    @Test
    public void alterarMapaOriginalDepoisNaoMudaPedido() {
        Map<String, Integer> original = new HashMap<>();
        original.put("LEI-0001", 2);
        sistema.receberLista("joao", original);

        original.put("LEI-0001", 999);
        original.put("ARR-0002", 5);
        original.remove("LEI-0001");

        ResultadoRodada r = sistema.fecharRodada();
        Pedido p = r.getPedidos().get(0);
        assertNotNull(p);
        assertEquals(Map.of("LEI-0001", 2), p.getItens());
        assertEquals(Map.of("LEI-0001", 2), mercado.loteNaCompra);
    }

    // mensagem sem valor rejeitado

    private void naoVaza(EntradaInvalidaException e, String... valores) {
        assertNotNull(e.getMessage());
        for (String v : valores) {
            assertFalse("mensagem vazou: " + v, e.getMessage().contains(v));
        }
    }

    @Test
    public void mensagemNaoContemIdRejeitado() {
        naoVaza(rejeita("ABC", valida()), "ABC");
        naoVaza(rejeita("Joao.Silva", valida()), "Joao.Silva", "Joao");
        naoVaza(rejeita("zz", valida()), "zz");
    }

    @Test
    public void mensagemNaoContemSkuRejeitado() {
        naoVaza(rejeita("joao", um("abc-1234", 1)), "abc-1234", "abc");
        naoVaza(rejeita("maria", um("XYZ-12", 1)), "XYZ-12", "XYZ");
    }

    @Test
    public void mensagemNaoContemQuantidadeRejeitada() {
        naoVaza(rejeita("joao", um("LEI-0001", 4242)), "4242");
        naoVaza(rejeita("maria", um("LEI-0001", -7777)), "-7777", "7777");
    }

    @Test
    public void mensagemNaoContemCompradorDuplicado() {
        sistema.receberLista("zeca", valida());
        naoVaza(rejeita("zeca", valida()), "zeca");
    }

    @Test
    public void excecaoEhRuntime() {
        assertTrue(RuntimeException.class.isAssignableFrom(EntradaInvalidaException.class));
    }
}
