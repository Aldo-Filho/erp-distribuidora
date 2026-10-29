# Clientes — plano técnico de implementação

> Para execução futura: seguir superpowers:executing-plans após revisão deste plano e autorização explícita de implementação. O desenho aprovado autoriza preparar o plano, não executar suas tarefas. Checkboxes representam trabalho pendente; não iniciar agentes, instalações, testes ou commits por ler este arquivo.

**Objetivo:** entregar Clientes no frontend atual, com listagem paginada, formulários PF/PJ, detalhes, endereços/CEP e associação opcional a tabela de preços. Incluir o padrão de datas também em Funcionários/Cargos.

**Arquitetura:** aproveitar o backend customer/address e a navegação autenticada existentes. Alterar explicitamente a busca para Page<Customer>, acrescentar somente os contratos de remoção de nascimento/complemento necessários e reutilizar componentes de interface. Consulta de CEP será feita pelo navegador ao ViaCEP, sem novo endpoint de proxy ou dependência.

**Stack:** Spring Boot/Java/JPA/PostgreSQL/Flyway e React/Vite/Tailwind/Lucide já presentes, com fetch e AbortController. Não atualizar frameworks nem instalar bibliotecas nesta entrega por conveniência.

**Especificações aprovadas:** [Clientes](../specs/2026-10-29-customers-design.md) e [Informações do registro](../specs/2026-10-29-record-metadata-design.md). O desenho de 10/10/2026 é histórico; não restaurar seu roteamento ou arquivos removidos indiscriminadamente.

## Restrições globais

- Este documento foi preparado em fase de análise/planejamento: somente documentação foi alterada.
- Ler AGENTS.md e o Git antes de futura execução. Preservar arquivos modificados e não rastreados; não basear o trabalho apenas no HEAD, que não contém toda a implementação atual.
- Usar customer/address como referência e manter injeção por campos, services por funcionalidade, records de entrada, Specification.allOf()/spec e respostas com entidades. Preservar ResponseErrorDTO.
- Sem testes automatizados, build, lint, commits ou push automaticamente. Cenários deste plano são critérios, não resultados executados. Não aplicar migrations ou recriar banco sem autorização específica.
- Não editar V1–V5 existentes, expor segredos ou reformatar globalmente. A próxima migration observada é V6; conferir versões novamente ao executar.
- Não alterar autenticação, sessões, auditoria, preços de pedidos ou demais módulos como efeito colateral. Sem WhatsApp, emissão fiscal, frontend de Preços completo ou exclusão de cliente na interface.
- Retornar o texto em português na interface e mensagens novas de backend em inglês, específicas e com ponto final. Não acrescentar validações fiscais/documentais ou @Size apenas para repetir colunas.

## Foco da revisão

1. Busca paginada e respostas atrasadas: nenhuma filtragem apenas sobre a página atual, resposta antiga não substitui filtro novo e navegação não fica presa a uma página vazia após inativação.
2. Campos apagados e omitidos: nascimento e tabela usam intenção explícita; complemento aceita limpar; campos não exibidos e associação não carregada são preservados.
3. Permissões diferentes: consulta sem edição, cadastro sem prices.read e exclusão de endereço sem exclusão de cliente mantêm ações coerentes com a API.
4. CEP concorrente: resposta atrasada de outro CEP/endereço não sobrescreve dados e falha externa permite preenchimento manual.
5. Datas e persistência: null não vira data fictícia, Cargo não ganha updatedAt inexistente e uma alteração de endereço não é apresentada como atualização do cliente.

## Contratos definidos para esta implementação futura

### Busca paginada

