package supercompras;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Caracterizacao da persistencia do legado com H2 em memoria. Registra que hoje
 * salvar e buscar devolve o pedido gravado. Nao corrige nada.
 */
public class CaracterizacaoPersistenciaTest {

    private Pedido pedidoDeAna() {
        Pedido p = new Pedido();
        p.setId("PROTO-ana");
        p.setCompradorId("ana");
        p.setMercado("mercado");
        p.setProtocolo("PROTO");
        p.setItens(Map.of("LEI-0001", 2));
        p.setValoresPorSku(Map.of("LEI-0001", new BigDecimal("2.00")));
        p.setSubtotal(new BigDecimal("2.00"));
        p.setFrete(new BigDecimal("0.00"));
        p.setTotal(new BigDecimal("2.00"));
        return p;
    }

    @Test
    public void salvarEBuscarDevolveOPedido() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:carac;DB_CLOSE_DELAY=-1")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            repo.salvar(pedidoDeAna());

            List<Pedido> achados = repo.buscarPorComprador("ana");

            assertEquals(1, achados.size());
            Pedido p = achados.get(0);
            assertEquals("mercado", p.getMercado());
            assertEquals(Integer.valueOf(2), p.getItens().get("LEI-0001"));
        }
    }
}
