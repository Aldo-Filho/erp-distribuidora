# Progresso — Registrar venda lado a lado

Plano: `docs/superpowers/plans/2026-10-31-order-entry.md`.

## Entregas de código

- Etapa 1: página integrada às rotas `/orders/new` e `/orders/:id/edit`; Nova venda e edição da listagem/consulta encaminham para a página. Retorno à listagem preserva filtros e página; retorno autenticado aceita os caminhos específicos.
- Etapa 2: catálogo independente, busca/paginação mantidas, variações expandidas na própria lista, quantidades por produto e cotações limitadas a quatro consultas auxiliares simultâneas. SKU simples resolvido é reaproveitado na seleção.
- Etapa 3: `TradeItemForm` renderiza embutido com as regras comerciais existentes. Inclusão rápida usa o mesmo submit validado e somente ocorre por uma seleção explícita nova, com apresentação/depósito definidos. Rascunhos retomados não disparam inclusão automática. Duplicidade permite somar quantidade ou editar a linha existente, preservando preço e desconto.
- Etapa 4: coordenador bloqueia gravações sobrepostas, conserva tentativas incertas em memória e permite conferir o resultado. Criação incerta possui consulta de candidatos para retomada explícita. Cabeçalho, exclusão, confirmação e cancelamento continuam nas APIs existentes.
- Etapa 5: catálogo e pedido em duas colunas conforme largura disponível; uma coluna em telas estreitas; consulta e despacho existentes mantidos. Cancelamento usa vermelho permanente, metadados e saldos/reservas permanecem acessíveis.

## Decisões de execução

- Executado no checkout compartilhado para preservar o trabalho acumulado deste chat, sem commits, troca de branch ou alteração das regras do backend.
- O editor ocupa um lugar estável na coluna do catálogo, mantendo o pedido visível na outra coluna. Isso evita outro modal e impede que uma busca/paginação desmonte um editor durante a gravação; o resultado selecionado fica destacado.
- O cabeçalho comercial existente foi reutilizado com um wrapper embutido. A variante modal e os contratos antigos continuam disponíveis.
- A largura responsiva é observada num contêiner apenas da área de trabalho; seletores auxiliares e consulta de estoque ficam fora da contenção, preservando seus modais.
- O subtotal de cada linha é uma prévia decimal calculada com inteiros; o total do pedido continua vindo da API.
- Sem migração de router, novos endpoints, bibliotecas ou armazenamento persistente de rascunhos.

## Verificação realizada e pendências

Revisão direta dos componentes, condições de permissão, payloads e diff, comparados com os formulários e contratos existentes. Formatação limitada aos arquivos envolvidos; o formatter conseguiu interpretar os JSX/JS alterados.

Conforme AGENTS.md, nenhum teste automatizado, build ou lint foi criado/executado. A conferência visual/funcional em navegador e a medição de desempenho descritas no plano não foram executadas. Os respectivos itens de validação permanecem pendentes; não são apresentados como aprovados.
