package supercompras;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mercado de teste configuravel. Permite simular precos, frete, protocolo e
 * falhas (excecoes checadas, de runtime ou Error) em qualquer chamada.
 */
class MercadoFake implements Mercado {

    String nome;
    Map<String, BigDecimal> precos = new LinkedHashMap<>();
    BigDecimal frete = BigDecimal.ZERO;
    String protocolo = "PROTO";

    Throwable erroNome;
    Throwable erroCotacao;
    Throwable erroFrete;
    Throwable erroCompra;

    int comprasChamadas = 0;
    int cotacoesChamadas = 0;
    Map<String, Integer> loteNaCompra;
    Map<String, BigDecimal> precosNaCompra;

    MercadoFake(String nome) {
        this.nome = nome;
    }

    MercadoFake preco(String sku, String valor) {
        precos.put(sku, new BigDecimal(valor));
        return this;
    }

    MercadoFake frete(String valor) {
        this.frete = new BigDecimal(valor);
        return this;
    }

    @Override
    public String nome() {
        if (erroNome instanceof RuntimeException r) {
            throw r;
        }
        if (erroNome instanceof Error e) {
            throw e;
        }
        return nome;
    }

    @Override
    public Map<String, BigDecimal> cotarPrecosUnitarios(Map<String, Integer> itens)
            throws MercadoIndisponivelException {
        cotacoesChamadas++;
        talvezLancar(erroCotacao);
        return precos;
    }

    @Override
    public BigDecimal frete(Map<String, Integer> itens) throws MercadoIndisponivelException {
        talvezLancar(erroFrete);
        return frete;
    }

    @Override
    public String comprar(Map<String, Integer> itens, Map<String, BigDecimal> precosUnitarios)
            throws MercadoIndisponivelException {
        comprasChamadas++;
        loteNaCompra = itens;
        precosNaCompra = precosUnitarios;
        talvezLancar(erroCompra);
        return protocolo;
    }

    private void talvezLancar(Throwable t) throws MercadoIndisponivelException {
        if (t == null) {
            return;
        }
        if (t instanceof MercadoIndisponivelException m) {
            throw m;
        }
        if (t instanceof RuntimeException r) {
            throw r;
        }
        if (t instanceof Error e) {
            throw e;
        }
    }
}