- GET /customer/search passa de array para Page<Customer>, como o Page usado em convites/auditoria. Conteúdo continua composto por entidades; não criar ResponseCustomerDTO. O frontend consome content, number, size, totalElements e totalPages, sem depender de metadados internos extras do serializador.
- page inicia em 0; padrão 0. size padrão 25, faixa 1–100; a interface oferece 25/50/100. page negativo, acima de 100000 ou size fora da faixa retorna 400, seguindo o limite já praticado em buscas de acesso. Ordenação fixa por legalName ascendente e id ascendente para desempate; não abrir sort arbitrário nesta entrega.
- Preservar todos os campos individuais de CustomerFilter e seus operadores atuais. Acrescentar query e searchField, com enum NAME, DOCUMENT ou CONTACT. query ausente/branco não acrescenta predicado; query preenchido sem campo usa NAME; campo inválido retorna 400.
- NAME usa busca parcial sem diferença de caixa em legalName OU tradeName. CONTACT usa busca parcial em email OU telefone/WhatsApp; habilitar o ramo numérico somente quando a consulta inteira representar telefone, contendo dígitos e pontuação de apresentação, sem letras/@. Não reduzir um e-mail com um dígito a uma pesquisa de telefone por esse dígito nem usar telefone vazio como predicado que casa tudo. DOCUMENT usa comparação parcial do documento normalizado para pesquisa, preservando letras e dígitos; consulta preenchida que se torne vazia após normalização retorna 400. Remover somente pontuação de apresentação na expressão de pesquisa, sem converter documento para número ou modificar dados armazenados.
- Predicado agrupado é unido com AND aos filtros individuais, personType, active e priceTableId. Escapar curingas LIKE vindos do usuário para que % e _ pesquisados sejam literais. Manter parâmetros vinculados; não montar SQL com entrada concatenada.
- Nome/documento/email devem permitir pesquisa coerente com os valores armazenados; normalização usada na busca não cria nova validação de CPF/CNPJ nem altera a unicidade atual. Índices adicionais só se justificam por evidência futura; não prometer que a paginação elimina custos de contagem/ordenação.
- Página além dos resultados devolve conteúdo vazio e total correto; a interface retorna à última página válida se houver resultados. Falha não é convertida em Page vazio.
- Não haverá modo oculto de retorno do array antigo ou migração global de outros endpoints. A mudança foi aprovada e deve ser documentada para os consumidores existentes.

### Cadastro, edição e endereços

- Preservar POST /customer, PATCH /customer/{id}, GET /customer/{id} e /customerAddress. findOne ausente continua 204. DELETE /customer/{id} permanece na API existente, mas não é chamado nem exibido pela nova tela.
- Acrescentar Boolean removeBirthDate ao PatchCustomerDTO. true com birthDate preenchido retorna 400 antes de mutações; true limpa; omissão/null/false não remove. Datas novas continuam sem futuro. Não alterar personType na edição.
- Complemento ausente/vazio/branco no cadastro vira null; no PATCH, omissão/null preserva e texto vazio/branco vira null. Retirar @NotBlank somente desse campo nos dois DTOs de cadastro de endereço e permitir null no schema por migration nova.
- Tabela opcional: novo vínculo envia priceTableId; remoção envia somente removePriceTable=true; preservação omite ambos. Valor atual não pode desaparecer por falta de prices.read, falha no carregamento ou tabela inativa.
- Cliente/endereço inicial continuam no POST transacional único. Endereços posteriores possuem cadastro/edição/exclusão separados e mantêm a unicidade cliente/tipo. Não alterar o cliente de um endereço pela interface.
- Permissões atuais: customers.read para consulta; customers.create para cadastro/inclusão de endereço; customers.update para edição/situação; customers.delete somente no botão de exclusão de endereço. prices.read autoriza carregar opções de tabela. Não conceder permissões adicionais automaticamente.

### CEP e informações do registro

