# Fornecedores — plano técnico de implementação

> **Para execução por agente:** usar superpowers:executing-plans para implementar sequencialmente, tarefa por tarefa, quando autorizado. As instruções do projeto prevalecem sobre testes e commits sugeridos pelas skills. Manter este chat para planejamento; encaminhar execução para outro chat somente após autorização do usuário.

**Objetivo:** entregar cadastro e endereços de fornecedores PJ no frontend autenticado, com telefone obrigatório, paginação e preservação do histórico.

**Arquitetura:** manter os módulos Supplier e SupplierAddress e a organização de customer/address como referência. Reutilizar API autenticada, modais, paginação e metadados existentes. Componentes específicos de Fornecedores permanecem juntos; reutilizar o mecanismo de CEP sem remodelar Clientes.

**Tecnologias:** Java/Spring Boot, JPA Specifications, React e componentes existentes; nenhuma dependência nova prevista.

**Desenho:** [Fornecedores](../specs/2026-10-29-suppliers-design.md), aprovado em 29/10/2026; [metadados](../specs/2026-10-29-record-metadata-design.md).

## Restrições globais

- Planejamento apenas nesta entrega. Não implementar código, instalar dependências, executar testes/build/lint/SQL/migrations, iniciar serviços ou criar commits.
- Na execução futura, conferir Git e arquivos atuais; preservar alterações locais e arquivos ignorados. Não criar checkout de HEAD substituindo o trabalho atual.
- Sem testes automatizados, build, lint, SQL ou aplicação de migrations por padrão, inclusive na execução. Preparar roteiro manual sem executá-lo automaticamente.
- Não alterar V1 ou migrations aplicadas. Nenhuma migration é necessária para complemento de fornecedor, já nullable. Telefone obrigatório será validado pela aplicação, preservando registros legados sem preenchimento; não adicionar NOT NULL nem inventar telefones.
- Sem telas ou mudanças de vínculos produto/marca/categoria, compras, fiscal, financeiro, autenticação ou WhatsApp. Não remover o DELETE existente de fornecedor.
- Seguir AGENTS.md: entidades nas respostas, @Autowired, records de entrada, Specification.allOf()/spec, nomes/mensagens de exceção em inglês, validações no lugar correspondente. Preservar contratos alheios ao escopo.

## Pontos principais de revisão

1. Fornecedor legado sem telefone: salvar edição cadastral exige preencher; PATCH exclusivamente de situação continua possível.
2. Endereços: vários do mesmo tipo são permitidos; duplicatas reais continuam rejeitadas; POST/create, PATCH/update e DELETE/delete preservados.
3. Busca com joins de vínculos: conteúdo e contagem sem fornecedores repetidos, filtros existentes preservados e paginação no banco.
4. CEP: país estrangeiro ou alterado durante requisição impede preenchimento tardio; gravação/fechamento cancela a consulta.
5. Opcionais: zero não é ausência, limpar prazo usa flag explícita e PATCH sem mudanças não grava nem perde dados.

## Decisões técnicas propostas pelo plano

