package supercompras;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio de pedidos sobre JDBC. Nao e dono da conexao: nunca a fecha.
 * Todo acesso usa consultas parametrizadas e fecha os recursos em qualquer
 * caminho.
 */
public final class PedidoRepositorioJdbc implements PedidoRepositorio {

    private final Connection conexao;

    public PedidoRepositorioJdbc(Connection conexao) {
        this.conexao = conexao;
        criarEsquema();
    }

    private void criarEsquema() {
        try (Statement s = conexao.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS pedido ("
                    + "id VARCHAR(200) PRIMARY KEY, comprador_id VARCHAR(100), mercado VARCHAR(200), "
                    + "protocolo VARCHAR(200), subtotal DECIMAL(19,2), frete DECIMAL(19,2), total DECIMAL(19,2))");
            s.execute("CREATE TABLE IF NOT EXISTS pedido_item ("
                    + "pedido_id VARCHAR(200), sku VARCHAR(20), quantidade INT, valor DECIMAL(19,2))");
        } catch (SQLException e) {
            throw new PersistenciaException("falha ao criar o esquema", e);
        }
    }

    @Override
    public void salvar(Pedido pedido) {
        boolean autocommit;
        try {
            autocommit = conexao.getAutoCommit();
        } catch (SQLException e) {
            throw new PersistenciaException("falha ao consultar o estado da conexao", e);
        }
        try {
            conexao.setAutoCommit(false);
            inserirPedido(pedido);
            inserirItens(pedido);
            conexao.commit();
        } catch (SQLException e) {
            desfazer();
            throw new PersistenciaException("falha ao salvar o pedido", e);
        } finally {
            restaurarAutocommit(autocommit);
        }
    }

    private void inserirPedido(Pedido p) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO pedido (id, comprador_id, mercado, protocolo, subtotal, frete, total) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getCompradorId());
            ps.setString(3, p.getMercado());
            ps.setString(4, p.getProtocolo());
            ps.setBigDecimal(5, p.getSubtotal());
            ps.setBigDecimal(6, p.getFrete());
            ps.setBigDecimal(7, p.getTotal());
            ps.executeUpdate();
        }
    }

    private void inserirItens(Pedido p) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO pedido_item (pedido_id, sku, quantidade, valor) VALUES (?, ?, ?, ?)")) {
            for (Map.Entry<String, Integer> item : p.getItens().entrySet()) {
                ps.setString(1, p.getId());
                ps.setString(2, item.getKey());
                ps.setInt(3, item.getValue());
                ps.setBigDecimal(4, p.getValoresPorSku().get(item.getKey()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public List<Pedido> buscarPorComprador(String compradorId) {
        if (!Validacoes.compradorValido(compradorId)) {
            return List.of();
        }
        List<Pedido> pedidos = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(
                "SELECT id, comprador_id, mercado, protocolo, subtotal, frete, total "
                        + "FROM pedido WHERE comprador_id = ?")) {
            ps.setString(1, compradorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(montarPedido(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("falha ao buscar pedidos", e);
        }
        return pedidos;
    }

    private Pedido montarPedido(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String compradorId = rs.getString("comprador_id");
        String mercado = rs.getString("mercado");
        String protocolo = rs.getString("protocolo");
        BigDecimal subtotal = rs.getBigDecimal("subtotal");
        BigDecimal frete = rs.getBigDecimal("frete");
        BigDecimal total = rs.getBigDecimal("total");

        Map<String, Integer> itens = new LinkedHashMap<>();
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        try (PreparedStatement ps = conexao.prepareStatement(
                "SELECT sku, quantidade, valor FROM pedido_item WHERE pedido_id = ?")) {
            ps.setString(1, id);
            try (ResultSet ri = ps.executeQuery()) {
                while (ri.next()) {
                    itens.put(ri.getString("sku"), ri.getInt("quantidade"));
                    valores.put(ri.getString("sku"), ri.getBigDecimal("valor"));
                }
            }
        }
        return new Pedido(compradorId, mercado, protocolo, itens, valores, subtotal, frete, total);
    }

    private void desfazer() {
        try {
            conexao.rollback();
        } catch (SQLException e) {
            throw new PersistenciaException("falha no rollback", e);
        }
    }

    private void restaurarAutocommit(boolean autocommit) {
        try {
            conexao.setAutoCommit(autocommit);
        } catch (SQLException e) {
            throw new PersistenciaException("falha ao restaurar o autocommit", e);
        }
    }
}
