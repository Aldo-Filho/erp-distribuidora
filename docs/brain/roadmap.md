# Roadmap

| Frente | Estado | Dependência | Próximo resultado |
| --- | --- | --- | --- |
| Base de conhecimento enxuta | Concluída | — | Consultar somente o contexto necessário antes de cada alteração. |
| Auditoria de integridade | Em andamento; validação pendente | Base de conhecimento | Correções centrais e inventário em `contexto-do-sistema.md`; fechar Tarefa 2 com execução explicitamente autorizada de testes/lint e tratar ocorrências remanescentes. |
| Configuração e segredos | Revertida por solicitação | Auditoria | Configuração direta no `application.yaml`; externalização somente mediante nova solicitação. |
| API orientada a recursos | Implementação aplicada; validação pendente | Auditoria com validação pendente; avanço aprovado | Rotas plurais, eventos comerciais e paginação implementados; matriz em `contexto-do-sistema.md`. Validar execução quando solicitada. |
| Fluxos de compras e vendas | Implementação aplicada; validação pendente | Auditoria e contratos da API | Tarefa 4A com página contínua, inclusão explícita, disponibilidade antecipada, recebimento comercial e filtros. Encerramento de saldo parcial adiado; aceite dinâmico pendente. |
| Identidade e tema | Implementação aplicada; validação pendente | Configuração direta preservada | Tarefa 5: logo e seis paletas globais; tema pessoal persistido. Executar V4 e conferir upload/combinações visuais quando solicitado. |
| Financeiro | Tarefas 6/7 implementadas; validação pendente | Auditoria e API | Contas, baixas, estornos e projeções em `financial`; aplicar V5/validar quando solicitado. Telas em `frontend/src/pages/Finance`; aceite visual/dinâmico pendente. |
| Código de barras | Tarefa 8 implementada; validação pendente | Auditoria e API | Cadastro por SKU e leitor compartilhado em vendas/compras/estoque; V6 e aceite dinâmico pendentes. |
| Sugestões de compra | Tarefa 9 implementada; validação pendente | Financeiro e estoque auditado | Geração em `purchase/suggestion`, memória de cálculo e rascunhos por fornecedor/depósito; V7 e aceite dinâmico pendentes. |
| Dashboard | Implementado; API e testes específicos validados; aceite visual pendente | Financeiro e sugestões | SQL de datas corrigido; 5 testes HTTP/SQL passaram e V8/V9 validadas em base isolada. Compilação backend, testes/build frontend passaram. Aceite em navegador pendente; limites da suíte geral no plano ativo. |
| NF-e demonstrativa | Tarefa 11 implementada; validação pendente | Venda, financeiro e empresa | Emissão única/atômica no despacho, snapshots e consulta/impressão sem validade fiscal. V10 preparada; aplicação, compilação e aceite dinâmico/visual pendentes. |
| WhatsApp e integrações reais | Futuro | Todas as frentes anteriores | Avaliar quando o núcleo estiver consistente. |

## Regra de atualização

Atualize este arquivo apenas ao iniciar, concluir, bloquear ou repriorizar uma frente. Detalhes ficam no código ou no plano ativo, nunca em um novo relatório.