- Cadastro PJ usa legalName/taxId, sem personType. Nome fantasia permanece opcional.
- CNPJ novo ou alterado: remover apenas espaços e pontuação de apresentação (ponto, barra e hífen), converter letras para maiúsculas e exigir 14 caracteres ASCII alfanuméricos. Isso é validação estrutural mínima, não verificação de existência, regularidade ou dígitos verificadores. Não introduzir consulta fiscal nem algoritmo de validação nesta entrega. Rejeitar CPF com 11 posições. Não apagar caracteres arbitrários para fazer entrada inválida passar.
- Preservar taxId legado quando omitido/null no PATCH. Na tela, comparação explícita com o original evita reenviar documento inalterado. Para documentos alterados, verificar duplicidade ignorando caixa e excluindo o próprio ID, além de manter a constraint do banco. Não normalizar cadastros antigos em lote. Registrar possíveis equivalências históricas não cobertas sem editar dados automaticamente.
- Acrescentar removeAvgDeliveryDays do tipo Boolean ao PATCH: true limpa; omitido/null/false preserva; true com avgDeliveryDays informado rejeita antes dos setters. Prazo zero permanece válido.
- PATCH exclusivamente de situação significa active informado e todos os demais campos de cadastro ausentes/null, sem flag de remoção true. Esse caso preserva telefone legado. Qualquer edição cadastral exige telefone final não vazio. Enviar telefone vazio é sempre inválido. PATCH vazio não deve forçar saneamento de legado.
- Endereço inicia com país Brasil, editável; reconhecer Brasil/BR/BRA ignorando caixa e espaços externos como brasileiro somente para habilitar CEP. Outros valores mantêm preenchimento manual e os campos existentes obrigatórios; não criar modelo internacional de endereço nesta etapa.

## Mapa de arquivos e tarefas

### Tarefa 1 — Cadastro PJ, telefone e PATCH de prazo

**Modificar:** backend/src/main/java/com/pi/erp/supplier/RequestSupplierDTO.java, PatchSupplierDTO.java, SupplierService.java e SupplierRepository.java.

**Referências:** customer/RequestCustomerDTO.java, PatchCustomerDTO.java, CustomerService.java; supplier/address/SupplierAddressService.java. Ler versões atuais antes de editar.

**Interface produzida:** POST /supplier mantém RequestSupplierDTO com phone obrigatório. PATCH /supplier/{id} mantém campos atuais e acrescenta Boolean removeAvgDeliveryDays. Respostas continuam Supplier; GET ausente continua 204.

- [ ] Adicionar @NotBlank a phone no DTO de cadastro; manter @Email e @PositiveOrZero existentes. Normalizar/verificar CNPJ no service antes de persistir, com auxiliares locais de responsabilidade específica.
- [ ] Acrescentar a flag de prazo ao record PATCH; tratar contradição, campos finais obrigatórios e exceção de situação isolada conforme decisões acima, antes de setters. Preservar normalização de textos opcionais e validação de e-mail.
- [ ] Acrescentar existsByTaxIdIgnoreCase e existsByTaxIdIgnoreCaseAndIdNot ao repository, usando-os nos novos documentos sem remover consumidores existentes indiscriminadamente. A constraint continua protegendo gravações concorrentes exatas.
- [ ] Manter transação do cadastro com endereço e PATCH atual, sem alterações no delete nem vínculos. Não acrescentar país ao endereço de Cliente por copiar DTOs.
- [ ] Revisar diretamente cadastro sem telefone, limpeza de telefone, legado com active isolado, edição de legado, CNPJ inválido/duplicado, prazo zero/remoção/contradição e atomicidade fornecedor/endereço. Registrar cenários para validação futura, sem executá-los.

### Tarefa 2 — Consulta paginada e filtros agrupados

**Modificar:** supplier/SupplierController.java, SupplierService.java, SupplierFilter.java. **Criar:** supplier/SupplierSearchField.java.

**Interface produzida:** GET /supplier/search com page padrão 0, size padrão 25; page 0..100000 e size 1..100. Page<Supplier> com content, number, size, totalElements, totalPages. Service: search(SupplierFilter filter, Pageable pageable). Enum NAME, DOCUMENT, CONTACT; filter acrescenta query e searchField, preservando componentes atuais e @Valid no controller por causa de avgDeliveryDays.

