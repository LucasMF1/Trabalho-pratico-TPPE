package supercompras;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging centralizado do sistema. Evita injecao de log: remove quebras de
 * linha e nunca registra a entrada bruta do usuario.
 */
final class LogSeguro {

    private static final Logger LOG = Logger.getLogger("supercompras");

    private LogSeguro() {
    }

    static void aviso(String mensagem) {
        LOG.log(Level.WARNING, sanitizar(mensagem));
    }

    /** Remove caracteres de controle para a mensagem caber em uma unica linha. */
    static String sanitizar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            sb.append(Character.isISOControl(c) ? ' ' : c);
        }
        return sb.toString();
    }
}
