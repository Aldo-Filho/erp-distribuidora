# Progresso — autenticação e administração de acessos
Plano: docs/superpowers/plans/2026-10-23-authentication-and-access.md

Autorização: implementação local no checkout atual, preservando alterações anteriores.
Sem testes, build, lint, aplicação SQL, commits, push ou publicação.

Ruling: manter o checkout atual explicitamente autorizado; worktree a partir do HEAD perderia a base não consolidada.
Ruling: substituir gates de testes/commits da skill por revisão direta e registro de limites, conforme instrução expressa.
Pre-flight: política/guarda → administração, admissão, tokens; mesmo bloqueio antes de mutações.
Pre-flight: catálogo → matriz HTTP e verificações comerciais; chaves compartilhadas por migration e código.
Pre-flight: sessão → autoria; replay de recebimento mantém o ator histórico.
Pre-flight: contratos HTTP → frontend; CSRF renovado após autenticação e logout.
Ruling: estruturas operacionais novas de segurança usam JDBC explícito em repositories/services próprios. As entidades e repositories JPA de User/Role continuam na organização existente. Isso permite controlar guarda, consumo, filtros de auditoria e tombstones sem serializar grafos JPA na sessão; não foi introduzida abstração genérica nem DTO de resposta de negócio.
Ruling: envio é assíncrono em executor limitado após commit. Além de retirar SMTP dos locks, impede que a latência SMTP diferencie recuperação de e-mail existente/inexistente. A fila é volátil, sem garantia de entrega após reinício; PENDING/FAILED permitem investigação/reenvio.
Ruling: bootstrap bloqueia APIs enquanto habilitado e encerra deliberadamente o processo após criar o administrador. Exige remover a flag e reiniciar; não deixa uma aplicação operacional com modo de bootstrap ativo.
Ruling: sessão anônima cuja identificação foi rotacionada por requisição concorrente é recusada antes de escrever contexto; o cliente pode obter contexto atual e tentar conscientemente. Não duplicar admissão nem sobrescrever um contexto novo com snapshot antigo.

## Registro por tarefa

- Tarefa 1: arquivos implementados — starters gerenciados, V3, perfis, segredos por ambiente, política/guarda e auditoria. BOM local lido: Security 7.1.0, Session 4.1.0. V1/V2 preservadas no estado local anterior. Nenhuma migration aplicada, ORM/compilação não verificados.
- Tarefa 2: arquivos implementados — catálogo executável/V4 com cargos novos sem promover fixtures; guarda e verificação de administrador após flush em User/Role/RolePermission; catálogo read-only e POST /user 405; password administrativo recusado. Permissões fictícias não são executáveis nem aparecem no catálogo de concessões.
- Tarefa 3: arquivos implementados — CSRF, login, rotação, contexto mínimo com persistência imediata, desafio pré-autenticado, troca, consulta/atividade/revogação, política e expiração. Sessões exigem identidade/versão e admissão lógica; tombstones impedem acesso com contexto antigo. Concorrência analisada por leitura, não comprovada em execução.
- Tarefa 4: arquivos implementados — convites/reenvio/cancelamento/aceite, recuperação, alteração própria, normalização, consumo único, revogação e bootstrap protegido. SMTP não configurado/executado; links não retornam na API nem são registrados no console.
- Tarefa 5: arquivos implementados — matriz central e checagens de estoque inicial/preço manual/desconto/transições; operador da sessão e legado opcional; replay preserva ator histórico, requestId, locks e grafos LOAD. Inventário de handlers no arquivo authentication-and-access-endpoints.md. Insomnia ganhou CSRF/login/sessão/convites e headers de escrita; nenhuma coleção executada.
- Tarefa 6: arquivos implementados — contenção limitada por IP/identidade, espera progressiva, cardinalidade expiráveis, concorrência Argon2 e corpo pequeno antes do custo; eventos sanitizados e busca paginada. Polling/activity não consomem limite de autenticação por IP. Sem promessa de defesa volumétrica ou retenção legal.
- Tarefa 7: arquivos implementados — entrada React, router, cliente /api, sessão/permissões, rotas/telas de conta e administração, convites, recuperação, desafio, aviso e atividade interativa, sinais entre abas e estados de erro. npm install react-router-dom executado com aprovação de rede; package.json/lockfile atualizados. Sem execução do frontend, navegação visual, build, lint ou testes.
- Tarefa 8: documentação e inventário escritos; revisão direta e uma revisão independente concluídas. Ajustes da revisão aplicados e examinados diretamente, com limites de validação registrados abaixo. Nenhum commit/push/deploy.