- [ ] Seguir CustomerController para limites e ordenação legalName/id ascendente. Query preenchida sem searchField usa NAME; enum inválido e limites inválidos retornam erro de entrada pelo tratamento existente.
- [ ] Preservar filtros individuais e joins de supplierAddressId, productId, brandId e categoryId. Compor tudo com AND; cada grupo novo usa OR internamente. Usar findAll(spec, pageable), sem fetch de coleção, com distinct onde os joins exigirem.
- [ ] NAME busca parcial sem diferenciar caixa em legalName/tradeName; CONTACT busca e-mail e, quando o texto inteiro tiver formato de telefone, números normalizados de phone/whatsapp. Não interpretar dígitos soltos de um e-mail como telefone.
- [ ] DOCUMENT busca parcial sobre documento normalizado para pesquisa, ignorando caixa/pontuação de apresentação; entrada não vazia que normalize para vazio é inválida. Busca não revalida tamanho de cadastro e não regrava documentos. Escapar curingas %, _ e o próprio caractere de escape, seguindo helpers de Clientes sem criar abstração global.
- [ ] Revisar agrupamento AND/OR, filtros textuais antigos, contagem com joins, ordem estável e ausência de paginação em memória. Não tornar os endpoints de endereços/vínculos paginados como efeito colateral.

### Tarefa 3 — Serviço frontend e campos de endereço

**Criar:** frontend/src/services/suppliers.js, frontend/src/pages/Suppliers/SupplierAddressFields.jsx. **Reutilizar sem alterar contratos:** services/api.js, services/postalCode.js e pages/Customers/usePostalCode.js.

**Interface produzida:** suppliers.search(filter, page=0, size=25, options), findOne(id, options), register(body), update(id, body), addresses(supplierId, options), registerAddress(body), updateAddress(id, body), deleteAddress(id). SupplierAddressFields recebe value/onChange/disabled; exporta emptySupplierAddress e supplierAddressKeys, incluindo country.

- [ ] Centralizar chamadas autenticadas nos endpoints /supplier e /supplierAddress, como no serviço de Clientes, mas com query string local (não importar utilitário do serviço de Clientes por conveniência). Não disponibilizar delete de fornecedor no serviço de tela.
- [ ] Criar campos específicos de endereço: país, tipo, CEP, estado, cidade, bairro, logradouro, número e complemento. Todos os tipos sempre disponíveis; não excluir tipo já utilizado por outro endereço.
- [ ] Reutilizar usePostalCode em um subcomponente montado somente para país brasileiro; toda mudança de país deve desmontar/cancelar a consulta e invalidar a resposta anterior. Para outros países, exibir os mesmos campos manuais sem consulta ViaCEP. Não duplicar o hook inteiro nem alterar o comportamento já validado de Clientes.
- [ ] Manter valor do país ao preencher retorno do CEP. Usar a interface change do hook para edições durante consulta; disabled enquanto grava e desmontagem ao fechar. Número/complemento permanecem manuais.
- [ ] Revisar ausência de cookies/CSRF na consulta pública e preservação de campos diante de erro, cancelamento e alteração de país.

### Tarefa 4 — Formulário de fornecedor

**Criar:** pages/Suppliers/SupplierForm.jsx e supplierFormData.js. **Reutilizar:** components/feedback/Modal.jsx, ErrorAlert.jsx e auth/drafts.js.

**Interfaces produzidas:** SupplierForm({supplier, onClose, onSaved}); supplierValues(supplier), validateSupplier(value, editing), supplierPayload(value, supplier) em supplierFormData.js. Nomes de arquivos distintos também ignorando caixa, evitando a colisão anterior CustomerForm/customerForm no Windows.

- [ ] Formulário PJ sem seletor PF/PJ; campos e obrigatoriedades do desenho, prazo numérico inteiro não negativo e opcionais sem exigir nome fantasia.
- [ ] Cadastro envia apenas campos conhecidos e endereço quando habilitado. Endereço opcional desabilitado não envia dados parciais ocultos. Edição não inclui endereço no PATCH principal.
- [ ] Payload parcial compara valores originais: limpar textos opcionais envia vazio; limpar prazo preexistente envia removeAvgDeliveryDays=true; zero envia 0; ausência original sem mudança não envia flag. Incluir telefone preenchido ao editar legado; não reenviar CNPJ inalterado.
- [ ] Formulário sem mudanças fecha/retorna sem PATCH. Cancelamento confirma descarte quando necessário, mantém rascunho em memória por usuário/modo/ID e limpa após salvar ou descarte confirmado. Usar chave React para remontar ao mudar entidade, como Clientes.
- [ ] Bloquear duplo envio, manter valores em erro e cancelar CEP ao gravar/desmontar. Textos de erro objetivos, sem subtítulos genéricos ou detalhes de implementação.
- [ ] Revisar separadamente criação sem/com endereço, edição de opcionais e legado sem telefone. Não apresentar a revisão como execução dos cenários.

