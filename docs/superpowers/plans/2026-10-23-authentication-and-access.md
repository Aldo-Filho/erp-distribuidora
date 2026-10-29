# Autenticação e administração de acessos — plano técnico

> Execução futura: usar superpowers:executing-plans somente após solicitação explícita de implementação e revisão deste plano. Não iniciar execução, agentes, instalações, testes ou commits por estar lendo este arquivo. Checkboxes representam trabalho pendente.

**Objetivo:** entregar acesso seguro e administração de funcionários/cargos/sessões no ambiente local de uma instalação individual do ERP.

**Arquitetura:** Spring Security com sessão JDBC no PostgreSQL, política administrativa persistida, permissões resolvidas pelo backend e frontend React com API centralizada. Convites precedem a criação do usuário; novas estruturas são adicionadas por Flyway, sem multi-tenancy ou reescrita das migrations existentes.

**Stack:** Java 21, Spring Boot 4.1.0, JPA, PostgreSQL, Flyway, Argon2, React 19, Vite, Tailwind e lucide-react. Dependências novas são propostas, ainda não instaladas.

**Especificação:** [2026-10-23-authentication-and-access-design.md](../specs/2026-10-23-authentication-and-access-design.md). Ler junto: o documento distingue decisões confirmadas de propostas técnicas e define os casos de borda.

## Restrições globais

- Somente planejamento nesta entrega. Sem código, alteração de migrations/dependências/configurações, aplicação de SQL, testes/build/lint ou commits.
- Em execução futura, respeitar AGENTS.md, customer/address, injeção existente, DTOs records, mensagens em inglês e retorno de entidades nos CRUDs de negócio.
- Preservar todas as alterações locais, inclusive arquivos não rastreados. O Git atual contém pedidos, compras, usuários, preços, reservas, recebimentos e movimentos ainda não consolidados em commits.
- Não criar worktree baseada apenas em HEAD e perder os módulos locais. Decidir o checkout no início da execução, após nova inspeção do Git.
- Preservar bloqueio de UserService.update, grafos de recebimentos e accept-float-as-int=false; não reimplementar correções auditadas.
- Não adicionar tenant_id, seleção/cadastro público de empresa, JWT, WhatsApp, financeiro ou emissão fiscal neste plano.
- Testes automatizados, build e lint somente mediante solicitação explícita. Revisão e cenários descritos abaixo não autorizam executá-los.
- Não reformatar globalmente, apagar dados, editar V1/V2 ou executar Flyway repair. Sem estimativa de prazo.

## Foco da revisão

1. Logins concorrentes, desafio repetido e sessão revogada em requisição concorrente: limite não é ultrapassado nem a sessão ressuscitada.
2. Mutação de usuário, cargo ou concessões: permissões novas valem na próxima requisição e pelo menos um administrador permanece.
3. Token reenviado, consumido simultaneamente ou entregue com falha SMTP: consumo único, estado rastreável e ausência de segredos nos logs.
4. CSRF após login/logout, aba em segundo plano, polling e política alterada: sessão segue o relógio do servidor e não expira pelo motivo errado.
5. Retry de recebimento com ator histórico inativo, preço manual e dados aninhados: autenticação nova não perde idempotência e permissão de tela não deixa escapar operações/campos proibidos.

## Interfaces HTTP propostas

Rotas abaixo são relativas ao backend. O navegador usa o prefixo /api pelo proxy local. Requisições que alteram estado exigem CSRF; ser rota pública não dispensa o token. Todas as respostas de segurança são no-store. Datas públicas usam ISO-8601 com offset UTC.