## Revisão direta realizada

Comparados customer/CustomerService, CustomerController, RequestCustomerDTO e address/CustomerAddressService com os services/controllers tocados: mantidos campos @Autowired, funcionalidades em pacotes, records de entrada, fluxo de validação antes de persistência e contratos 200/201/204 de negócio. Novos records de saída são exclusivos de segurança.

Verificados por leitura: último administrador com permissões conjuntas por usuário/cargo ativo; JDBC vê alterações JPA após flush; custos/associações continuam expostos conforme contrato; senha/hash e versão do usuário ignorados no JSON; UserSession não serializa sessionId/userId/versão/revogação; tokens persistem somente como hash; troca de sessão não concede acesso antes da admissão; IP usa socket remoto; bootstrap não expõe endpoint público.

Preservadas as correções anteriores: findByIdForUpdate de UserService.update, transação, valores finais de PATCH, grafos LOAD de recebimentos e accept-float-as-int=false. O diff contra HEAD contém alterações de módulos e V1/V2 anteriores a este chat; não é um diff exclusivo desta entrega.

## Estado antes da autorização de testes

Não foram criados/executados testes automatizados, Maven/compilação, build/lint, aplicação backend/frontend, SQL/Flyway, demonstração visual ou SMTP. Os cenários descritos não são testes aprovados. Não usar verificações antigas do backend como evidência desta autenticação.

O npm executou auditoria de dependências como parte da instalação. Uma consulta npm audit --json identificou um aviso de severidade alta no Vite 8.3.0–8.3.2 já existente (incluindo GHSA-587h-7wmj-h5pm). O roteador não foi apontado. Não foi executado audit fix nem atualização geral do Vite; esta dependência exige ajuste/validação em etapa própria.

Configurações ainda necessárias: PostgreSQL por ambiente, revisão de histórico/checksums/colisões antes de aplicar V3/V4, caixa SMTP e remetente, URL-base confiável e primeiro administrador em console protegido. Backend: dependências declaradas, mas resolução/compilação não executadas.

## Roteiro manual futuro — requer autorização para executar

1. Preparar cópia segura do banco e conferir Flyway/colisões; aplicar V3/V4 sem repair automático e conferir constraints/contagens/mapeamento.
2. Bootstrap por console protegido, com cargo explícito; conferir encerramento do processo, desativação da flag e ausência de promoção de fixtures.
3. Obter CSRF, entrar, conferir rotação/cookie/no-store e consultar sessão; entrar duas vezes no mesmo navegador sem ocupar vaga nova.
4. Usar dois contextos de navegador; no terceiro, validar senha, escolher sessão e conferir revogação/consumo único. Repetir em paralelo, inclusive no mesmo contexto anônimo.
5. Alterar cargo/concessões/usuário e confirmar próxima requisição; tentar remover os últimos poderes administrativos em paralelo e conferir rollback.
6. Convidar, reenviar, aceitar uma vez; tentar link antigo/usado/expirado e cargo inativo. Indisponibilizar SMTP e conferir status/novo envio sem segredo em logs.
7. Recuperar e alterar senha; conferir resposta pública genérica, revogação de sessões/tokens/desafios e ausência de login automático.
8. Deixar polling sem interação até expirar; aba oculta não renova. Conferir aviso, prazo absoluto e política reduzida/aumentada sem reativar sessões antigas.
9. Registrar recebimento e repetir por outro funcionário autorizado, com ator original inativo; conferir mesmo lote/saldo/ator e recusa de conteúdo diferente/requestId reaproveitada.
10. Conferir 401/403/405/409/429, ownership de sessões, preços/descontos/estoque inicial e todas as rotas do inventário; testar visualmente de 320 px a desktop, teclado e duas abas.

