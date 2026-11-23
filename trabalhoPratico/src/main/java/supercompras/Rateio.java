package supercompras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metodo do maior resto. Reparte um total em centavos entre participantes de
 * acordo com pesos, sem criar nem perder centavos.
 */
final class Rateio {

    private Rateio() {
    }

    /**
     * @param total   centavos a repartir
     * @param pesos   participante -> peso (iteracao ja em ordem de desempate)
     * @return participante -> centavos atribuidos
     */
    static Map<String, Long> repartir(long total, Map<String, Long> pesos) {
        long somaPesos = 0;
        for (long p : pesos.values()) {
            somaPesos += p;
        }

        Map<String, Long> resultado = new LinkedHashMap<>();
        List<Fracao> fracoes = new ArrayList<>();
        long distribuido = 0;
        for (Map.Entry<String, Long> e : pesos.entrySet()) {
            long numerador = total * e.getValue();
            long parte = numerador / somaPesos;
            long resto = numerador % somaPesos;
            resultado.put(e.getKey(), parte);
            distribuido += parte;
            fracoes.add(new Fracao(e.getKey(), resto));
        }

        long sobra = total - distribuido;
        fracoes.sort(Comparator.comparingLong((Fracao f) -> f.resto).reversed());
        for (int i = 0; i < sobra; i++) {
            String id = fracoes.get(i).id;
            resultado.put(id, resultado.get(id) + 1);
        }
        return resultado;
    }

    private static final class Fracao {
        final String id;
        final long resto;

        Fracao(String id, long resto) {
            this.id = id;
            this.resto = resto;
        }
    }
}