| Método e rota | Entrada e autorização | Saída e comportamento |
| --- | --- | --- |
| GET /auth/csrf | Público, sujeito a limite | 200, token CSRF e nome do header; cria contexto anônimo se necessário, sem vaga autenticada. |
| POST /auth/login | email/password, público com CSRF | 200 com SessionView e cookie; 401 genérico; 409 com LoginChallengeView quando faltam vagas; 429 em contenção. |
| POST /auth/sessionReplacement | challengeToken e sessionId público, contexto pré-autenticado com CSRF | 200 com SessionView/cookie; 409 para escolha concorrente; 400 para desafio inválido/expirado, sem liberar operações. |
| GET /auth/session | Sessão válida | 200 com usuário/cargo/permissões atuais, política, publicSessionId e prazos; não renova atividade interativa; 401 se ausente. |
| POST /auth/activity | Sessão válida e CSRF | 204, atualiza somente a atividade interativa; não renova duração absoluta. |
| POST /auth/logout | CSRF; idempotente para contexto atual | 204, invalida sessão/contexto e cookie. |
| GET /auth/sessions | Sessão válida | 200, metadados mínimos das próprias sessões válidas; sem identificador de autenticação. |
| DELETE /auth/sessions/{id} | Dono da sessão e CSRF | 204; sessão alheia/ausente não revela existência. A própria sessão pode ser encerrada. |
| PATCH /auth/password | currentPassword/newPassword, sessão e CSRF | 204, altera senha, revoga sessões e exige novo login. |
| POST /auth/passwordRecovery | email, público com CSRF | 202 genérico independentemente da existência da conta; envio limitado. |
| POST /auth/passwordReset | token/newPassword, público com CSRF | 204 após consumo; 400 genérico para token inválido/expirado/utilizado. |
| POST /auth/invitationAcceptance | token/password, público com CSRF | 201, usuário criado; não realiza login automático; 400 para link sem validade. |
| GET /user/invitation/search, POST /user/invitation, POST /user/invitation/{id}/resend, DELETE /user/invitation/{id} | access.users.manage e CSRF nas escritas | Listar paginado, criar 201, reenviar 202 e cancelar 204; nunca retornar token. |
| POST /user/{id}/passwordRecovery | access.users.manage e CSRF | 202, inicia e-mail sem expor token ou permitir senha escolhida pelo administrador. |
| GET /user/{id}/sessions; DELETE /user/{userId}/sessions/{id} | security.sessions.manage; CSRF na escrita | Listagem paginada e revogação dentro do usuário informado. |
| GET/PATCH /security/settings | security.settings.manage; CSRF no PATCH | 200, política atual/editada; mudança é auditada e afeta sessões existentes. |
| GET /security/audit/search | security.audit.read | 200 paginado, filtros por período/ator/ação/resultado; sem payloads ou segredos. |

SessionView e LoginChallengeView são records de transporte de segurança, não DTOs de resposta dos módulos de negócio. SessionView expõe userId, name, email, roleId, roleName, permissionKeys, publicSessionId, serverTime, lastActivityAt, idleExpiresAt, absoluteExpiresAt e policy. LoginChallengeView expõe código SESSION_LIMIT_REACHED, token temporário e sessões do próprio usuário com IDs públicos, identificação resumida do navegador e horários. Não expor hash, cookie, token de sessão, IP completo ou atributos internos serializados.

Erros fora do conflito de login usam ResponseErrorDTO. O conflito de login tem contrato próprio documentado acima para permitir escolha; filtros não dependem de @RestControllerAdvice para escrever JSON. Preservar 200/201/204 dos endpoints de negócio e o 204 de findOne ausente.

## Tarefa 1 — Persistência e configuração local

**Depende de:** revisão das propostas da especificação; nenhuma implementação anterior deste plano.

**Arquivos futuros:** backend/pom.xml; backend/src/main/resources/application.yaml; novos application-local.yaml e application-production.yaml na mesma pasta; backend/src/main/resources/db/migration/V3__create-authentication-and-access.sql; sob backend/src/main/java/com/pi/erp/auth/, novos config/AuthenticationProperties.java, settings/SecuritySettings.java, settings/SecuritySettingsRepository.java, audit/SecurityAuditEvent.java, audit/SecurityAuditRepository.java e audit/SecurityAuditService.java.

**Contrato produzido:** estruturas persistentes e propriedades usadas pelas tarefas 2–7. Prefixo das propriedades: erp.auth para URL pública, remetente, limites e bootstrap; segredos SMTP/datasource vêm do ambiente.

