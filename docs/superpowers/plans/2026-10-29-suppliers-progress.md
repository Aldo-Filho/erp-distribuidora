# Progresso — plano: 2026-10-29-suppliers.md

Execução autorizada em 29/10/2026 no checkout local existente. Estado inicial: apenas desenho e plano de Fornecedores não rastreados; preservados. Clientes atual usado como referência, sem reabrir suas pendências antigas.

Ruling: instruções diretas prevalecem sobre passos genéricos da skill executing-plans — sem worktree, agentes, testes, build, lint, SQL, migrations, dependências ou commits. Revisão final pelo próprio executor. Registro neste arquivo em vez dos scripts da skill que pressupõem testes/commits.

Pre-flight: tarefas 1/2 produzem POST/PATCH/Page consumidos por 3/4/5; tarefa 3 inclui país e todos os tipos de endereço; tarefa 4 não inclui endereço no PATCH; tarefa 5 usa permissões existentes. Nenhum conflito encontrado nos contratos aprovados.

## Etapas implementadas e revisadas por leitura

1. Cadastro PJ: phone com NotBlank; CNPJ novo/alterado normalizado apenas por apresentação, maiúsculas e estrutura ASCII de 14 posições; duplicidade sem caixa e excluindo próprio ID. Omitir documento preserva legado. Telefone final obrigatório nas alterações cadastrais; PATCH vazio ou de situação mantém legado sem telefone. removeAvgDeliveryDays Boolean, contradição rejeitada antes de setters; zero preservado. Transações e DELETE existentes mantidos.
2. Consulta: SupplierSearchField e query; Page no banco, page 0..100000, size 1..100, padrão 25; ordem legalName/id. Filtros individuais mantidos em AND e grupos em OR; joins distinct sem fetch de coleção; escape de LIKE e contato sem extrair números de e-mail.
3. Serviço frontend e endereço: chamadas autenticadas próprias, sem delete de fornecedor. País e todos os tipos disponíveis. Brasil/BR/BRA usa usePostalCode de Clientes em subcomponente com key por país; trocar país desmonta/cancela e invalida resposta. País estrangeiro usa campos manuais. Disabled ao salvar cancela consulta. PostalCode existente mantém credentials omit, sem CSRF; não alterado.
4. Formulário PJ: opcionais, prazo inteiro/zero/remoção, primeiro endereço opcional apenas no POST, payload parcial, documento histórico inalterado não reenviado. Sem alterações fecha sem PATCH, inclusive legado sem telefone; mudança cadastral exige telefone. Rascunhos por usuário/ID, confirmação de descarte e bloqueio de envio duplicado, valores preservados diante de erro. Arquivos SupplierForm.jsx e supplierFormData.js distintos no Windows.
5. Lista/detalhes/endereços: rota /suppliers e navegação com suppliers.read. Busca 400 ms com geração/cancelamento, páginas 25/50/100 e ajuste ao reduzir resultado. Edição carrega por ID; 204 tratado como ausente. Retorno aos detalhes recarrega registro. Datas reais via RecordMetadata e prazo zero visível. Ativar/inativar confirma e envia somente active. Endereços mantêm IDs e supplierId apenas na criação, tipos repetidos permitidos. Create/update/delete checados na apresentação e na gravação correspondente; API permanece autoridade. Sem products.read ou tela de vínculos.
6. Contratos: READMEs, índice de endpoints e somente seção Fornecedores da coleção Insomnia atualizados. Cookies e CSRF da coleção preservados. Revisão direta do diff e de todos os arquivos novos; conferência de espaços com git diff --check sem ocorrências. Nenhum teste foi criado ou executado.

## Autorrevisão final

Comparação com customer/address: @Autowired, records, controller enxuto, entidades nas respostas, Specifications, helpers e mensagens em inglês. Diferenças de domínio preservadas: somente PJ (sem personType), fantasia opcional, país no endereço, múltiplos endereços por tipo e exceção explícita de telefone legado. PositiveOrZero e validação de e-mail existentes mantidos; @Valid no PATCH/filtro por prazo. Nenhum DTO de resposta criado.

Leitura dos caminhos de Modal, drafts, estilos responsivos e CEP: foco contido/restaurado, Escape segue guarda de descarte, bloqueio durante gravação e estados de erro/carregamento/vazio separados. Layout reaproveita classes existentes com quebra de filtros/controles e rolagem de tabela; isso é inspeção estática, sem comprovação visual a 320 px.