### Tarefa 5 — Lista, detalhes, endereços e navegação

**Criar:** pages/Suppliers/SuppliersPage.jsx, useSuppliers.js, SupplierDetailsModal.jsx e SupplierAddressModal.jsx. **Modificar:** routes/AppRoutes.jsx, routes/navigation.js, components/layout/AppLayout.jsx. Estilos: reutilizar classes existentes; se necessário, criar pages/Suppliers/suppliers.css para diferenças locais, sem formatação global.

**Interfaces consumidas:** serviço da tarefa 3, SupplierForm da tarefa 4, RecordMetadata/Modal/Pagination existentes. useSuppliers segue a interface de useCustomers (filters/page/size/data/loading/error/changeFilters/changePage/changeSize/reload), sem personType. SupplierDetailsModal({supplierId,onClose,onEdit,onAddresses}); SupplierAddressModal({supplierId,onClose}).

- [ ] Adicionar /suppliers protegido por suppliers.read, item Fornecedores com ícone existente apropriado e retorno seguro pelo navigation.js. Não mudar rotas existentes ou firstRoute de forma especial.
- [ ] Lista com colunas do desenho, busca agrupada NAME/DOCUMENT/CONTACT, situação Todos inicialmente, tamanho 25 e opções 50/100. Debounce de 400 ms, cancelamento e geração para resposta atrasada; mudar filtro/tamanho retorna à página zero. Depois de gravação/inativação, recarregar e ajustar página se o resultado encolher.
- [ ] Abrir formulário de edição carregando o fornecedor atual por ID; 204 vira estado de não encontrado. Preservar retorno de modal aos detalhes e tratamento de erro com nova tentativa.
- [ ] Detalhes incluem opcionais, prazo (zero visível), endereços e RecordMetadata com createdAt/updatedAt. Sem campos fictícios de datas em endereços e sem telas de vínculos nesta etapa.
- [ ] Ativar/inativar com confirmação e PATCH apenas de active; nenhuma ação de excluir fornecedor. Não alterar vínculos ou compras durante essa ação.
- [ ] Gerenciar endereços por lista e formulário próprio, IDs originais fixos, sem transferência entre fornecedores. Dados do endereço inicial vão no POST principal; endereços posteriores no endpoint próprio. Preservar resposta de duplicidade e edição diante de conflito.
- [ ] Exigir read para a tela; create para cadastro/endereço novo, update para editar/situação/endereço existente e delete para excluir endereço. Visibilidade e execução devem checar a permissão; API continua sendo a autoridade. Sem necessidade de products.read nesta fase.
- [ ] Revisar navegação por teclado, foco, Escape, descarte, erro/estado vazio/carregamento e layout desde 320 px, por inspeção do código; verificação visual futura depende de autorização.

### Tarefa 6 — Contratos, documentação e revisão final

**Modificar:** backend/README.md, frontend/README.md, testeRequisicoesInsomnia.yaml e, se o contrato listado exigir, docs/superpowers/plans/2026-10-23-authentication-and-access-endpoints.md. **Criar na execução:** docs/superpowers/plans/2026-10-29-suppliers-progress.md.

