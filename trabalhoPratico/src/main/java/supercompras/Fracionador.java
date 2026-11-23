package supercompras;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reparte o lote comprado entre os compradores (RN-07). Cada comprador recebe
 * exatamente o que pediu, ao preco do lote, sem criar nem perder centavos.
 */
final class Fracionador {

    private final Map<String, Integer> lote;
    private final Cotacao vencedora;
    private final Map<String, Map<String, Integer>> listas;

    Fracionador(Map<String, Integer> lote, Cotacao vencedora, Map<String, Map<String, Integer>> listas) {
        this.lote = lote;
        this.vencedora = vencedora;
        this.listas = listas;
    }

    List<Pedido> fracionar(String protocolo) {
        Map<String, Map<String, Long>> valores = new TreeMap<>();
        for (String comprador : listas.keySet()) {
            valores.put(comprador, new LinkedHashMap<>());
        }

        for (Map.Entry<String, Integer> item : lote.entrySet()) {
            String sku = item.getKey();
            BigDecimal preco = vencedora.precos().get(sku);
            long linha = Dinheiro.centavos(Dinheiro.arredondar(preco.multiply(BigDecimal.valueOf(item.getValue()))));

            Map<String, Long> pesos = new TreeMap<>();
            for (Map.Entry<String, Map<String, Integer>> lista : listas.entrySet()) {
                Integer qtd = lista.getValue().get(sku);
                if (qtd != null) {
                    pesos.put(lista.getKey(), (long) qtd);
                }
            }
            Map<String, Long> parte = Rateio.repartir(linha, pesos);
            for (Map.Entry<String, Long> e : parte.entrySet()) {
                valores.get(e.getKey()).put(sku, e.getValue());
            }
        }

        Map<String, Long> subtotais = new TreeMap<>();
        for (Map.Entry<String, Map<String, Long>> e : valores.entrySet()) {
            long soma = 0;
            for (long v : e.getValue().values()) {
                soma += v;
            }
            subtotais.put(e.getKey(), soma);
        }

        long freteTotal = Dinheiro.centavos(vencedora.frete());
        Map<String, Long> fretes = Rateio.repartir(freteTotal, subtotais);

        List<Pedido> pedidos = new ArrayList<>();
        for (String comprador : listas.keySet()) {
            Map<String, BigDecimal> valoresSku = new LinkedHashMap<>();
            for (Map.Entry<String, Long> e : valores.get(comprador).entrySet()) {
                valoresSku.put(e.getKey(), Dinheiro.deCentavos(e.getValue()));
            }
            long subtotalCent = subtotais.get(comprador);
            long freteCent = fretes.get(comprador);
            pedidos.add(new Pedido(comprador, vencedora.nome(), protocolo, listas.get(comprador), valoresSku,
                    Dinheiro.deCentavos(subtotalCent), Dinheiro.deCentavos(freteCent),
                    Dinheiro.deCentavos(subtotalCent + freteCent)));
        }
        return pedidos;
    }
}
