# Code smells do sistema legado

Levantados a partir do commit legado `772e660`. Os defeitos de comportamento estão cobertos por `RegressaoLegadoTest`, que fixa a correção na entrega.

| # | Smell | Onde | Evidência |
|---|---|---|---|
| 1 | `double` para dinheiro | `SistemaCompras.fecharRodada`, `Math.round` | erro de ponto flutuante e escala inconsistente |
| 2 | Método longo | `fecharRodada` faz tudo | cotação, escolha, compra, fracionamento e persistência juntos |
| 3 | Campos públicos / estado exposto | `SistemaCompras`, `ResultadoRodada`, `Pedido` | `mercados`, `repo`, `listas` e campos de resultado públicos |
| 4 | Estado estático | `static int contador` | estado global sem uso real |
| 5 | Números e textos mágicos | `999999999`, `System.out.println` | limites e logs chumbados no código |
| 6 | Comentário que mente | "frete a gente soma depois" | o frete nunca entra no critério de escolha |
| 7 | `null` como sinal | `receberLista` retorna em `null` | entrada inválida ignorada em silêncio |
| 8 | Exposição de estrutura interna | `setItens`/`getItens` guardam e devolvem a referência | mutável por quem chama |
| 9 | Injeção de SQL | `PedidoRepositorioJdbc` concatena strings | `INSERT ... VALUES ('" + p.getId() + "'...` |
| 10 | Captura larga / erro engolido | `catch (Throwable)`, `e.printStackTrace()` | esconde falhas e não há fail-over na compra |
| 11 | Obsessão por primitivos | `compradorId` e SKU sem tipo nem validação | aceita qualquer texto |
| 12 | Código duplicado | `Math.round(x*100)/100.0` repetido | arredondamento espalhado |
| 13 | Recurso JDBC não fechado | `Statement`/`ResultSet` abertos | vazamento em qualquer caminho |

Defeitos de comportamento corrigidos (ver `RegressaoLegadoTest`): aceitava comprador e lista inválidos, não fazia fail-over (deixava o protocolo nulo) e dividia o frete igualmente em vez de proporcional ao subtotal.