- [ ] Documentar telefone obrigatório, tratamento de legado, formato estrutural de CNPJ, flag removeAvgDeliveryDays e mudança lista→Page. Atualizar somente requisições/descrições de Fornecedores na coleção, preservando sessão/CSRF e os demais módulos.
- [ ] Buscar novamente todos os consumidores de /supplier/search e construções de SupplierFilter. Levantamento atual encontrou backend/tools/backend-audit.cjs (arquivo ignorado) com checkList e consulta direta de rollback que esperam lista. Registrar essas incompatibilidades no progresso/roteiro: não modificar nem executar o executor automatizado nesta entrega sem autorização específica. Não prometer compatibilidade com esse script após mudar a API.
- [ ] Comparar arquivos backend com customer/address e registrar diferenças justificadas: sem personType, nome fantasia opcional, país, múltiplos endereços por tipo e legado sem telefone. Preservar validações que não conflitem com as decisões aprovadas.
- [ ] Revisar diff completo e arquivos novos; conferir que não houve alteração de vínculos, migrations, autenticação, segredos ou arquivos alheios. Não ler/expor credenciais de application.yaml.
- [ ] Atualizar progresso com o que foi implementado e o que foi apenas revisado. Apresentar pendências e roteiro abaixo; não declarar testes, build, validação visual ou prontidão de produção sem evidência correspondente.

## Roteiro manual futuro — preparar, não executar automaticamente

1. Cadastrar PJ com razão social/CNPJ/telefone e opcionais vazios; rejeitar telefone vazio, CPF, CNPJ estruturalmente inválido e duplicidade; preservar documentos legados inalterados.
2. Cadastrar com e sem primeiro endereço; erro no endereço não deixa fornecedor parcialmente criado. Complemento ausente/limpo permanece opcional.
3. Editar legado sem telefone: exigir preenchimento cadastral; ativar/inativar isoladamente sem forçar alteração de outros campos. Nenhum telefone ou documento inventado.
4. Prazo ausente, zero, positivo, negativo, fracionário, remoção explícita, contradição flag+valor e PATCH sem alterações.
5. Nome/documento/contato com filtros legados combinados, e-mail com números, caracteres de LIKE, pesquisa parcial, paginação e join de múltiplos vínculos sem duplicar fornecedor/total.
6. Usuários com read apenas e combinações de create/update/delete: tela e endpoints respeitam cada ação; sem exclusão física do fornecedor nem acesso ampliado a Produtos.
7. Endereços repetindo tipo, duplicidade real, edição/exclusão e falha de gravação preservando formulário; não transferir entre fornecedores.
8. CEP válido/inválido/inexistente/indisponível, timeout, troca de país/CEP, edição durante consulta, gravação e fechamento; endereço estrangeiro permanece manual.
9. Metadados reais, updatedAt nulo, prazo zero, rede lenta, respostas fora de ordem, navegação/retorno, teclado e responsividade.
10. Conferir a integração real após autorização própria; registrar separadamente validação de negócio, revisão de permissões e qualquer teste automatizado expressamente autorizado.

## Dependências e paralelismo possível

Tarefa 2 depende de preservar as regras existentes, mas pode ser desenvolvida separadamente da 1 depois de fixar contratos. Tarefa 3 pode ocorrer em paralelo às tarefas backend; tarefa 4 depende de 1/3 e tarefa 5 depende de 2/3/4. Documentação e revisão final vêm após a integração. Essa possibilidade não autoriza iniciar execução ou delegar agentes automaticamente; a execução proposta é sequencial para reduzir conflitos.

## Autorrevisão do plano

Cobertura conferida contra o desenho aprovado: cadastro PJ/telefone e prazo na tarefa 1; paginação e filtros na 2; CEP/país na 3; formulário/rascunhos na 4; permissões, detalhes, datas e situação na 5; consumidores/limites de verificação na 6. Os cinco focos de revisão têm cenários no roteiro manual. Não há migration prevista nem exigência de novas dependências. Os detalhes técnicos propostos acima precisam constar da revisão do plano antes da implementação.