- [ ] Conferir as versões JDBC/Security/Mail gerenciadas pelo Boot 4.1.0; acrescentar os starters correspondentes sem atualizar o framework e manter crypto/BouncyCastle existentes. Não instalar frontend nesta tarefa.
- [ ] Descrever a V3 com tabelas padrão SPRING_SESSION/SPRING_SESSION_ATTRIBUTES e metadados user_sessions: id público UUID, vínculo único à sessão JDBC, user_id, authenticated_at, last_activity_at e credential_version. Definir índices de usuário e validade e remoção compatível com a limpeza do JDBC.
- [ ] Acrescentar users.credential_version não nulo com zero para existentes; índice único de e-mail normalizado, com precondição que falha em colisão sem apagar dados.
- [ ] Acrescentar security_settings com linha única, tempos, máximo simultâneo, versão da política e updated_at/updated_by; essa linha também será a guarda de mutações de acesso. Acrescentar user_invitations, password_reset_tokens e login_challenges com hashes únicos, validade e consumo/cancelamento. Convite tem e-mail, nome, cargo/criador; recuperação/desafio têm usuário/versão; desafio vincula contexto anônimo.
- [ ] Acrescentar security_audit_events com ator opcional, ação, alvo, resultado, occurred_at, correlation_id e mudanças permitidas mascaradas. Sem edição/exclusão de auditoria na API; integridade do histórico não depende de manter o usuário físico indefinidamente.
- [ ] Criar o modelo/repository da política e a base de escrita de auditoria antes de seus consumidores. SecuritySettingsRepository expõe obtenção da linha de controle com bloqueio; SecurityAuditService registra sucesso na transação principal e falha após rollback em transação própria. Consulta HTTP de auditoria permanece na tarefa 6.
- [ ] Configurar Flyway como criador único dessas tabelas; perfis local HTTP e produção HTTPS separados. Manter open-in-view=false, ddl-auto=validate e rejeição de floats em inteiros. Remover credenciais fixas da configuração futura, sem imprimir valores e sem editar arquivos de segredos do usuário.

**Conclusão verificável:** definição de tabelas/constraints/propriedades coerente com a versão do framework e com User atual; nenhuma tabela de tenant; plano de colisões/checksums explícito. Aplicação de migration e validação ORM ficam pendentes de execução autorizada.

## Tarefa 2 — Catálogo de permissões e proteção da administração

**Depende de:** tarefa 1.

**Arquivos futuros:** backend/src/main/resources/db/migration/V4__register-access-permissions.sql; novos PermissionKey.java e AccessAdministrationService.java em backend/src/main/java/com/pi/erp/auth/; alterações em user/UserService.java, UserRepository.java, UserController.java, config/role/RoleService.java, RoleRepository.java, config/RolePermissionService.java, config/permission/PermissionController.java e PermissionRepository.java, todos sob backend/src/main/java/com/pi/erp/.

**Contrato produzido:** PermissionKey contém as chaves executáveis. AccessAdministrationService coordena escritas com a guarda e verifica o administrador remanescente. Preservar serviços de domínio por funcionalidade; a guarda não substitui suas validações.

| Modelos de cargo | Concessões iniciais propostas |
| --- | --- |
| Administrador | Todas as permissões executáveis nesta entrega, inclusive gestão de acesso e segurança. |
| Vendas | Leitura de clientes, produtos, tabelas de preço e estoque; gestão de clientes e pedidos; confirmação/cancelamento; preço manual e desconto. Sem saída física, compras ou segurança. |
| Compras | Consulta de produtos/estoque e gestão de fornecedores/compras, envio/confirmação/cancelamento. Sem recebimento físico ou administração. |
| Estoque | Consulta de produtos/depósitos/estoque, leitura de pedidos/compras e históricos; recebimento, saída e movimento/ajuste manual. |
| Financeiro | Inicialmente sem permissões financeiras executáveis porque o módulo ainda não existe. Pode entrar, gerenciar a própria sessão e aguardar habilitação das telas futuras. |

