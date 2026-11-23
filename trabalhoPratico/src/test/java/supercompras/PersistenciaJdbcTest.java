package supercompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/** RN-11 (seguranca/SQL) e RN-12 (robustez da persistencia). */
public class PersistenciaJdbcTest {

    private static final String HOSTIL = "Mercado \"X\"; DROP TABLE pedido;--";

    private Connection abrir(String nome) throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + nome + ";DB_CLOSE_DELAY=-1");
    }

    /** Gera pedidos reais pela API, um por comprador. */
    private List<Pedido> gerar(String mercado, String... compradores) {
        MercadoFake m = new MercadoFake(mercado).preco("LEI-0001", "1.00").preco("PAO-0002", "2.50")
                .frete("1.00");
        SistemaCompras s = new SistemaCompras(List.of(m), new RepositorioMemoria());
        int qtd = 1;
        for (String c : compradores) {
            s.receberLista(c, Map.of("LEI-0001", qtd, "PAO-0002", qtd + 1));
            qtd++;
        }
        return s.fecharRodada().getPedidos();
    }

    private int contar(Connection c, String tabela) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    public void nomeHostilEGravadoELidoLiteralmente() throws Exception {
        try (Connection c = abrir("hostil")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            Pedido p = gerar(HOSTIL, "ana").get(0);

            repo.salvar(p);
            List<Pedido> lidos = repo.buscarPorComprador("ana");

            assertEquals(1, lidos.size());
            assertEquals(HOSTIL, lidos.get(0).getMercado());
            assertEquals(1, contar(c, "pedido"));
        }
    }

    @Test
    public void injecaoNaBuscaRetornaVazioEPreservaBanco() throws Exception {
        try (Connection c = abrir("injecao")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            repo.salvar(gerar("a", "ana").get(0));

            assertTrue(repo.buscarPorComprador("x'; DROP TABLE pedido;--").isEmpty());
            assertTrue(repo.buscarPorComprador("ana' OR '1'='1").isEmpty());

            assertEquals(1, contar(c, "pedido"));
            assertEquals(1, repo.buscarPorComprador("ana").size());
        }
    }

    @Test
    public void idForaDoFormatoRetornaVazioSemLancar() throws Exception {
        try (Connection c = abrir("idinvalido")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            repo.salvar(gerar("a", "ana").get(0));

            assertTrue(repo.buscarPorComprador("AB").isEmpty());
            assertTrue(repo.buscarPorComprador("x'; DROP").isEmpty());
            assertTrue(repo.buscarPorComprador("").isEmpty());
            assertTrue(repo.buscarPorComprador(null).isEmpty());

            assertEquals(1, contar(c, "pedido"));
        }
    }

    @Test
    public void buscaNuncaRetornaPedidoDeOutroComprador() throws Exception {
        try (Connection c = abrir("isolamento")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            for (Pedido p : gerar("a", "ana", "bia", "caio")) {
                repo.salvar(p);
            }

            List<Pedido> deBia = repo.buscarPorComprador("bia");

            assertEquals(1, deBia.size());
            for (Pedido p : deBia) {
                assertEquals("bia", p.getCompradorId());
            }
            assertTrue(repo.buscarPorComprador("zeca").isEmpty());
        }
    }

    @Test
    public void conexaoNaoEFechadaEPodeSerReutilizada() throws Exception {
        try (Connection c = abrir("naofecha")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            repo.salvar(gerar("a", "ana").get(0));
            repo.buscarPorComprador("ana");

            assertFalse(c.isClosed());
            assertEquals(1, contar(c, "pedido"));
        }
    }

    @Test
    public void autocommitTrueERestauradoAposSalvar() throws Exception {
        try (Connection c = abrir("autocommit_true")) {
            c.setAutoCommit(true);
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);

            repo.salvar(gerar("a", "ana").get(0));

            assertTrue(c.getAutoCommit());
            assertEquals(1, repo.buscarPorComprador("ana").size());
        }
    }

    @Test
    public void autocommitFalseEMantidoAposSalvar() throws Exception {
        try (Connection c = abrir("autocommit_false")) {
            c.setAutoCommit(false);
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);

            repo.salvar(gerar("a", "ana").get(0));

            assertFalse(c.getAutoCommit());
            assertEquals(1, repo.buscarPorComprador("ana").size());
        }
    }

    @Test
    public void falhaSqlViraPersistenciaExceptionComCausa() throws Exception {
        Connection c = abrir("falha");
        PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
        Pedido p = gerar("a", "ana").get(0);
        c.close();

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.salvar(p));

        assertNotNull(e.getCause());
        assertTrue(e.getCause() instanceof SQLException);
    }

    @Test
    public void falhaSqlNaBuscaViraPersistenciaException() throws Exception {
        Connection c = abrir("falha_busca");
        PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
        c.close();

        PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.buscarPorComprador("ana"));

        assertTrue(e.getCause() instanceof SQLException);
    }

    @Test
    public void idDuplicadoFazRollbackEMantemConexaoUtil() throws Exception {
        try (Connection c = abrir("duplicado")) {
            c.setAutoCommit(true);
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            Pedido p = gerar("a", "ana").get(0);
            repo.salvar(p);

            PersistenciaException e = assertThrows(PersistenciaException.class, () -> repo.salvar(p));

            assertTrue(e.getCause() instanceof SQLException);
            assertTrue(c.getAutoCommit());
            assertFalse(c.isClosed());
            assertEquals(1, contar(c, "pedido"));
            assertEquals(p.getItens().size(), contar(c, "pedido_item"));
        }
    }

    @Test
    public void roundTripPreservaItensEValores() throws Exception {
        try (Connection c = abrir("roundtrip")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            Pedido p = gerar("Mercado A", "ana", "bia").get(1);

            repo.salvar(p);
            List<Pedido> lidos = repo.buscarPorComprador("bia");

            assertEquals(1, lidos.size());
            Pedido l = lidos.get(0);
            assertEquals(p.getId(), l.getId());
            assertEquals(p.getMercado(), l.getMercado());
            assertEquals(p.getProtocolo(), l.getProtocolo());
            assertEquals(p.getItens(), l.getItens());
            assertEquals(p.getValoresPorSku(), l.getValoresPorSku());
            assertEquals(p.getSubtotal(), l.getSubtotal());
            assertEquals(p.getFrete(), l.getFrete());
            assertEquals(p.getTotal(), l.getTotal());
            for (BigDecimal v : l.getValoresPorSku().values()) {
                assertEquals(2, v.scale());
            }
            assertEquals(2, l.getTotal().scale());
        }
    }

    @Test
    public void mapasLidosSaoImutaveis() throws Exception {
        try (Connection c = abrir("imutavel")) {
            PedidoRepositorioJdbc repo = new PedidoRepositorioJdbc(c);
            repo.salvar(gerar("a", "ana").get(0));

            Pedido l = repo.buscarPorComprador("ana").get(0);

            assertThrows(UnsupportedOperationException.class, () -> l.getItens().put("ZZZ-0001", 1));
            assertThrows(UnsupportedOperationException.class,
                    () -> l.getValoresPorSku().put("ZZZ-0001", BigDecimal.ONE));
        }
    }
}