A revisão ajustou o fechamento sem PATCH antes da validação cadastral, para não obrigar saneamento de legado sem qualquer mudança. A validação nativa é chamada explicitamente após essa comparação, para que required do telefone não bloqueie o caso inalterado antes do submit. Também explicitou telefone ausente na listagem. Não há achado crítico/importante conhecido remanescente nesta autorrevisão; ausência de execução impede concluir funcionamento integrado ou prontidão de produção. Não houve revisão independente por agente, conforme instrução direta.

Nenhuma alteração em Clientes, vínculos, autenticação, compras, migrations, schema, dependências ou credenciais. application.yaml não foi consultado. Documentos prévios, checkout e arquivos ignorados preservados.

## Incompatibilidades e limites

- Na entrega inicial, backend/tools/backend-audit.cjs permaneceu intacto; a incompatibilidade com Page em Fornecedores foi adaptada após autorização específica, conforme registro abaixo. A pesquisa de consumidores não encontrou construtores Java de SupplierFilter/PatchSupplierDTO no código rastreado atual.
- Duplicidade histórica entre CNPJ pontuado e novo normalizado pode escapar à comparação exata sem caixa; não houve saneamento em lote. A constraint existente protege colisões exatas, sem nova garantia de concorrência para diferenças somente de caixa. Conforme desenho, não se acrescentou constraint/migration.
- Regras de fornecedores inativos em compras/vínculos permanecem as existentes; avaliar na etapa correspondente. Nenhum vínculo foi removido ou recadastrado.
- Não executados: testes automatizados/manuais, build, lint, SQL, migrations, backend, navegador/validação visual, instalação, commits, push ou deploy. Apenas implementação e revisão estática.

## Roteiro manual futuro — não executado

1. Cadastro PJ mínimo/com opcionais; telefone vazio, CPF/CNPJ estrutural inválido e duplicado; CNPJ legado preservado na edição sem mudança de documento.
2. Cadastro sem/com endereço; erro do endereço reverte cadastro; complemento ausente/limpo.
3. Legado sem telefone exige preenchimento na edição cadastral; active isolado e formulário inalterado não exigem saneamento; nenhum dado inventado.
4. Prazo ausente, zero, positivo, negativo, fracionário, remoção true/null/false/omitida, contradição e PATCH vazio.
5. Filtros agrupados + individuais, e-mail com números, literais de LIKE, documento parcial, joins múltiplos sem duplicar conteúdo/total e paginação estável.
6. Perfis read/create/update/delete; botões e endpoints correspondentes, sem exclusão definitiva de fornecedor ou ampliação de acesso a Produtos.
7. Endereços do mesmo tipo, duplicata real, edição/exclusão, conflito preservando rascunho e impossibilidade de transferência.
8. CEP válido/inválido/inexistente/indisponível/timeout; país estrangeiro, troca Brasil/BR/país durante consulta, edição manual, salvar/fechar com resposta pendente.
9. Datas reais e updatedAt nulo, prazo zero, rede lenta/respostas antigas, retorno entre modais, foco/Tab/Escape/descarte e tela de 320 px.
10. Após autorização específica, conferir integração e registrar evidências separadas; adaptar executor legado somente com autorização própria.


## Complemento autorizado — adaptação do executor

O usuário autorizou adaptar backend/tools/backend-audit.cjs, sem solicitar sua execução. O arquivo continua ignorado pelo Git, sem alteração de regras de ignore ou inclusão forçada no índice.

- checkList agora encaminha /supplier/search ao mesmo fluxo paginado já existente para Clientes. Mantém filtros em todas as páginas, size 100, limites da API e verificações de content/number/size/totalElements/totalPages; procura o ID até encontrá-lo ou falhar na última página. As demais buscas continuam esperando arrays.
- A consulta direta de rollback usa page 0/size 25 e exige content vazio, número/tamanho esperados e totalElements/totalPages zerados. O cenário que antecede essa consulta e as outras expectativas legadas foram preservados.
- Revisão direta das duas alterações, dos consumidores /supplier/search e da documentação. Nenhuma chamada HTTP, execução do script, teste, build, lint, serviço, SQL ou migration. Esta adaptação não valida as demais expectativas históricas do executor nem autenticação/CSRF.