Antes de produção: validar sessão JDBC/serialização, concorrência, CSRF/cookies/HTTPS, SMTP, migrações e interface; definir infraestrutura confiável, backup/restauração, atualização de dependências e revisão de segurança. Não há declaração de prontidão para produção.

## Revisão final e encerramento da implementação em arquivos

A revisão independente foi interrompida pelo limite de uso antes de produzir resultado. Após o pedido do usuário para retomar, a mesma revisão foi retomada e concluída, somente por leitura. Não houve redistribuição das tarefas de implementação nem execução da aplicação.

Resultado: dois achados Important e um Minor. A leitura não identificou Critical; isso não equivale a uma certificação de ausência de defeitos.

- Final: ajustado o caminho de restauração do identificador anônimo. A admissão invalida a sessão anônima e cria uma nova linha física, em vez de changeSessionId na mesma PRIMARY_ID. Uma escrita atrasada do framework não encontra a linha anterior para restaurar seu ID. A V3 nova não possui mais ON UPDATE CASCADE na FK de metadados. Login repetido em sessão válida continua reutilizando a vaga. A guarda e a persistência imediata antes de liberar a admissão permanecem.
- Final: ajustada a ampliação de política. Sob a guarda, sessões inválidas pela política anterior recebem tombstone antes do UPDATE da política. Depois, os limites novos são aplicados às restantes, incluindo expiração e excesso. Sessão naturalmente expirada não volta a valer por aumento posterior.
- Final: Ruling: reclassificar o achado de CSRF entre abas como Important para esta entrega — a primeira escrita legítima, inclusive logout, podia falhar após a sincronização de acesso, contrariando o fluxo funcional aprovado — custo de manter: tentativa manual adicional e mensagem de acesso negado indevida. Ajuste aplicado: BroadcastChannel limpa o cache CSRF, a próxima mutação obtém o token atual e gerações impedem uma resposta CSRF antiga de repovoar o cache. Nenhuma gravação é repetida automaticamente.
- Final: complemento da revisão direta: callbacks antigos de consulta de sessão não restauram estado após logout/login; rascunhos não secretos de funcionário/convite ficam em memória da aba para retomada pelo mesmo usuário e são descartados em logout, troca de conta e reload. Nenhuma senha/token entra nesses rascunhos.
- Final: normalização e validação de e-mail foram colocadas explicitamente em AccessEmailService, após trim/caixa inferior. @Email continua sendo a mesma regra de formato, aplicada ao valor final por Validator; foi retirada dos DTOs de convite/recuperação e PatchUserDTO para não recusar espaços externos antes de normalizar. Não foi suprimida a validação de formato. Senhas usam presença não nula nos DTOs e comprimento no service, preservando espaços/Unicode sem trim. Hashes fictícios sem formato Argon2 usam o custo dummy de autenticação e não habilitam a conta.
- Final: formatação localizada nos arquivos novos de autenticação/frontend e no CSS alterado, com Prettier existente. Não foi feita formatação global nem executado lint. O CSS exigiu gravação fora do ambiente restrito e a formatação localizada foi concluída com aprovação. Formatação não comprova compilação ou funcionamento.

Rulings sobre os itens que o revisor não julgou:

- Compilação/resolução completa de APIs: permanece pendente por restrição explícita a build/Maven; não afirmar compatibilidade executada.
- Flyway/ORM e corridas concorrentes: permanecem no roteiro autorizado futuro, sem aplicar SQL ou simular aprovação por leitura.
- SMTP/entrega e navegação/responsividade/acessibilidade no navegador: dependem de configuração/execução futura; nenhuma entrega de e-mail ou validação visual foi anunciada.
- Dependências e produção: o aviso existente do Vite está documentado; atualização geral e prontidão de infraestrutura não foram inferidas como autorização desta entrega local.
- Base operacional anterior: preservada no checkout; a revisão trata das ligações de segurança e autoria tocadas, sem declarar nova auditoria integral dos módulos anteriores.