- [ ] Registrar chaves reais e esses modelos uma vez pela V4; não promover cargos fictícios da V2 e não sobrescrever concessões personalizadas em cada inicialização. Permissões financeiras/fiscais só entram com seus módulos; não criar telas/handlers fictícios.
- [ ] Tornar o catálogo somente leitura na API, com POST/PATCH/DELETE /permission retornando 405 para autenticados; concessões continuam nos vínculos do cargo. GET do catálogo exige access.roles.manage. Identificar permissões por chave, nunca pelo nome/ID presumido do cargo.
- [ ] Manter administração de usuários/cargos e concessões sob permissões explícitas, guarda única e transação; impedir perda do último usuário/cargo ativo com access.users.manage, access.roles.manage e security.settings.manage conjuntamente. A instalação vazia aguarda bootstrap; isso não abre uma exceção na API pública.
- [ ] Desabilitar POST /user para cadastro direto de senha e encaminhar o fluxo de criação para convite. Rejeitar password no PATCH administrativo sem ignorar a entrada. Preservar o bloqueio de atualização de usuário. Os pontos de revogação de sessão são conectados na tarefa 3, e os de token na tarefa 4, sem stubs que anunciem revogação antes de implementá-la.
- [ ] Consultar permissões atuais a cada requisição, sem manter concessões antigas no principal JDBC. Cargos inativos bloqueiam acesso. Alteração de cargo/concessões vale na próxima requisição e é auditada.

**Conclusão verificável:** qualquer caminho de alteração de usuário/cargo/concessão participa da guarda e da verificação do último administrador; nenhuma entrada de senha administrativa contorna convite/recuperação; cinco cargos são editáveis, sem poderes associados ao nome.

## Tarefa 3 — Login, política e sessões persistentes

**Depende de:** tarefas 1–2.

**Arquivos novos sob backend/src/main/java/com/pi/erp/auth/:** AuthenticationController.java, AuthenticationService.java, CurrentUser.java, ErpPrincipal.java, RequestLoginDTO.java, SessionView.java; config/SecurityConfig.java, SecurityErrorHandler.java; session/UserSession.java, UserSessionRepository.java, UserSessionController.java, UserSessionService.java, SessionPolicyFilter.java, LoginChallenge.java, LoginChallengeRepository.java, LoginChallengeView.java, RequestSessionReplacementDTO.java; settings/SecuritySettingsController.java, SecuritySettingsService.java, PatchSecuritySettingsDTO.java. Conectar revogação nos services existentes de usuário/cargo e reutilizar os modelos de política/auditoria da tarefa 1.

**Contrato produzido:** CurrentUser.requireId devolve exclusivamente o ID autenticado ou falha com 401. UserSessionService coordena admissão, substituição, validade e revogação. AuthenticationService valida credenciais e retorna SessionView ou LoginChallengeView. Controllers delegam regras aos services.

- [ ] Implementar CSRF, cookie e respostas JSON do contrato HTTP; persistir o contexto e regenerar ID/CSRF no login, incluindo login customizado e Spring Security da versão local. Evitar redirecionamento HTML.
- [ ] Verificar credenciais fora da guarda; sob guarda revalidar hash/versão e usuário/cargo antes de criar sessão persistida e lastLoginAt. Não realizar Argon2 nem SMTP com o bloqueio global adquirido.
- [ ] Implementar contagem de sessões logicamente válidas, desafio pré-autenticado e substituição atômica conforme a especificação. Persistir a admissão antes de liberar a guarda e impedir restauração por requisição concorrente antiga.
- [ ] Reutilizar a vaga no login repetido da mesma conta/contexto; troca de conta exige logout. Conectar revogação de sessões/desafios em inativação de usuário/cargo e alteração de e-mail, com incremento da versão de credenciais pertinente. Reativação não restaura sessões anteriores.
- [ ] Aplicar política atual, credential_version e permissões em cada requisição. Relógio interativo separado do acesso automático, com /auth/activity limitado; renovações não alteram authenticatedAt. Rejeitar sessões inválidas mesmo antes da limpeza JDBC.
- [ ] Implementar listagem/revogação própria e administrativa, sem expor credencial; validar ownership também na rota aninhada de usuário. Logout limpa cookie/contexto; alteração própria de senha será ligada na tarefa 4.
- [ ] Validar PATCH da política nas faixas propostas, sob guarda e auditoria. Mostrar impacto ao reduzir limites e revogar excedentes de forma determinística; não restaurar sessões por aumento posterior.

**Conclusão verificável:** contrato cobre dois navegadores, terceira tentativa, dois logins simultâneos, desafio repetido, token CSRF renovado, polling, sessão revogada e política reduzida/aumentada. Contagem não depende de job de limpeza e usuário inativo não faz replay autenticado usando sessão antiga.

## Tarefa 4 — Convites, recuperação e bootstrap

**Depende de:** tarefas 1–3.

