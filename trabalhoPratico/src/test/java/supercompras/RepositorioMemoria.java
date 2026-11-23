package supercompras;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Repositorio em memoria para testes. Pode simular falha ao salvar. */
class RepositorioMemoria implements PedidoRepositorio {

    final List<Pedido> salvos = new ArrayList<>();
    final Set<String> compradoresQueFalham = new HashSet<>();

    @Override
    public void salvar(Pedido pedido) {
        if (compradoresQueFalham.contains(pedido.getCompradorId())) {
            throw new PersistenciaException("falha simulada", new RuntimeException("io"));
        }
        salvos.add(pedido);
    }

    @Override
    public List<Pedido> buscarPorComprador(String compradorId) {
        List<Pedido> r = new ArrayList<>();
        for (Pedido p : salvos) {
            if (p.getCompradorId().equals(compradorId)) {
                r.add(p);
            }
        }
        return r;
    }
}