Estado final: tarefas 1–8 implementadas no escopo de arquivos e documentação, com revisão estática e ajustes acima. Sem pendência de implementação conhecida classificada como Important nesta revisão; sem validação em execução e sem promessa de prontidão operacional/produção. Próximo marco é configurar o ambiente e autorizar a validação executada do roteiro, antes de avançar para telas de cadastros/preços.

## Validação posterior autorizada pelo usuário

Após a entrega acima, o usuário pediu explicitamente executar os testes referentes à autenticação/acessos. Esta autorização substitui a restrição anterior para esta rodada; não autoriza testes automáticos em tarefas futuras.

Foram acrescentadas e executadas suítes específicas: 17 testes JUnit de integração com servidor HTTP, PostgreSQL/JDBC/Argon2 e SMTP local, mais 9 testes node:test do cliente de API. Resultado final: 26 casos aprovados, sem falhas. Compilação backend e npm run build também concluídos. Flyway aplicou V1–V4 em banco novo auth_verify_1792732970807, exclusivo na porta 55433; o banco de desenvolvimento erp não foi acessado.

O startup revelou a criação automática de um UserDetailsService padrão pelo Spring. Um teste reproduziu o problema; SecurityConfig recebeu AuthenticationManager explícito que recusa autenticação fora do fluxo do ERP. O teste de ausência da conta padrão e toda a suíte ficaram verdes; o log final não registra senha padrão gerada.

O executor recebeu um ajuste para o diretório de sockets temporários do Java 25 no Windows. Não foi necessário alterar regras de negócio para fazer os demais testes passarem. A cobertura inclui concorrência, expiração, CSRF, convites/reset, preservação do último administrador, autoria histórica e permissões adicionais em entradas compostas. O envio foi verificado em captura SMTP local e com indisponibilidade simulada; não houve entrega em provedor real.

Relatório completo: [2026-10-23-authentication-and-access-verification.md](2026-10-23-authentication-and-access-verification.md). Permanecem pendentes navegação visual/E2E, bootstrap interativo positivo, configuração SMTP externa e validação de implantação. Processos de teste encerrados; sem commit/push ou publicação.

## Simplificação posterior solicitada pelo usuário

O usuário pediu remover o uso de variáveis de ambiente e os dois arquivos extras de configuração. Essa decisão substitui a separação de perfis prevista no plano.

- Removidos application-local.yaml e application-production.yaml, incluindo suas cópias já geradas em target/classes.
- application.yaml mantém os valores locais diretamente: conexão restaurada da configuração anterior, cookie compatível com HTTP local, URL do frontend, SMTP e parâmetros de acesso. Autenticação, CSRF, sessões e migrations foram preservados.
- Removida a seleção do perfil local na suíte de integração; o executor continua passando conexão exclusiva de teste, com guarda antes do Flyway.
- Documentação ajustada para configurar banco, SMTP e bootstrap no YAML único. Configuração para hospedagem fica para a etapa futura.
- Revisão direta dos arquivos; sem nova execução de testes/build/lint, inicialização da aplicação ou acesso ao banco nesta alteração. Os resultados de testes acima pertencem à rodada anterior.

## Confirmação manual do usuário — 28/10/2026

O usuário apresentou o log com Administrator created, confirmando o bootstrap no banco local. Depois confirmou ter iniciado backend/frontend, entrado com o administrador e conferido as telas de Funcionários, Cargos, Sessões, Política de acesso e Auditoria.

Essa confirmação cobre o acesso e a conferência básica das telas pelo usuário; não representa nova execução pelo agente nem confirma todos os fluxos administrativos, responsividade ou acessibilidade.

Próxima pendência: escolher/configurar o servidor SMTP e validar convite, aceite e recuperação pelo navegador. O envio para provedor externo continua sem confirmação.

## Configuração de envio real — Gmail

O usuário escolheu aldinhuuu145@gmail.com como remetente e forneceu uma senha de app para configurar o envio. application.yaml foi ajustado para smtp.gmail.com:587, autenticação, STARTTLS obrigatório, verificação da identidade do servidor e remetente correspondente. A senha não é reproduzida neste registro.

Mantida a configuração em um único YAML, conforme preferência expressa do usuário. A fixture SMTP dos testes mantém STARTTLS obrigatório desativado explicitamente, pois usa captura local sem TLS.