**Arquivos novos sob backend/src/main/java/com/pi/erp/auth/:** invitation/UserInvitation.java, UserInvitationRepository.java, UserInvitationController.java, UserInvitationService.java, RequestUserInvitationDTO.java, UserInvitationFilter.java, RequestInvitationAcceptanceDTO.java; password/PasswordRecoveryController.java, PasswordRecoveryService.java, PasswordResetToken.java, PasswordResetTokenRepository.java, RequestPasswordRecoveryDTO.java, RequestPasswordResetDTO.java, PatchPasswordDTO.java; mail/AccessMailService.java; bootstrap/AdministratorBootstrap.java. Alterar UserService.java/RequestUserDTO.java/PatchUserDTO.java e os repositories de acesso apenas nas responsabilidades necessárias.

**Contrato produzido:** convites/reenvios/consumos e recuperação conforme tabela HTTP. AccessMailService envia somente para SMTP configurado; fluxo HTTP público nunca devolve token. Bootstrap é procedimento local de implantação, sem endpoint público permanente.

- [ ] Criar convites separados do usuário, hashes/validades e consulta administrativa paginada. Reenvio/cancelamento/aceitação concorrem sob a mesma coordenação e invalidam tokens anteriores. Aceitação valida cargo ativo e ausência do e-mail antes de criar User com Argon2.
- [ ] Implementar normalização de e-mail consistente com o índice proposto; manter senhas sem trim/truncamento e aplicar política nova apenas na definição/alteração. Duas aceitações não podem produzir dois usuários.
- [ ] Implementar recuperação genérica, consumo único, alteração própria com senha atual e revogação das sessões/tokens/desafios na mesma transação de senha. Registrar auditoria sem token/hash e exigir novo login após sucesso.
- [ ] Enviar e-mails após commit, com URLs de base configurada, validade no texto e sem credenciais em logs. Falhas retornam fluxo público genérico e deixam evento operacional para investigação/reenvio; não reverter senha por falha de notificação.
- [ ] Criar primeiro administrador somente em modo de bootstrap explicitamente ativado pela implantação e sem administrador existente. Receber roleId explicitamente e verificar as permissões necessárias, sem inferir autoridade pelo nome do cargo; receber senha por entrada interativa protegida, nunca por argumentos de processo, arquivo versionado ou senha padrão. Desativar o modo após uso. Não ativar usuários fictícios da V2.

**Conclusão verificável:** convite válido cria um usuário, reenvio invalida link anterior, cargo inativado impede aceite, token usado/expirado não altera senha, SMTP não retém locks e bootstrap não vira acesso público. Provedor/caixa SMTP será configurado na execução; não simular sucesso de entrega.

## Tarefa 5 — Proteger todas as operações existentes e obter o operador da sessão

**Depende de:** tarefas 2–4.

**Arquivos futuros:** todos os controllers existentes sob backend/src/main/java/com/pi/erp/{customer,supplier,product,warehouse,stock,price,order,purchase,user}; OrderService.java, order/item/OrderItemService.java, order/dispatch/OrderDispatchService.java e RequestOrderDispatchDTO.java; purchase/receipt/PurchaseReceiptService.java e RequestPurchaseReceiptDTO.java; stock/movement/StockMovementService.java e RequestStockMovementDTO.java; auth/PermissionKey.java e config/SecurityConfig.java; testeRequisicoesInsomnia.yaml apenas para documentar os contratos alterados, sem executar requisições.

**Contrato produzido:** negação por padrão e matriz abaixo. Para cada família CRUD existente, GET exige .read, POST/PATCH/DELETE a ação correspondente; subrotas e ações compostas recebem regra explícita. URLs atuais permanecem, inclusive /price-table e endpoints sem /search.

