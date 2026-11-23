# Code smells do sistema legado

Registro da Frente A, feito a partir da leitura do codigo em `772e660` e dos testes de caracterizacao em `CaracterizacaoLegadoTest`.

| # | Smell | Onde |
|---|---|---|
| 1 | `double` para dinheiro | `SistemaCompras.fecharRodada` com `Math.round` |
| 2 | Metodo longo | `fecharRodada` faz cotacao, escolha, compra, fracionamento e persistencia |
| 3 | Campos publicos / estado exposto | `SistemaCompras`, `ResultadoRodada` e `Pedido` |
| 4 | Estado estatico | `static int contador` em `SistemaCompras` |
| 5 | Numeros e textos magicos | `999999999` e `System.out.println` |
| 6 | Comentario que mente | "frete a gente soma depois", que nunca acontece |
| 7 | `null` como sinal | `receberLista` retorna quando a lista e nula |
| 8 | Exposicao de estrutura interna | `setItens`/`getItens` guardam e devolvem a mesma referencia |
| 9 | Injecao de SQL | `PedidoRepositorioJdbc` concatena valores na query |
| 10 | Captura larga e erro engolido | `catch (Throwable)` e `e.printStackTrace()` |
| 11 | Obsessao por primitivos | `compradorId` e SKU como `String` sem validacao |
| 12 | Codigo duplicado | arredondamento `Math.round(x*100)/100.0` repetido |
| 13 | Recurso JDBC nao fechado | `Statement` e `ResultSet` ficam abertos |

Defeitos de comportamento confirmados pela caracterizacao: aceita comprador fora do formato, ignora lista nula em silencio, divide o frete igualmente, escolhe o mercado ignorando o frete e nao faz fail-over (deixa o protocolo nulo).