- ViaCEP consultado diretamente por HTTPS no navegador, com fetch independente do cliente autenticado /api: sem cookie, credencial ou header CSRF do ERP enviado ao provedor. Não usar JSONP.
- Aceitar oito dígitos, com ou sem o hífen de apresentação na posição correta; entrada com letras/tamanho incorreto é inválida e não gera chamada. Timeout proposto de oito segundos; cancelar chamadas superadas e ao fechar/trocar formulário.
- Resultado mapeia uf, localidade, bairro e logradouro. Número e complemento permanecem manuais, mesmo que o provedor retorne complemento. erro=true é CEP inexistente; falha/timeout é indisponibilidade; ambos preservam edição manual.
- Preencher campos vazios ou ainda iguais ao último preenchimento assistido. Se houver valores manuais diferentes, pedir confirmação antes de substituí-los. Conferir ID da requisição, CEP, cliente/endereço e revisão do formulário antes de aplicar; edição manual durante a chamada invalida a aplicação automática da resposta.
- Componente RecordMetadata recebe record e fields, com campos suportados explicitamente. Defaults createdAt/updatedAt; Cargo usa apenas createdAt. null/ausência de createdAt suportado mostra Data não registrada; updatedAt suportado nulo mostra Sem atualização registrada; string inválida mostra Data inválida. Campos não suportados não produzem linha.
- Formato DD/MM/AAAA HH:mm:ss. Timestamps sem offset preservam componentes de calendário/hora recebidos, sem acrescentar Z; valores com offset são formatados em pt-BR no fuso do navegador, como os instantes já exibidos no frontend. Datas LocalDate de nascimento/vigência não passam por conversão de instante.

## Tarefa 1 — Complemento opcional e remoção do nascimento

**Dependências:** nenhuma outra tarefa deste plano; reler DTOs/service/schema atuais.

**Arquivos:** backend/src/main/java/com/pi/erp/customer/{RequestCustomerDTO.java,PatchCustomerDTO.java,CustomerService.java}; customer/address/{RequestCustomerAddressDTO.java,CustomerAddressService.java,CustomerAddress.java} sob o mesmo pacote-base; novo backend/src/main/resources/db/migration/V6__allow-null-customer-address-complement.sql.

**Interfaces produzidas:** removeBirthDate no PATCH e complemento nullable nos fluxos inicial/posterior. Preservar nomes de métodos/controllers.

- [ ] Validar contradição e nascimento futuro antes de aplicar setters; calcular intenção final de nascimento sem tornar o campo obrigatório. Manter removePriceTable com Boolean.TRUE.equals e as demais validações existentes.
- [ ] Remover obrigatoriedade de complemento em RequestCustomerDTO.AddressDTO e RequestCustomerAddressDTO; normalizar apenas o complemento no service, no cadastro e na edição conforme o contrato. Não criar construtores de DTO com normalização nem tornar opcionais os demais campos do endereço.
- [ ] Garantir que o caminho de criação por CustomerService.register passe pela mesma regra de complemento do service de endereço, na mesma transação existente. Ajustar mapeamento da entidade apenas se necessário para refletir nullable, sem outros callbacks/campos.
- [ ] Criar somente a migration que relaxa NOT NULL de customer_addresses.complement; não apagar ou reescrever valores existentes. Nascimento já é nullable e não precisa de migration própria. Não executar SQL.

**Critérios de conclusão:** revisão direta contempla cadastro inicial e separado sem complemento, remoção com string vazia, PATCH omitido preservando, remoção de nascimento sem data nova e conflito com data preenchida. Não declarar execução desses cenários.

## Tarefa 2 — Busca, filtros e paginação no backend

**Dependências:** tarefa 1 para evitar edição concorrente de CustomerService; nenhuma mudança em repositories de outros módulos.

**Arquivos:** customer/CustomerController.java, CustomerService.java, CustomerFilter.java, CustomerRepository.java; novo customer/CustomerSearchField.java. Caminhos sob backend/src/main/java/com/pi/erp/. Modificar repository somente se for necessário carregar a associação priceTable de forma adequada à página; não adicionar métodos redundantes de acesso.

**Interfaces produzidas:** CustomerController.search recebe CustomerFilter e page/size explícitos; CustomerService.search recebe filter e Pageable e retorna Page<Customer>. CustomerSearchField declara NAME/DOCUMENT/CONTACT. Nenhum outro endpoint muda para Page.