| Rotas atuais | Chaves de permissão propostas |
| --- | --- |
| /customer e /customerAddress | customers.read/create/update/delete; endereço usa a ação equivalente. |
| /supplier, /supplierAddress, /supplier/{id}/brands e /categories | suppliers.read/create/update/delete; vincular/desvincular marca/categoria exige suppliers.update. |
| /productSupplier | suppliers.read para consulta; suppliers.update para escrita. |
| /product, /brand, /category | products.read/create/update/delete conforme as operações existentes; brand/category sem PATCH continuam sem PATCH. |
| /warehouse e /warehouseAddress | warehouses.read/create/update/delete. |
| /stockItem | stock.read; stock.configure para cadastrar/configurar/excluir saldo sem histórico, respeitando regras atuais. |
| /stockMovement | stock.read para consulta; stock.adjust para movimento manual. SALE/PURCHASE continuam exclusivos de coordenadores comerciais. |
| /price-table e /priceTableItem | prices.read/create/update/delete. |
| /order e /orderItem | orders.read/create/update/delete; transições exigem também orders.confirm ou orders.cancel; saída exige orders.dispatch. |
| /purchase e /purchaseItem | purchases.read/create/update/delete; transições exigem também purchases.send/confirm/cancel. |
| /purchase/{id}/receive e /receipts | purchases.receive para novo recebimento ou retry; purchases.read para histórico. |
| /user, convites e redefinição administrativa | access.users.manage; GET de usuários também aceita security.sessions.manage para selecionar alvo de revogação. Operações próprias de conta ficam no auth. |
| /role e /role/{id}/permissions; GET /permission | access.roles.manage; GET /role e /role/search também aceitam access.users.manage para escolher cargo de um convite/usuário, sem conceder escrita de cargos. |
| Configuração, sessões de terceiros e auditoria | security.settings.manage, security.sessions.manage e security.audit.read, respectivamente. |

