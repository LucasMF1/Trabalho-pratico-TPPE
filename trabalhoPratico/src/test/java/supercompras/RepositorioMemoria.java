package supercompras;

import java.util.ArrayList;
import java.util.List;

/** Dublê de repositório em memória para testar sem banco. */
class RepositorioMemoria implements PedidoRepositorio {

    final List<Pedido> salvos = new ArrayList<>();

    @Override
    public void salvar(Pedido pedido) {
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