- [ ] Acrescentar os dois campos opcionais ao filtro, mantendo todos os campos antigos. Implementar predicados agrupados com OR interno e AND externo sobre Specification.allOf(), variável spec e métodos privados focados do próprio service.
- [ ] Construir Pageable com validação simples de limites e ordenação legalName/id. Não importar helper de paginação de um controller de auditoria nem criar abstração global apenas por uniformidade.
- [ ] Retornar repository.findAll(spec, pageable), sem buscar todos e fazer subList em memória. Garantir count com os mesmos filtros e sem fetch de coleções que distorça total/paginação.
- [ ] Conferir representação de priceTable e callbacks com open-in-view=false, mantendo entidade como resposta e sem acesso posterior a coleção de endereços. Usar o mapeamento/estratégia existente ou ajuste pontual de carregamento se necessário.
- [ ] Documentar a mudança do retorno para Page e os operadores da nova busca. Localizar consumidores de /customer/search e mudanças necessárias na coleção; não ampliar para /customerAddress/search ou /price-table/search, que permanecem listas nesta entrega.

**Critérios de conclusão:** contrato paginado definido, limite máximo aplicado no servidor, ordenação determinística, filtros antigos preservados e nenhum OR indevido entre filtros independentes. %/_ e query de e-mail não fazem ramos numéricos vazios retornarem todos.

## Tarefa 3 — Modal acessível e metadados, inclusive telas existentes

**Dependências:** especificação de metadados; pode ser preparada após contratos sem depender da implementação do formulário de Clientes.

**Arquivos novos:** frontend/src/components/feedback/Modal.jsx; frontend/src/components/record/RecordMetadata.jsx; frontend/src/utils/dateTime.js. **Alterações:** frontend/src/pages/Access/UsersPage.jsx, RolesPage.jsx, frontend/src/services/access.js e frontend/src/index.css apenas nas regras necessárias.

**Interfaces produzidas:** Modal recebe title, onClose, busy, children e wide opcional; RecordMetadata recebe record e fields; formatRecordDateTime recebe string de data/hora e devolve apresentação ou indicação controlada de ausência/inválido. access.user(id) consulta /user/{id} para detalhes.

- [ ] Criar Modal reutilizável com foco inicial, confinamento/restauração de foco, Escape e botão de fechar identificável. Enquanto busy, impedir fechamento/envio repetido. Reutilizar classes dialog/modal-backdrop existentes com extensão localizada para largura; não refatorar os diálogos de sessão como efeito colateral.
- [ ] Implementar o contrato de datas acima sem bibliotecas novas, data fictícia ou conversão de LocalDate em UTC. Usar campos suportados explícitos; não inferir suporte de updatedAt apenas por valor truthy.
- [ ] Acrescentar Informações/Detalhes na linha do funcionário, buscando o cadastro com a autorização já existente. Em 204, apresentar registro não encontrado; não apresentar metadados vazios como se o registro existisse.
- [ ] Em Cargos, mostrar createdAt do cargo persistido selecionado, fora dos inputs; não exibir no formulário de Novo cargo. Após recarga usar o registro atual, sem depender de dados antigos do form.
- [ ] Manter permissões atuais das páginas e security.audit.read intactas. Não adicionar timestamps à entidade Role, modificar auditoria ou transformar a consulta em edição.

**Critérios de conclusão:** Funcionário mostra ambas as datas e Cargo somente criação; leitura independe de iniciar edição; formato e ausência são consistentes; modal opera por teclado e não rompe as telas administrativas existentes por alteração global de CSS.

## Tarefa 4 — Serviços de Clientes/preços e endereço assistido por CEP

**Dependências:** tarefas 1–3 para os contratos; sem endpoint novo de CEP.