- [ ] Inventariar cada método HTTP real e conferir cobertura por default-deny; somente rotas públicas expressas na tabela de autenticação ficam liberadas, com CSRF/rate limit. Não liberar /auth/** inteiro.
- [ ] Aplicar permissões no servidor também às entradas aninhadas/compostas. Produto cadastrado com estoque inicial exige stock.configure além de products.create. Cliente/fornecedor com endereço usa a mesma ação de cadastro; itens exigem autorização da operação principal. Chamadas de coordenadores a services não são endpoints públicos.
- [ ] Em pedidos, exigir orders.manualPrice quando unitPrice explícito é informado e orders.discount quando desconto é criado/alterado; manter descontos/preços existentes quando omitidos no PATCH. Reprecificação automática pelo produto/tabela não é preço manual. Responsável comercial pode ser informado conforme permissão de edição, sem alterar autoria do evento.
- [ ] Remover obrigatoriedade dos campos legados de operador nos DTOs de movimentos/saída/recebimento e adaptar resolução no service. Novo efeito usa CurrentUser.requireId; valor legado divergente retorna 400. Responsável em Order/Purchase continua distinto.
- [ ] Adaptar o replay de recebimento conforme a especificação: autorizar antes, resolver operador histórico para lote já existente, comparar conteúdo sem sobrescrever ator e preservar requestId, locks, grafos LOAD e ausência de efeitos duplicados. Não reabrir a correção de serialização nem exigir operador histórico ativo para consultar o replay autorizado.
- [ ] Revisar dados retornados em associações: produtos e catálogos de fornecedor expõem custos. Nesta entrega, products.read/suppliers.read incluem esses custos; não anunciar sigilo de margem/custo por cargo sem implementar projeção/mascaramento em todos os caminhos. Registrar a limitação para revisão do usuário; não recriar DTOs de resposta de negócio silenciosamente.
- [ ] Atualizar a coleção Insomnia com obtenção de CSRF/login/sessão e autoria inferida, documentando 401/403/405 e mudanças de contrato. Não executar coleção ou scripts antigos automaticamente.

**Conclusão verificável:** nenhum endpoint ou campo composto contorna a matriz; token/ID de operador não permite personificação; CRUD de usuário/permissão não contorna os fluxos novos; retry idêntico preserva saldo e ator histórico; regras comerciais, status e respostas ausentes permanecem.

## Tarefa 6 — Contenção e auditoria de acesso

**Depende de:** tarefas 1–5; auxiliares usados no login devem ser conectados antes de expor a primeira entrega.

**Arquivos novos sob backend/src/main/java/com/pi/erp/auth/:** audit/SecurityAuditController.java, audit/SecurityAuditFilter.java; protection/AuthenticationThrottleService.java, protection/AuthenticationThrottleFilter.java. Reutilizar a escrita de auditoria da tarefa 1; ajustar SecurityErrorHandler.java, AuthenticationProperties.java e os fluxos administrativos para emitir eventos.

**Contrato produzido:** limitação com 429/Retry-After, espera progressiva e eventos paginados; parâmetros da especificação são propostas iniciais, não garantias de resistência a ataque.

- [ ] Limitar login por conta/IP, recuperação, convites e concorrência Argon2, antes de trabalho caro e sem sleeps que ocupem workers. Evitar enumerar contas por resposta/status/tempo e limitar memória/cardinalidade das chaves anônimas.
- [ ] Validar IP remoto conforme a implantação; local não confia em headers encaminhados arbitrários. Túnel/proxy futuro terá lista explícita de intermediários confiáveis.
- [ ] Auditar sessões, convites, recuperação, alterações de usuário/cargo/permissões e política. Eventos de sucesso devem corresponder ao commit; falhas não podem desaparecer apenas pelo rollback da operação principal.
- [ ] Filtrar senha, hash, token, cookie e parâmetros de link; evitar corpos completos de requisição/resposta. Restringir auditoria e validar período/paginação com 25 por padrão e máximo 100.
- [ ] Separar política de sessão editável pelo administrador dos limites de infraestrutura/abuso mantidos pela implantação. Não anunciar bloqueio volumétrico pelo rate limiter da aplicação.

**Conclusão verificável:** contenção considera NAT, erros são JSON, tentativas falhas têm registros sanitizados e a consulta de auditoria não expõe segredos. Retenção de auditoria e alertas de produção ficam no roteiro operacional, sem excluir registros automaticamente nesta entrega.

## Tarefa 7 — Frontend de acesso e administração

**Depende de:** contratos 1–6; estrutura visual pode ser desenhada em paralelo após revisão das rotas.

**Arquivos futuros:** frontend/package.json e package-lock.json para roteador compatível quando instalação for autorizada; frontend/index.html e src/index.css; novos src/main.jsx, App.jsx, routes/AppRoutes.jsx, routes/ProtectedRoute.jsx, services/api.js, services/auth.js, services/access.js, services/security.js; auth/AuthProvider.jsx, useAuth.js, SessionActivity.jsx; components/layout/AppLayout.jsx, components/feedback/ErrorAlert.jsx, components/feedback/SessionExpiryDialog.jsx, components/feedback/SessionReplacementDialog.jsx; pages/Auth/LoginPage.jsx, InvitationAcceptancePage.jsx, PasswordRecoveryPage.jsx, PasswordResetPage.jsx; pages/Access/UsersPage.jsx, RolesPage.jsx, InvitationsPage.jsx; pages/Security/SessionsPage.jsx, SecuritySettingsPage.jsx, SecurityAuditPage.jsx; pages/Account/AccountPage.jsx. Paths a partir de frontend/src nas famílias sem o prefixo explícito.

**Contrato produzido:** cliente fetch centralizado com credentials/CSRF, contexto de autenticação, rotas e páginas abaixo; nenhum cookie de sessão acessível ao JavaScript nem credencial persistida no navegador.

- [ ] Restabelecer entrada React, identidade ERP Distribuidora e idioma pt-BR, aproveitando Tailwind/Lucide existentes. Retirar título e script de demonstração do HTML ao preparar essa tela, sem apagar estrutura ou dependências. Não redesenhar módulos operacionais nesta tarefa.
- [ ] Definir rotas públicas /login, /accept-invitation, /forgot-password e /reset-password; privadas /account, /access/users, /access/roles, /access/invitations, /security/sessions, /security/settings e /security/audit. Abrir a primeira rota autorizada após login, com /account como alternativa funcional.
- [ ] Liberar /security/sessions para todo usuário autenticado consultar/revogar suas próprias sessões; habilitar seleção de outro usuário somente com security.sessions.manage. Leitura de cargos por gestor de usuários alimenta seletores sem liberar a tela de edição de cargos.
- [ ] Carregar sessão antes de liberar rotas; manter permissões atuais no contexto e atualizá-las a partir do servidor. GET /auth/session não mantém atividade. Expiração redireciona ao login com retorno interno validado; 403 apresenta acesso negado, sem loop de login.
- [ ] Implementar obtenção/renovação de CSRF antes de mutações e após login/logout. Não repetir automaticamente gravações por 401/403/timeout; permitir nova tentativa consciente sem duplicar efeitos. Token CSRF não é token de autenticação.
- [ ] Implementar login, limite de sessões e escolha de substituição. Nenhum menu privado fica disponível no estado de desafio; desafios e tokens de URL ficam apenas em memória e são limpos ao sair/consumir.
- [ ] Implementar convites, recuperação, definição/alteração de senha, usuários sem campo administrativo de senha, cargos com checkboxes agrupados e configurações com validações iguais às do backend. Reenvio informa solicitação de envio, não entrega confirmada.
- [ ] Implementar sinal interativo limitado e aviso com cinco minutos restantes. Continuar renova somente inatividade, se sessão ainda válida e prazo absoluto não atingido. Sincronizar logout/mudança de sessão entre abas; relógio visual usa serverTime e não decide validade.
- [ ] Proteger formulários contra perda silenciosa: manter estado em memória quando possível, avisar em expiração e nunca persistir senhas/tokens. Não prometer restauração de rascunhos após recarregar o navegador. Erro com Tentar novamente na mesma linha quando couber, estados de vazio/carregamento e responsividade desde 320 px.

**Conclusão verificável:** percurso administrador → convite → funcionário → permissão → sessão/revogação está representado integralmente; interface objetiva, sem dados falsos, botões sem ação ou módulos ainda ausentes. Verificação visual e HTTP permanecem pendentes até execução autorizada.

## Tarefa 8 — Revisão final e passagem para o próximo módulo

**Depende de:** tarefas 1–7.

**Arquivos de documentação futuros:** docs/superpowers/plans/2026-10-23-authentication-and-access-progress.md; backend/README.md e frontend/README.md para configuração local e contratos, sem credenciais. A criação deste plano não cria o arquivo de progresso nem marca implementação como feita.

- [ ] Revisar o diff e comparar annotations, services, DTOs e controllers alterados com customer/address. Justificar DTOs de transporte de segurança e os poucos services coordenadores específicos; não migrar o estilo de injeção nem remover validações de domínio.
- [ ] Revisar os cinco focos do início, os contratos 401/403/405 e os endpoints diretos/aninhados, incluindo serializers que podem expor campos de associações. Preservar literalmente o formato ResponseErrorDTO fora do desafio específico.
- [ ] Registrar o que foi implementado, revisado estaticamente e não executado, com riscos/limitações. Não somar cenários documentados como testes aprovados nem reutilizar as 541 verificações antigas como evidência da autenticação nova.
- [ ] Preparar roteiro manual futuro: primeiro administrador, convite/aceite, dois navegadores, terceira tentativa, permissão revogada, expiração sem polling, recuperação e replay de recebimento autorizado. Não executar por padrão; testes automatizados/build/lint só quando explicitamente solicitados.
- [ ] Antes de considerar produção, programar validação executada de migrations, persistência/serialização de sessões, SMTP, concorrência, CSRF e UI; acrescentar HTTPS, backup/restauração e revisão de segurança da infraestrutura. Uma revisão estática local não substitui esses marcos.

**Conclusão verificável:** documentação reflete o estado real; nenhuma alegação de autenticação pronta se só houve planejamento/revisão; próxima entrega é cadastros/preços nas telas, sem WhatsApp ou publicação automática.

## Ordem e trabalho paralelo possível

1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 é a sequência de revisão. Componentes de contenção/auditoria da tarefa 6 precisam acompanhar a exposição dos fluxos 3–4, não esperar uma publicação intermediária insegura. Interfaces visuais podem ser preparadas após os contratos, mas a integração e o aceite dependem do backend.

Não iniciar tarefas paralelas/agentes neste planejamento. Durante execução futura autorizada, catálogo/documentação e desenho visual podem ocorrer em paralelo, preservando os contratos e evitando escrita concorrente nos mesmos arquivos.

## Revisão necessária antes de implementar

As regras funcionais vieram das decisões da conversa. As faixas administrativas, duração dos tokens/desafio, política de senha, concessões iniciais, coordenação de concorrência e mudanças explícitas dos endpoints são propostas técnicas deste documento. O usuário deve revisar essas escolhas antes da implementação. Configuração SMTP e hospedagens não são escolhidas por este plano; basta configurar uma caixa de teste para a fase local.

Preparar este plano não altera o limite vigente de somente análise e planejamento. Nenhuma tarefa está executada. A revisão documental não equivale à autorização de implementação.
