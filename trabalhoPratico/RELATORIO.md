# Relatório do grupo: Compras Coletivas de Supermercado

**Grupo:** _(definir)_
**Integrantes (8):** _(nome, matrícula, @usuário no Git)_

1. Vitor Valerio Hoffmann, _(matrícula)_, @_(usuário)_
2. _(integrante)_
3. _(integrante)_
4. _(integrante)_
5. _(integrante)_
6. _(integrante)_
7. _(integrante)_
8. _(integrante)_

> Este relatório é o roteiro da defesa oral. Os hashes citados são curtos; use `git show <hash>` para o commit completo.

## 1. Visão geral da arquitetura

**Antes.** Uma classe `SistemaCompras` concentrava tudo: agrupava listas, cotava, escolhia o mercado, fracionava e persistia. Usava `double` para dinheiro, campos públicos, estado estático (`contador`), `catch (Throwable)` com `printStackTrace`, e `PedidoRepositorioJdbc` montava SQL por concatenação. `Pedido` e `ResultadoRodada` eram bags mutáveis de setters.

**Depois.** Responsabilidades separadas, mantendo o contrato congelado:

- `Validacoes` e `LogSeguro`: barricada de entrada e logging sem injeção.
- `Dinheiro`: arredondamento HALF_EVEN com escala 2 e conversão para centavos.
- `Cotacao`: cópia imediata de preços e frete, com o total do mercado.
- `Rateio`: método do maior resto.
- `Fracionador`: divisão do lote entre compradores.
- `SistemaCompras`: orquestra cotação, fail-over de compra e persistência.
- `Pedido` e `ResultadoRodada`: imutáveis.
- `PedidoRepositorioJdbc`: consultas parametrizadas, transação e `try-with-resources`.

```
listas -> Validacoes -> SistemaCompras.agrupar -> lote imutavel
       -> Cotacao (N mercados) -> menor total / desempate -> comprar (fail-over)
       -> Fracionador (Rateio) -> pedidos imutaveis -> PedidoRepositorioJdbc
```

## 2. Code smells e refatorações

| # | Smell encontrado | Onde estava | Refatoração aplicada | Teste que protegeu | Commit |
|---|---|---|---|---|---|
| 1 | `double` para dinheiro | `SistemaCompras` (total, `Math.round`) | Substituir primitivo por `BigDecimal`/centavos (`Dinheiro`) | `FracionamentoTest`, `CotacaoMercadoTest` | 46e71f8 |
| 2 | Método longo | `fecharRodada` | Extrair classe (`Cotacao`, `Fracionador`, `Rateio`) | `FecharRodadaTest` | 46e71f8 |
| 3 | Campos públicos / estado exposto | `SistemaCompras`, `ResultadoRodada` | Encapsular campo, tornar imutável | `ImutabilidadeTest` | 46e71f8 |
| 4 | Estado estático | `static int contador` | Remover estado global | `ContratoApiTest` | 46e71f8 |
| 5 | Números e textos mágicos | `999999999`, `println` | Constantes e remoção | `FecharRodadaTest` | 46e71f8 |
| 6 | Comentário que mente | "frete a gente soma depois" | Remover; corrigir critério | `CotacaoMercadoTest` | 46e71f8 |
| 7 | `null` como sinal | `receberLista` retornava em `null` | Fail fast com exceção | `ValidacaoEntradaTest` | bcfa95b |
| 8 | Exposição de estrutura interna | `setItens`/`getItens` com referência | Cópia defensiva e imutabilidade | `ValidacaoEntradaTest`, `ImutabilidadeTest` | 46e71f8 |
| 9 | Injeção de SQL | `PedidoRepositorioJdbc` concatenava | Consulta parametrizada | `PersistenciaJdbcTest` | 46e71f8 |
| 10 | Captura larga / erro engolido | `catch (Throwable)`, `printStackTrace` | Capturar `RuntimeException`, deixar `Error` propagar | `CotacaoMercadoTest`, `ImutabilidadeTest` | 46e71f8 |
| 11 | Obsessão por primitivos sem validação | `compradorId` cru | Barricada de validação | `ValidacaoEntradaTest` | bcfa95b |
| 12 | Código duplicado | `Math.round(x*100)/100.0` repetido | Centralizar em `Dinheiro.arredondar` | `FracionamentoTest` | c6f606e |
| 13 | Recurso JDBC não fechado | `Statement`/`ResultSet` abertos | `try-with-resources` | `PersistenciaJdbcTest` | 46e71f8 |

## 3. TDD: regra por regra

| Regra | Teste(s) que a especificam | Commit vermelho (`test:`) | Commit verde (`feat:`/`fix:`) | Refatoração posterior |
|---|---|---|---|---|
| RN-01 | `ValidacaoEntradaTest` | 48da79d | bcfa95b | 46e71f8 |
| RN-02 | `ValidacaoEntradaTest` | 48da79d | bcfa95b | 46e71f8 |
| RN-03 | `ImutabilidadeTest` | 03a47f2 | 46e71f8 | - |
| RN-04 | `CotacaoMercadoTest` | 9de4867 | 1eb2356, 46e71f8 | - |
| RN-05 | `CotacaoMercadoTest` | 9de4867 | 46e71f8 | - |
| RN-06 | `CotacaoMercadoTest` | 9de4867 | 46e71f8 | - |
| RN-07 | `RateioTest`, `FracionamentoTest` | 4852ecb, b83572f | 9cad92b, 46e71f8 | - |
| RN-08 | `FecharRodadaTest` | 4852ecb | 46e71f8 | - |
| RN-09 | `FracionamentoTest` | b83572f | 46e71f8 | - |
| RN-10 | `FecharRodadaTest` | 4852ecb | 46e71f8 | - |
| RN-11 | `PersistenciaJdbcTest` | 0a7435d | 46e71f8 | - |
| RN-12 | `PersistenciaJdbcTest`, `ImutabilidadeTest` | 0a7435d, 03a47f2 | 46e71f8 | - |