**Arquivos novos:** frontend/src/services/customers.js, prices.js, postalCode.js; frontend/src/pages/Customers/CustomerAddressFields.jsx e usePostalCode.js.

**Interfaces produzidas:** customers.search(filter,page,size,{signal}), findOne(id,{signal}), register(data), update(id,data), addresses(customerId,{signal}), registerAddress(data), updateAddress(id,data), deleteAddress(id). prices.search(filter,{signal}) consulta o endpoint atual de tabelas; lookupPostalCode(zipCode,{signal}) usa fetch público. CustomerAddressFields recebe value, onChange, disabled e tipos disponíveis; não grava por conta própria.

- [ ] Usar api.js existente para todas as chamadas do ERP, com sessão/CSRF e sem retry automático de gravações. Não criar cliente paralelo autenticado. Converter parâmetros vazios para omissão; 204 continua null, e /customerAddress/search continua array.
- [ ] Implementar consulta ViaCEP por ação explícita e contrato de formato/timeout/mapeamento acima. Abort por troca de CEP/fechamento não vira mensagem de erro de servidor; indisponibilidade e inexistência recebem mensagens distintas e preenchimento manual permanece disponível.
- [ ] Criar hook com sequência e revisão de formulário: resposta só afeta a instância/endereço/CEP que a iniciou. Proteger correções manuais e pedir confirmação das substituições necessárias. Desabilitar duplicação da mesma consulta enquanto pendente, sem bloquear digitação manual.
- [ ] Compartilhar campos de endereço entre cadastro inicial e gerenciamento posterior; tipos MAIN/BILLING/SHIPPING são Principal/Cobrança/Entrega. Complemento opcional não recebe valor padrão inventado.

**Critérios de conclusão:** nenhuma credencial do ERP vai para ViaCEP; resposta de outro cliente/CEP não modifica o formulário; número/complemento são preservados; consulta externa falha sem bloquear endereço manual. Nenhuma chamada externa foi executada por este plano.

## Tarefa 5 — Formulário PF/PJ e associação a preço

**Dependências:** tarefas 1, 3 e 4.

**Arquivos novos:** frontend/src/pages/Customers/CustomerForm.jsx e customerForm.js. Reutilizar auth/useAuth.js e auth/drafts.js, sem novo armazenamento persistente.

**Interfaces produzidas:** CustomerForm recebe customer (null para cadastro), onSaved e onClose. customerForm.js concentra valores iniciais, validação de entrada e construção de cadastro/PATCH; não duplica regras de persistência. onSaved recebe a entidade retornada pela API; pai controla recarga da página.

- [ ] Exibir campos conforme PF/PJ e conservar valores digitados ao alternar tipo antes do cadastro. Na edição, personType é somente leitura. Não apagar dados legados ocultos e não adicionar novos validadores de autenticidade do documento.
- [ ] Criar payload por campos explícitos, sem espalhar entidade inteira. Nome/documento/telefone obrigatórios, nome fantasia obrigatório para PJ, e-mail validado quando preenchido e nascimento sem futuro. Preservar LocalDate como string de calendário.
- [ ] No PATCH, comparar com o cadastro original e enviar somente alterações. Campos opcionais apagados enviam string vazia; remoção de nascimento e tabela usam as flags explícitas e nunca novo valor junto. Se nada mudou, não disparar PATCH apenas para produzir novo updatedAt.
- [ ] Carregar opções ativas de preço somente com prices.read; representar a tabela atual mesmo inativa. Carregamento/erro desse seletor não impede editar outros campos e não limpa a associação; sem permissão, seleção não é modificável. Nome atual vindo do cliente pode ser mostrado sem consultar itens da tabela.
- [ ] Cadastro com endereço habilitado monta address e usa o POST único. Desabilitar a seção omite address; não gravar cliente e endereço em requisições separadas. Estado/erros preservam o formulário completo.
- [ ] Usar rascunho em memória por usuário/modo/cliente, limpando ao salvar/cancelar/trocar conta segundo o mecanismo existente. Remontar o formulário com key por modo/ID ao trocar de registro: o useDraft atual não reinicializa seu useState apenas porque a chave do argumento mudou. Não prometer recuperação após reload. Modal bloqueia envio/fechamento enquanto salva e confirma descarte quando houver edição não salva.

