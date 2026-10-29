# Progresso — Clientes

Plano: `docs/superpowers/plans/2026-10-29-customers.md`.

Execução autorizada em 29/10/2026 no checkout atual. Git inicial: especificação de metadados modificada e plano/especificação de Clientes não rastreados. Esses documentos foram preservados.

Decisões de execução: checkout atual sem worktree, commits, subagentes, testes, build, lint ou SQL, conforme autorização. Revisão final direta. Registro preservado porque não haverá commits como histórico da execução. Formatação limitada aos arquivos novos do frontend; nenhum formatador global.

Interfaces conferidas: tarefas 1/2 produzem PATCH e Page consumidos por 4/5/6; tarefa 3 produz Modal/RecordMetadata para 5/6; tarefa 4 produz campos/serviços para 5/6. Sem conflito com as especificações aprovadas.

- Tarefa 1: completa por revisão direta do fluxo inicial e separado. removeBirthDate validado antes dos setters; complemento normalizado no service compartilhado; V6 criada, não aplicada.
- Tarefa 2: completa por revisão direta. Page no banco, limites/ordenação, AND de filtros e OR agrupado, curingas escapados. priceTable permanece ManyToOne EAGER; suas coleções e endereços são ignorados no JSON. Sem fetch de coleção na paginação.
- Tarefa 3: completa por revisão direta. Modal compartilhado, metadados, detalhes independentes de Funcionários e criação em Cargos.
- Tarefa 4: completa por revisão direta. Serviços ERP existentes, CEP público com timeout/cancelamento/revisão, campos de endereço compartilhados. Nenhuma chamada ViaCEP executada.
- Tarefa 5: completa por revisão direta. Formulário PF/PJ, PATCH explícito, preço opcional, rascunho em memória por usuário/registro e descarte confirmado.
- Tarefa 6: completa por revisão direta. Rota/menu, lista/filtros/paginação, detalhes, endereços e situação; nenhum DELETE de cliente na tela.
- Tarefa 7: completa por revisão direta. READMEs/Insomnia atualizados, roteiro manual preparado, diff rastreado e arquivos novos revisados. Verificação em execução continua pendente por decisão explícita do usuário.

Revisão final: corrigidos cancelamento de CEP ao iniciar gravação, datas de calendário inválidas, opção de preço indisponível em rascunho e convivência do foco com os avisos de sessão. Nenhum cenário foi executado. Sem mudanças materiais no desenho aprovado.

## Consumidores e preservação

Coleção Insomnia atualizada para Page, parâmetros de busca/paginação, removeBirthDate e complemento nullable, preservando o fluxo de sessão/CSRF e as demais requisições.

Consumidor legado: `backend/tools/backend-audit.cjs` (ignorado no Git) foi adaptado após solicitação do usuário. checkList consome Page somente em /customer/search, valida o envelope e percorre páginas de até 100 registros mantendo os filtros, até encontrar o ID ou registrar falha na última página/no limite da API. Resposta inválida ou HTTP inesperado interrompe a busca com falha registrada; os demais endpoints continuam exigindo arrays. Revisão direta, sem executar o script ou criar novos cenários automatizados. Esta adaptação não valida as demais compatibilidades do executor antigo com autenticação/CSRF atuais. Não foram encontrados outros consumidores ativos da busca fora do novo serviço e da coleção. A suíte de autenticação consulta clientes para pedidos, mas não este endpoint; não serve como evidência da entrega.

`docs/fluxograma-sistema.md` apareceu durante a execução e não foi alterado por este trabalho. V1–V5, autenticação/CSRF, permissões de auditoria e preços de pedidos não foram alterados.

## Roteiro manual futuro — não executado

1. Com backend atualizado e aplicação da V6 previamente autorizada, cadastrar PF e PJ, confirmar nome fantasia obrigatório para PJ e tipo fixo na edição. Conferir telefone/documento/nome obrigatórios e nascimento sem futuro.
2. Cadastrar com e sem endereço inicial; omitir/branquear complemento. Confirmar atomicidade do POST e preservação dos demais campos. Editar complemento com null/omissão (preserva) e vazio/branco (limpa).
3. Remover nascimento pela tela e pela flag, preservar com omissão/null/false e rejeitar flag true com data nova. Cancelar não grava; formulário sem mudanças não faz PATCH.
4. Associar, trocar e remover tabela; preservar tabela atual inativa e vínculo sem prices.read ou diante de erro. Cadastro sem prices.read continua disponível. Verificar que pedidos antigos mantêm preços.
5. Consultar com apenas customers.read; separar criação/edição/situação/exclusão de endereço pelas permissões. A tela não oferece exclusão física de cliente. Permissões administrativas/auditoria permanecem próprias.
6. Buscar Nome/Documento/Contato com filtros combinados e 25/50/100, incluindo email com dígitos, documento com letras/zeros, %/_, page além do resultado, limites inválidos e campo de busca inválido. Conferir total filtrado, ordem estável e retorno à última página após inativação.
7. Simular rede lenta, alteração de filtro/cliente, falha e retry: resposta antiga não substitui resultado atual, erro não vira vazio e campos editados são preservados. Verificar envio duplicado bloqueado.
8. Incluir/editar/excluir endereços e unicidade de tipo; simular conflito concorrente, recarregar tipos e manter edição. Não transferir endereço entre clientes.
9. Consultar CEP válido, inválido, inexistente e indisponível; timeout, edição manual durante chamada, troca/fechamento e substituição confirmada. Não preencher número/complemento nem enviar cookie/CSRF ao ViaCEP.
10. Conferir datas reais de Clientes/Funcionários/Cargos, null, valor inválido, timestamp sem offset e instante com offset; Cargo só criação. Alterar endereço não deve simular atualização do cliente.
11. Percorrer teclado/foco/Escape/descarte, abertura/retorno de detalhes e responsividade desde 320 px.

## Limites da verificação

Na entrega inicial, somente leitura direta/diff, sem navegador. Não criados/executados testes automatizados; sem build, lint, inicialização do backend, SQL ou migrations. Nenhum resultado antigo foi usado como validação do novo módulo. Aplicação futura da V6, demais cenários manuais e testes/build/lint dependem de autorização própria. Não há declaração de prontidão de produção.

## Correção da página em branco — 29/10/2026

Após solicitação do usuário, foi aberta a instância já em execução em http://localhost:5173. A tela em branco foi reproduzida e o console mostrou que CustomerForm.js não exportava default. No Windows, os nomes CustomerForm.jsx e customerForm.js colidiam na resolução do import sem extensão.

O auxiliar foi renomeado para customerFormData.js, seus dois consumidores foram atualizados e a importação do componente em CustomersPage passou a indicar CustomerForm.jsx explicitamente, inclusive para desfazer a resolução antiga mantida pelo servidor de desenvolvimento.

Após recarregar, a aplicação renderizou a tela de login. O console capturado manteve apenas os dois erros anteriores à correção, sem novo erro no carregamento conferido. Evidência visual salva em login-after-fix.jpg na pasta de visualizações deste chat. Não foram fornecidas credenciais nem consultados os fluxos autenticados de Clientes. Sem testes automatizados, build, lint ou aplicação da V6 nesta correção.
