# Compras Coletivas de Supermercado

Trabalho prático de Técnicas de Programação para Plataformas Emergentes (FGA-0242 / FCTE - UnB).

O ponto de partida é um sistema legado de compras coletivas que errava contas, aceitava entrada inválida, quebrava com mercados que falham e era vulnerável a injeção de SQL. O objetivo é transformá-lo em um sistema correto, testado e defensivo, sem alterar a API pública congelada descrita no [enunciado](trabalhoPratico/README.md).

## Sobre o sistema

Vários compradores enviam listas de compras. Ao fechar uma rodada, o sistema agrupa as listas em um lote, cota o lote em vários mercados externos, escolhe o de menor preço total, compra com fail-over, fraciona os itens entre os compradores e persiste os pedidos em banco (H2 em memória).

```
listas -> agrupa -> lote -> cotacao (N mercados) -> vencedor -> compra
                                                                 |
pedidos persistidos <- fraciona entre os compradores <-----------+
```

Os mercados são sistemas externos e não confiáveis: podem falhar, demorar, devolver lixo ou tentar alterar os dados recebidos.

## Arquitetura

Código em `trabalhoPratico/src/main/java/supercompras`.

| Classe | Responsabilidade |
|---|---|
| `SistemaCompras` | Orquestra a rodada: validação, agrupamento, cotação, compra com fail-over e persistência |
| `Validacoes` | Barricada de entrada do identificador de comprador (RN-01) e da lista (RN-02) |
| `LogSeguro` | Logging por `java.util.logging`, sem injeção de log e sem entrada bruta |
| `Dinheiro` | Arredondamento HALF_EVEN com escala 2 e conversão para centavos |
| `Cotacao` | Cotação válida de um mercado, com cópia imediata de preços e frete |
| `Rateio` | Método do maior resto (RN-07) |
| `Fracionador` | Divisão do lote comprado entre os compradores |
| `Pedido`, `ResultadoRodada` | Tipos de resultado imutáveis |
| `PedidoRepositorioJdbc` | Persistência com consultas parametrizadas, transação e `try-with-resources` |

As interfaces (`Mercado`, `PedidoRepositorio`) e as exceções fazem parte do contrato congelado e não foram alteradas.

## Regras de negócio

As regras RN-01 a RN-12 estão no [enunciado](trabalhoPratico/README.md#6-regras-de-negócio). Em resumo: validação de comprador e lista, lote imutável, cotação de mercados não confiáveis, total com arredondamento linha a linha, desempate por nome, fracionamento pelo maior resto, compra com fail-over, formato monetário com escala 2, encerramento da rodada, segurança de SQL e logs, e imutabilidade dos objetos devolvidos.

## Como compilar e testar

Requisitos: JDK 17.

1. Baixe os três `.jar` listados em [`trabalhoPratico/libs/LEIAME.txt`](trabalhoPratico/libs/LEIAME.txt) e coloque-os em `trabalhoPratico/libs/` (não são versionados).

2. Em Linux ou macOS:

```bash
cd trabalhoPratico
./build.sh
./test.sh
```

`test.sh` compila e roda todas as classes `*Test` com asserções habilitadas (`-ea`). No Windows nativo, use os comandos manuais do enunciado (troque `:` por `;` no classpath).

## Estrutura do repositório

```
trabalhoPratico/
  src/main/java/supercompras/   codigo (contrato + classes do grupo)
  src/test/java/supercompras/   testes (inclui ContratoApiTest)
  libs/LEIAME.txt               bibliotecas permitidas (jars nao versionados)
  build.sh, test.sh             scripts
  RELATORIO.md                  relatorio da defesa
  README.md                     enunciado
```

## Testes

| Suíte | Cobre |
|---|---|
| `ContratoApiTest` | API pública congelada (não alterar) |
| `ValidacaoEntradaTest` | RN-01, RN-02 |
| `CotacaoMercadoTest` | RN-04, RN-05, RN-06 |
| `RateioTest`, `FracionamentoTest` | RN-07, RN-09 |
| `FecharRodadaTest` | RN-05 a RN-10 |
| `ImutabilidadeTest` | RN-03, RN-12 |
| `PersistenciaJdbcTest` | RN-11, RN-12 |
| `RegressaoLegadoTest` | defeitos do legado corrigidos (ver [SMELLS.md](trabalhoPratico/SMELLS.md)) |
| `MercadoFake`, `RepositorioMemoria` | dublês usados pela suíte |

## Processo

Commits em Conventional Commits: `test:` (teste novo), `feat:`/`fix:` (comportamento), `refactor:` (sem mudar comportamento), `docs:`. Cada regra RN tem um commit `test:` próprio. Rode o `ContratoApiTest` a cada commit.

## Divisão de tarefas

A base (núcleo correto e defensivo, suíte inicial e documentação) já está implementada. Cada integrante amplia uma frente, garantindo participação em todas as áreas avaliadas na defesa.

| Integrante | Tarefa |
|---|---|
| Vitor Valerio Hoffmann | Estrutura inicial do projeto e suíte base de testes |
| Integrante 2 | Ampliar testes de ataque de RN-01/RN-02 e caracterizar o tratamento de entrada do legado |
| Integrante 3 | Casos extras de arredondamento HALF_EVEN e mercados hostis (RN-04/RN-05); documentar os smells |
| Integrante 4 | Testes de fail-over, encerramento e concorrência (RN-06/RN-08/RN-10) |
| Integrante 5 | Testes por invariantes do fracionamento com listas aleatórias (RN-07/RN-09) |
| Integrante 6 | Estresse da persistência: falha no meio do lote, vazamento de recurso, injeção de SQL (RN-11/RN-12) |
| Integrante 7 | Frente de logging: handler no logger raiz, log injection, linha única, ausência de entrada bruta |
| Integrante 8 | Seções 1, 6 e 7 do relatório, polimento do README, verificação no Windows e preparação da defesa |

Detalhes e evidências (hashes, testes, smells) no [RELATORIO.md](trabalhoPratico/RELATORIO.md).
