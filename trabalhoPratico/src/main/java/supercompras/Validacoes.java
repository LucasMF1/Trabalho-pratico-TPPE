package supercompras;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Barricada de entrada. Valida identificador de comprador e lista de compras
 * antes de qualquer uso. Mensagens de erro nunca repetem o valor rejeitado.
 */
final class Validacoes {

    private static final Pattern COMPRADOR = Pattern.compile("[a-z0-9][a-z0-9_.-]{2,29}");
    private static final Pattern SKU = Pattern.compile("[A-Z]{3}-[0-9]{4}");
    private static final int MIN_QTD = 1;
    private static final int MAX_QTD = 999;
    private static final int MAX_ITENS = 20;

    private Validacoes() {
    }

    static boolean compradorValido(String compradorId) {
        return compradorId != null && COMPRADOR.matcher(compradorId).matches();
    }

    static void exigirComprador(String compradorId) {
        if (!compradorValido(compradorId)) {
            LogSeguro.aviso("identificador de comprador rejeitado");
            throw new EntradaInvalidaException("identificador de comprador invalido");
        }
    }

    /**
     * Copia a lista recebida (nao confiavel) e valida a copia. Retorna um mapa
     * imutavel com exatamente o que foi validado.
     */
    static Map<String, Integer> exigirLista(Map<String, Integer> itens) {
        if (itens == null) {
            rejeitarLista();
        }
        Map<String, Integer> copia = new LinkedHashMap<>(itens);
        if (copia.isEmpty() || copia.size() > MAX_ITENS) {
            rejeitarLista();
        }
        for (Map.Entry<String, Integer> e : copia.entrySet()) {
            String sku = e.getKey();
            Integer qtd = e.getValue();
            if (sku == null || !SKU.matcher(sku).matches()) {
                rejeitarLista();
            }
            if (qtd == null || qtd < MIN_QTD || qtd > MAX_QTD) {
                rejeitarLista();
            }
        }
        return Map.copyOf(copia);
    }

    private static void rejeitarLista() {
        LogSeguro.aviso("lista de compras rejeitada");
        throw new EntradaInvalidaException("lista de compras invalida");
    }
}
