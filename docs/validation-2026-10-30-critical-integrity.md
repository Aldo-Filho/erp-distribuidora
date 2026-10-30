# JamsHub — validação crítica de estoque, compras e vendas

Data: 30/10/2026. Fonte: checkout local atual, incluindo alterações pré-existentes. AGENTS.md consultado. Nenhuma alteração em código de produção nesta rodada.

## Resultado

**46 execuções aprovadas, nenhuma falha crítica de integridade reproduzida.**

- 39 execuções da nova `CriticalIntegrityIntegrationTests`, com HTTP real e PostgreSQL.
- 5 testes existentes de `ProductCatalogIntegrationTests`.
- 1 teste existente de inicialização, `ErpApplicationTests`.
- 1 teste existente de recebimento por outro operador: `AuthenticationAccessIntegrationTests#receiptReplayByAnotherAuthorizedOperatorPreservesHistoricalActor`.

A execução conjunta dos primeiros três grupos terminou com 45 testes aprovados. O teste de outro operador foi executado separadamente e aprovado.

## Ambiente e migrations

Instância exclusiva em `127.0.0.1:55435`, banco `jamshub_test`, arquivos em `backend/target/jamshub-critical-db`. Antes da aplicação das migrations, a consulta confirmou **zero tabelas no schema public**. A identidade do banco e a porta foram confirmadas antes das operações; a nova suíte também as verifica antes de cada cenário.

As duas migrations atualmente existentes, V1 e V2, foram aplicadas com sucesso em banco vazio. Flyway validou seus checksums nas execuções seguintes e Hibernate validou os mapeamentos com `ddl-auto=validate`. Não foram restauradas migrations antigas nem alteradas migrations atuais. O banco habitual `erp` e as credenciais do projeto não foram alterados. A instância de teste foi encerrada após a validação; os dados foram preservados.

## Cobertura aprovada

| Cenário | Evidência verificada |
| --- | --- |
| UUID e payload iguais, simultâneos | 8 requisições por rodada, 5 rodadas: um 201, sete 200, mesmo registro e um único efeito |
| UUID igual e payload divergente | 5 rodadas, inclusive estoques diferentes: um 201 e um 400; divergência no mesmo estoque também rejeitada |
| UUIDs diferentes no mesmo estoque | 8 saídas concorrentes por rodada, 5 rodadas: saldo insuficiente rejeitado, sem perda de atualização ou saldo negativo |
| Repetição após sucesso e timeout | 3 timeouts reais do cliente com requisição bloqueada no PostgreSQL; desbloqueio e repetição resultaram em um único movimento |
| Falha intermediária | 3 rodadas com falha de banco injetada em movimento manual, segundo item de recebimento e segundo item de despacho; rollback e nova tentativa conferidos |
| Recebimento parcial e final | 3 rodadas: saldos, contadores, PARTIALLY_RECEIVED/RECEIVED, repetição concorrente e posterior sem duplicação |
| Recebimentos diferentes concorrentes | 3 rodadas: excesso de recebimento rejeitado; quantidade recebida não ultrapassou a contratada |
| Reserva, despacho e repetição | 3 rodadas: físico, reservado, disponível, CONFIRMED/INVOICED e um movimento por reserva |
| Pedidos concorrentes | 3 rodadas disputando saldo insuficiente para ambos: somente uma reserva aprovada |
| Compra, venda e ajuste no mesmo estoque | 3 rodadas simultâneas: saldo final, histórico e status consistentes |
| Cancelamento | Venda confirmada libera reserva; compra confirmada cancelada não recebe; repetições sem efeito adicional |
| Quantidade inválida e saldo insuficiente | Zero, negativos, fração indevida em UN, precisão excessiva e excesso de saída/recebimento rejeitados; fração válida em KG aceita |
| Atomicidade da reserva | Segundo item sem saldo: primeiro item não fica reservado e pedido continua PENDING |
| Outro operador autorizado | Repetição de recebimento preserva registro e operador históricos, mesmo após inativação do operador original |

HTTPs conferidos: 201 na criação, 200 na repetição válida, 400 em violações de domínio e 409 na falha de constraint injetada para verificar rollback. Despacho usa o ID do pedido como identidade da operação, conforme implementação atual.

A conferência SQL final encontrou **zero violações em 12 verificações**: saldos, soma de reservas, contadores recebidos, UUIDs duplicados, cardinalidade dos movimentos, último saldo, aritmética/continuidade do histórico e consistência dos status. As quatro constraints de unicidade pertinentes estão presentes. Nenhum trigger de falha permaneceu no banco.

## Falhas encontradas e ajustes

- O teste existente de estoque inicial lia `/stockItem/search` como array. A API atual retorna página. Corrigida somente a leitura de `content` no teste; os cinco testes de catálogo passaram depois do ajuste.
- As proteções dos testes existentes passaram a aceitar também o endereço exato `jdbc:postgresql://127.0.0.1:55435/jamshub_test`, preservando a rejeição do banco habitual.
- Problemas do executor local: comunicação do Java com caminho do Windows e limite de conexões entre contextos de teste. Resolvidos no executor/dados de teste, sem mudanças na configuração habitual.
- **Nenhuma correção funcional ou refatoração foi necessária.**

Arquivos criados/alterados nesta rodada:

- `backend/src/test/java/com/pi/erp/CriticalIntegrityIntegrationTests.java` — nova suíte HTTP e verificações persistidas.
- `backend/src/test/java/com/pi/erp/product/ProductCatalogIntegrationTests.java` — proteção do banco e leitura da resposta atual.
- `backend/src/test/java/com/pi/erp/auth/AuthenticationAccessIntegrationTests.java` — proteção do banco.
- Este relatório.

Arquivos locais de apoio, ignorados pelo Git: `backend/target/run-critical-verification.ps1`, `critical-invariants.sql`, `critical-invariants.txt`, `critical-main-tests.log`, `critical-tests.log`, logs de PostgreSQL e relatórios Surefire. O executor configura a instância isolada e executa os quatro grupos acima; usa as ferramentas instaladas nesta máquina.

## Limites

Não executados: carga prolongada, múltiplas instâncias da aplicação, interrupção real de rede, encerramento abrupto do processo/PostgreSQL e atualização do banco habitual. O timeout foi simulado por bloqueio transacional com expiração real do cliente; não representa todos os tipos de falha de infraestrutura. Os demais testes de autenticação e o frontend não fazem parte desta validação. Sem commits ou deploy.
