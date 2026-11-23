package supercompras;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Operacoes monetarias. Todo valor exposto tem escala 2 e usa HALF_EVEN. */
final class Dinheiro {

    static final int ESCALA = 2;

    private Dinheiro() {
    }

    static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_EVEN);
    }

    /** Converte um valor ja arredondado em centavos. */
    static long centavos(BigDecimal valorEscala2) {
        return valorEscala2.movePointRight(ESCALA).longValueExact();
    }

    static BigDecimal deCentavos(long centavos) {
        return BigDecimal.valueOf(centavos).movePointLeft(ESCALA);
    }
}