> Observação honesta: a reescrita do núcleo foi consolidada no commit `refactor` 46e71f8 por causa do acoplamento do contrato congelado. Os testes de cada regra estão em commits `test:` próprios e cada integrante deve ampliar a cobertura da sua frente aplicando o ciclo vermelho-verde a partir daqui (ver seção 8).

## 4. Defesas: técnica, lugar, ataque e teste

| Técnica defensiva | Onde foi aplicada | Problema que evita | Teste que ataca a defesa | Custo / efeito colateral |
|---|---|---|---|---|
| Barricada | `Validacoes` em `receberLista` | entrada hostil propagando no sistema | `ValidacaoEntradaTest` | rejeita na fronteira; custo por chamada |
| Fail fast / pré-condição | `Objects.requireNonNull` no construtor; `IllegalStateException` sem listas | NPE tardio; rodada vazia | `FecharRodadaTest.fecharSemListasLancaIllegalState` | falha cedo em vez de estado inválido |
| Asserção | invariante soma dos totais == `totalLote` em `fecharRodada` | erro de rateio passar silencioso | roda com `-ea` em toda a suíte | só ativa com `-ea` |
| Exceção com contexto | `PersistenciaException` com protocolo e mercado | perder a compra já confirmada | `FecharRodadaTest.falhaDePersistencia...` | mensagem precisa sanitizar dado externo |
| Cópia defensiva / imutabilidade | `Map.copyOf` em lista, lote, preços e `Pedido` | mutação externa ou em outra thread | `ImutabilidadeTest`, `ValidacaoEntradaTest` | cópia extra na fronteira |
| `try-with-resources` | `PedidoRepositorioJdbc` | vazamento de `Statement`/`ResultSet` | `PersistenciaJdbcTest` | - |
| Consulta parametrizada | todo o `PedidoRepositorioJdbc` | injeção de SQL | `PersistenciaJdbcTest` (nome hostil, injeção na busca) | - |
| Log seguro | `LogSeguro.sanitizar` e mensagens genéricas | log injection; vazar entrada bruta | _(a ampliar: ver seção 8, integrante 7)_ | perde parte do contexto no log |

## 5. Onde NÃO aplicamos defesa (e por quê)

- **Casas decimais dos preços do mercado.** RN-04 só exige preço não nulo e ≥ 0,01; validar o número de casas seria excesso e contraria o enunciado ("qualquer número de casas decimais").
- **`Error` nunca é capturado.** Problema de JVM não é "mercado ruim"; capturar mascararia falhas graves. Deixamos propagar de propósito.
- **Sem sincronização de `listas`.** As RN não especificam `receberLista` como concorrente. Fazemos cópia defensiva do mapa recebido, mas não pagamos o custo de locks que o contrato não pede.
- **Não recopiamos o `Pedido` ao salvar.** Ele já é imutável; cópia adicional seria redundante.

## 6. Decisões difíceis e divergências do grupo

- **`buscarPorComprador` com id inválido.** O enunciado permite lista vazia ou exceção. Escolhemos **lista vazia**: a busca é leitura, e devolver vazio é mais previsível para o chamador do que lançar. _(registrar aqui a divergência do grupo)_
- **Consolidar a reescrita em um `refactor`.** Discutimos fatiar o núcleo em vários commits verdes, mas o contrato congelado acopla `Pedido`, `ResultadoRodada`, `SistemaCompras` e `PedidoRepositorioJdbc`; fatiar deixaria commits que não compilam. Optamos por utilitários isolados antes e o núcleo num commit coeso. _(registrar aqui a divergência do grupo)_

## 7. Limitações conhecidas

- `SistemaCompras` não é thread-safe.
- `id` do pedido limitado a `VARCHAR(200)`; protocolos muito longos não são tratados.
- Não há teste de timeout nem de laço infinito.
- Falta a frente de logging com handler anexado ao logger raiz (seção 8, integrante 7).

## 8. Quem fez o quê

| Integrante | Principais contribuições (commits, testes, partes do código) |
|---|---|
| Vitor Valerio Hoffmann | Estrutura inicial do projeto e suíte base de testes. |
| Integrante 2 | Ampliar testes de ataque de RN-01/RN-02; caracterização do tratamento de entrada do legado (commit original 772e660). |
| Integrante 3 | Casos extras de arredondamento HALF_EVEN e mercados hostis (RN-04/RN-05); revisar e documentar os smells. |
| Integrante 4 | Testes de fail-over e encerramento (RN-06/RN-08/RN-10), incluindo concorrência em `receberLista`. |
| Integrante 5 | Testes por invariantes do fracionamento (RN-07/RN-09) com listas aleatórias. |
| Integrante 6 | Estresse da persistência (RN-11/RN-12): falha no meio do lote, vazamento de recurso, corpus de injeção de SQL. |
| Integrante 7 | Frente de logging: anexar handler ao logger raiz e testar log injection, linha única e ausência da entrada bruta; preencher a defesa de log (seção 4). |
| Integrante 8 | Seções 1, 6 e 7 do relatório, polimento do README, verificação do build no Windows e preparação da defesa. |
