package supercompras;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

/** RN-07: metodo do maior resto. */
public class RateioTest {

    @Test
    public void exemploDoApendice() {
        Map<String, Long> pesos = new TreeMap<>();
        pesos.put("ana", 1L);
        pesos.put("bia", 2L);
        pesos.put("caio", 4L);

        Map<String, Long> r = Rateio.repartir(1000, pesos);

        assertEquals(143L, (long) r.get("ana"));
        assertEquals(286L, (long) r.get("bia"));
        assertEquals(571L, (long) r.get("caio"));
    }

    @Test
    public void somaPreservaOTotal() {
        Map<String, Long> pesos = new TreeMap<>();
        pesos.put("ana", 3L);
        pesos.put("bia", 3L);
        pesos.put("caio", 1L);

        Map<String, Long> r = Rateio.repartir(1000, pesos);

        long soma = r.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(1000L, soma);
    }

    @Test
    public void empateDeRestoVaiParaMenorIdentificador() {
        Map<String, Long> pesos = new LinkedHashMap<>();
        pesos.put("ana", 1L);
        pesos.put("bia", 1L);

        Map<String, Long> r = Rateio.repartir(1, pesos);

        assertEquals(1L, (long) r.get("ana"));
        assertEquals(0L, (long) r.get("bia"));
    }
}
