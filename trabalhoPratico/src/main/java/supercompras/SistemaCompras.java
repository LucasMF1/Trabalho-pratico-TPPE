package supercompras;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Compras coletivas de supermercado. Agrupa as listas, cota em varios mercados
 * nao confiaveis, compra no mais barato com fail-over, fraciona entre os
 * compradores e persiste os pedidos.
 */
public final class SistemaCompras {

    private static final BigDecimal PRECO_MINIMO = new BigDecimal("0.01");

    private final List<Mercado> mercados;
    private final PedidoRepositorio repositorio;
    private final Map<String, Map<String, Integer>> listas = new TreeMap<>();

    public SistemaCompras(List<Mercado> mercados, PedidoRepositorio repositorio) {
        this.mercados = List.copyOf(Objects.requireNonNull(mercados, "mercados"));
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    public void receberLista(String compradorId, Map<String, Integer> itens) {
        Validacoes.exigirComprador(compradorId);
        Map<String, Integer> lista = Validacoes.exigirLista(itens);
        if (listas.containsKey(compradorId)) {
            LogSeguro.aviso("segunda lista do mesmo comprador na rodada rejeitada");
            throw new EntradaInvalidaException("comprador ja enviou lista nesta rodada");
        }
        listas.put(compradorId, lista);
    }

    public ResultadoRodada fecharRodada() {
        if (listas.isEmpty()) {
            throw new IllegalStateException("nenhuma lista pendente");
        }
        Map<String, Integer> lote = agrupar();

        List<Cotacao> cotacoes = cotar(lote);
        if (cotacoes.isEmpty()) {
            throw new SemCotacaoValidaException("nenhum mercado apresentou cotacao valida");
        }
        cotacoes.sort(Comparator.comparing(Cotacao::total).thenComparing(Cotacao::nome));

        String protocolo = null;
        Cotacao vencedora = null;
        for (Cotacao c : cotacoes) {
            String proto = tentarComprar(c, lote);
            if (proto != null) {
                protocolo = proto;
                vencedora = c;
                break;
            }
        }
        if (vencedora == null) {
            throw new CompraNaoRealizadaException("nenhum mercado aceitou a compra");
        }

        List<Pedido> pedidos = new Fracionador(lote, vencedora, listas).fracionar(protocolo);
        assert somaDosTotais(pedidos).compareTo(vencedora.total()) == 0 : "fracionamento nao bate com o total do lote";

        listas.clear();
        persistir(pedidos, protocolo, vencedora.nome());
        return new ResultadoRodada(vencedora.nome(), protocolo, vencedora.total(), pedidos);
    }

    private Map<String, Integer> agrupar() {
        Map<String, Integer> lote = new TreeMap<>();
        for (Map<String, Integer> lista : listas.values()) {
            for (Map.Entry<String, Integer> item : lista.entrySet()) {
                lote.merge(item.getKey(), item.getValue(), Integer::sum);
            }
        }
        return Map.copyOf(lote);
    }

    private List<Cotacao> cotar(Map<String, Integer> lote) {
        List<Cotacao> cotacoes = new ArrayList<>();
        for (Mercado m : mercados) {
            Cotacao c = cotarMercado(m, lote);
            if (c != null) {
                cotacoes.add(c);
            }
        }
        return cotacoes;
    }

    private Cotacao cotarMercado(Mercado m, Map<String, Integer> lote) {
        String nome;
        try {
            nome = m.nome();
        } catch (RuntimeException e) {
            LogSeguro.aviso("mercado desqualificado: falha ao obter o nome");
            return null;
        }
        if (nome == null || nome.isBlank()) {
            LogSeguro.aviso("mercado desqualificado: nome ausente");
            return null;
        }

        Map<String, BigDecimal> cotados;
        try {
            cotados = m.cotarPrecosUnitarios(lote);
        } catch (MercadoIndisponivelException | RuntimeException e) {
            LogSeguro.aviso("mercado desqualificado na cotacao");
            return null;
        }
        if (cotados == null) {
            LogSeguro.aviso("mercado desqualificado: cotacao nula");
            return null;
        }

        Map<String, BigDecimal> precos = new LinkedHashMap<>();
        for (String sku : lote.keySet()) {
            BigDecimal p = cotados.get(sku);
            if (p == null || p.compareTo(PRECO_MINIMO) < 0) {
                LogSeguro.aviso("mercado desqualificado: preco invalido");
                return null;
            }
            precos.put(sku, p);
        }

        BigDecimal frete;
        try {
            frete = m.frete(lote);
        } catch (MercadoIndisponivelException | RuntimeException e) {
            LogSeguro.aviso("mercado desqualificado no frete");
            return null;
        }
        if (frete == null || frete.signum() < 0) {
            LogSeguro.aviso("mercado desqualificado: frete invalido");
            return null;
        }

        return new Cotacao(m, nome, precos, Dinheiro.arredondar(frete), total(lote, precos, frete));
    }

    private BigDecimal total(Map<String, Integer> lote, Map<String, BigDecimal> precos, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> item : lote.entrySet()) {
            BigDecimal linha = precos.get(item.getKey()).multiply(BigDecimal.valueOf(item.getValue()));
            total = total.add(Dinheiro.arredondar(linha));
        }
        return Dinheiro.arredondar(total.add(Dinheiro.arredondar(frete)));
    }

    private String tentarComprar(Cotacao c, Map<String, Integer> lote) {
        String proto;
        try {
            proto = c.mercado().comprar(lote, c.precos());
        } catch (MercadoIndisponivelException | RuntimeException e) {
            LogSeguro.aviso("compra recusada pelo mercado");
            return null;
        }
        if (proto == null || proto.isBlank()) {
            LogSeguro.aviso("compra sem protocolo valido");
            return null;
        }
        return proto;
    }

    private void persistir(List<Pedido> pedidos, String protocolo, String mercado) {
        List<Throwable> falhas = new ArrayList<>();
        for (Pedido p : pedidos) {
            try {
                repositorio.salvar(p);
            } catch (RuntimeException e) {
                falhas.add(e);
            }
        }
        if (falhas.isEmpty()) {
            return;
        }
        PersistenciaException ex = new PersistenciaException(
                "falha ao persistir pedidos protocolo=" + LogSeguro.sanitizar(protocolo)
                        + " mercado=" + LogSeguro.sanitizar(mercado),
                falhas.get(0));
        for (int i = 1; i < falhas.size(); i++) {
            ex.addSuppressed(falhas.get(i));
        }
        throw ex;
    }

    private static BigDecimal somaDosTotais(List<Pedido> pedidos) {
        BigDecimal soma = BigDecimal.ZERO.setScale(Dinheiro.ESCALA);
        for (Pedido p : pedidos) {
            soma = soma.add(p.getTotal());
        }
        return soma;
    }
}