**Critérios de conclusão:** payload não contém datas de registro, campos de outro cliente ou associações não alteradas; apagar nascimento/complemento/tabela tem efeitos distintos corretos; usuário sem prices.read cadastra sem tabela e edita sem perda de vínculo.

## Tarefa 6 — Página, detalhes, endereços e navegação

**Dependências:** tarefas 2–5. Integrar os botões somente quando os fluxos correspondentes existirem; não publicar ações sem funcionamento.

**Arquivos novos:** frontend/src/pages/Customers/CustomersPage.jsx, useCustomers.js, CustomerDetailsModal.jsx e CustomerAddressModal.jsx. **Alterações:** frontend/src/routes/{AppRoutes.jsx,navigation.js}, components/layout/AppLayout.jsx se precisar mapear ícone e src/index.css somente para apresentação localizada.

**Interfaces produzidas:** rota /customers protegida por customers.read, navegação Clientes; hook useCustomers controla busca/página/recarga; detalhes recebem customerId e callbacks; gerenciador de endereços recebe customerId/onClose. Permissões consultadas na sessão atual, sem nomes de cargo codificados.

- [ ] Montar listagem aprovada com cabeçalho e filtros compactos, tabela e Pagination existentes. Estado inicial: página 0, size 25, pesquisa NAME vazia, tipo/situação Todos. Oferecer 25/50/100; ao mudar filtro/tamanho reiniciar página de forma consistente.
- [ ] Aplicar debounce de 400 ms apenas à pesquisa textual e carregar pelo backend. Abortar chamada anterior e usar geração para impedir que resposta antiga atualize dados/erro/loading. Não filtrar somente content no navegador nem usar useLoad atual sem proteção de corrida; manter esse comportamento específico no hook de Clientes.
- [ ] Consumir totalElements/totalPages para contagem e navegação. Se inativação ou filtro deslocar o resultado para uma página inexistente, consultar a última válida uma vez; total zero permanece página inicial. Diferenciar lista sem cadastros, filtro sem resultados, carregamento e falha.
- [ ] Detalhes consultam o cliente e carregam endereços com estado de erro próprio, sem esconder dados já disponíveis por falha do endereço. Exibir nome da tabela e RecordMetadata; em 204 apresentar registro não encontrado e não abrir edição de uma linha antiga.
- [ ] Abrir edição/cadastro e refletir retornos/recarregamento sem conservar datas antigas. Ativar/inativar exige customers.update, confirmação e PATCH apenas de active. Não disponibilizar DELETE de cliente ou chamar esse endpoint pelo serviço da tela.
- [ ] Gerenciar endereços com tipos disponíveis, edição sem mudança de cliente, permissões por ação e confirmação de exclusão. Unicidade é garantida também pelo backend/banco; conflito concorrente preserva a edição e permite recarregar os tipos, sem afirmar exclusividade só pelo seletor.
- [ ] Evitar modais sobrepostos: fechar/suspender detalhes ao abrir edição/endereço e restaurar o contexto após concluir, sem perder o cliente selecionado. Resposta de um cliente não pode preencher o modal de outro.
- [ ] Preservar estrutura de navegação, ícones Lucide diretos e políticas de retorno seguro de rota. Ajustar navigation/safeReturn para reconhecer /customers sem liberar caminho arbitrário. Não expor edição a quem só consulta, nem exclusão de endereço sem customers.delete.
- [ ] Conferir rótulos, foco, estados disabled, rolagem e layout desde 320 px; datas ficam nos detalhes. Reutilizar ErrorAlert com retry na mesma linha quando couber e sem subtítulos genéricos.