Configuração revisada diretamente; sem testes automatizados, build/lint, inicialização do backend ou envio de mensagens nesta alteração. Autenticação SMTP e entrega real ainda não foram confirmadas. Reiniciar o backend e validar convite/recuperação na interface; links permanecem locais enquanto public-base-url for localhost.

## Administrador inicial por migration — decisão posterior do usuário

O usuário solicitou substituir o bootstrap por INSERT em migration e confirmou deixar campos com texto para preenchimento manual. Esta decisão substitui o procedimento inicial aprovado anteriormente; não altera o histórico das verificações já executadas.

- Criada V5__insert-initial-administrator.sql com adminEmail e adminPasswordHash contendo marcadores explícitos. Senha recebe hash Argon2; não armazenar senha em texto puro no password_hash. Preencher antes da primeira aplicação da V5 no banco.
- Removidos AdministratorBootstrap, campos em AuthenticationProperties, flags do YAML e bloqueio de requisições exclusivo desse modo. Iniciar o backend não pede mais dados por terminal.
- A V5 participa da guarda única de segurança; reconhece administrador por usuário/cargo ativos com as três permissões conjuntas. Conta já existente é preservada. Sem administrador, valida campos/cargo e recusa colisão de e-mail sem promover usuário existente. Inserção é auditada sem credenciais.
- V1–V4 preservadas. Nenhuma migration aplicada no banco local; o administrador já criado pelo usuário não foi modificado.
- Fixture existente adaptada para copiar migrations para target e preencher somente a cópia com dados próprios de teste; expectativa de versão atualizada para V5. Sem criar nova bateria ou executar testes.
- README e instruções atualizados. Os antigos JARs empacotados não foram reconstruídos; recompilar normalmente ao usar a versão nova.
- Revisão direta de referências e fluxo, sem build/lint, inicialização da aplicação, execução SQL ou novos resultados de teste nesta alteração.

## Restauração do bootstrap — 28/10/2026

Por solicitação explícita do usuário, a alternativa por migration foi revertida: V5__insert-initial-administrator.sql removida e AdministratorBootstrap restaurado. As propriedades bootstrap-enabled e bootstrap-role-id e o bloqueio HTTP durante esse modo voltaram; bootstrap-enabled permanece false no YAML para uso normal.

A suíte existente voltou às fixtures próprias e à expectativa V1–V4, mantendo o SMTP de captura sem STARTTLS obrigatório. As instruções foram atualizadas e a configuração Gmail preservada. Nenhuma conta foi modificada, nenhuma migration aplicada e nenhum teste/build/lint ou startup executado nesta reversão.

Uma consulta somente de leitura ao histórico Flyway no banco local retornou que flyway_schema_history não existe. Não houve reparo nem alteração do banco; o estado atual das contas não foi confirmado por essa consulta.

## Auditoria legível — 29/10/2026

Implementada a proposta aprovada: nomes e IDs do responsável e do alvo, ações/resultados e filtros em português, filtro adicional pelo nome histórico do responsável. Detalhes da política de sessão são apresentados com rótulos em português. A permissão security.audit.read foi preservada.

V5__enrich-security-audit.sql acrescenta campos de snapshot aos novos eventos. Essa V5 é exclusiva da auditoria; a antiga V5 de administrador permanece removida. Bootstrap e credenciais preservados. Exclusões capturam o nome antes de perder o cadastro; renomeações posteriores não reescrevem eventos. Eventos antigos mostram Nome não registrado.

Aceite de convite, redefinição por token e desafio de limite de sessões após prova de identidade registram o funcionário comprovado. Recuperação pública não presume que o solicitante é o dono da conta. Bootstrap/envio automático são Sistema; solicitações sem identidade comprovada são Não identificado.

Revisão direta e independente somente leitura, sem execução de testes/build/lint, startup, envio real ou migrations. Ajustada apenas a expectativa de versões da suíte existente para V1–V5, sem executá-la. Os novos campos dependem da aplicação da migration no próximo startup com a versão atualizada. Sem validação no navegador. Plano e limites em 2026-10-29-security-audit-presentation.md.
