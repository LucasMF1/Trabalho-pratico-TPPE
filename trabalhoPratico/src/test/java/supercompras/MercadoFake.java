package supercompras;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dublê de mercado para testar sem rede. */
class MercadoFake implements Mercado {

    String nome;
    Map<String, BigDecimal> precos = new LinkedHashMap<>();
    BigDecimal frete = BigDecimal.ZERO;
    String protocolo = "PROTO";
    MercadoIndisponivelException erroCompra;

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
        return nome;
    }

    @Override
    public Map<String, BigDecimal> cotarPrecosUnitarios(Map<String, Integer> itens) {
        return precos;
    }

    @Override
    public BigDecimal frete(Map<String, Integer> itens) {
        return frete;
    }

    @Override
    public String comprar(Map<String, Integer> itens, Map<String, BigDecimal> precosUnitarios)
            throws MercadoIndisponivelException {
        if (erroCompra != null) {
            throw erroCompra;
        }
        return protocolo;
    }
}