**Critérios de conclusão:** percurso lista → cadastro/edição → endereço → detalhes → situação está representado com permissões, estados e paginação; nenhuma ação de cliente exclui fisicamente; loading/erro ou resposta antiga não altera outro registro.

## Tarefa 7 — Documentação, revisão e marco de validação

**Dependências:** tarefas 1–6.

**Arquivos futuros:** testeRequisicoesInsomnia.yaml; backend/README.md e frontend/README.md em trechos pertinentes; novo docs/superpowers/plans/2026-10-29-customers-progress.md. Não criar esse registro de progresso como se execução já tivesse ocorrido.

- [ ] Atualizar exemplos da coleção para Page<Customer>, query/searchField/page/size e removeBirthDate, mantendo aquisição de sessão/CSRF. Explicar complemento nullable, 204 de detalhe e que a tela não chama exclusão de cliente. Preservar credenciais e outras requisições; não imprimir ou substituir segredos da coleção.
- [ ] Revisar os consumidores existentes encontrados. A busca atual não foi referenciada diretamente nas suítes lidas nesta preparação; conferir novamente ao executar e informar se houver expectativas dependentes do array. Não criar/executar uma bateria de testes para esta tarefa por iniciativa própria.
- [ ] Comparar alterações com customer/address, removendo imports não usados e mantendo o estilo sem formatação global. Conferir os cinco focos, o uso de Boolean e as validações antes dos setters; não migrar estilo de injeção ou adicionar response DTO de negócio.
- [ ] Revisar diff e arquivos novos, registrando mudanças da API e limites da verificação. Checkboxes concluídos significam implementação/revisão documentada, não execução automática dos cenários.
- [ ] Preparar roteiro manual futuro: PF/PJ; endereço inicial opcional; complemento ausente/limpo; nascimento removido e conflito de flag; associação preservada/trocada/removida; leitura sem edição e sem prices.read; paginação com filtros; retorno atrasado; CEP inválido/inexistente/indisponível; datas de Funcionários/Cargos/Clientes.
- [ ] Registrar separadamente revisão estática, aplicação futura da V6, execução manual e testes/build/lint, quando explicitamente solicitados. Nenhum resultado antigo de autenticação comprova esta nova entrega. Não publicar ou declarar prontidão de produção com base em leitura.

**Critérios de conclusão:** artefatos refletem o estado real, mudanças de contrato são rastreáveis e nenhuma alegação de teste/funcionamento sem evidência. Próximo módulo operacional será detalhado separadamente após a entrega de Clientes.

## Ordem, paralelismo e pendências externas

Ordem sugerida: 1 → 2 → 3 → 4 → 5 → 6 → 7. Tarefas 1–2 compartilham CustomerService e devem ser sequenciais. Metadados/modal podem ser preparados separadamente dos contratos de backend; CEP/serviços podem avançar após definição das interfaces, sem escrita concorrente nos mesmos arquivos. Não iniciar agentes ou execução paralela neste planejamento.

ViaCEP não exige credencial do ERP nem configuração SMTP. A chamada HTTPS depende de acesso à internet/CORS do provedor e, na hospedagem futura, da política connect-src do site; indisponibilidade sempre mantém entrada manual. Não usar a tarefa para implantar frontend/backend ou alterar ngrok.

## Referências e estado deste plano

- [ViaCEP: formato, erro e preenchimento](https://viacep.com.br/).
- [Spring Data JPA: Specifications e paginação](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html).
- CustomerController/Service/Filter, customer/address e Page usados em convites/auditoria do checkout de 29/10/2026.

As páginas online podem apresentar versões posteriores ao framework local. Não atualizar dependências como consequência de ler a documentação.

Plano preparado para revisão; nenhuma tarefa executada. Aprovação do desenho e deste plano não substitui a autorização explícita para implementar, aplicar migrations, executar testes ou fazer commits. A preferência anterior é manter este chat para planejamento e encaminhar implementação autorizada para outro chat.
