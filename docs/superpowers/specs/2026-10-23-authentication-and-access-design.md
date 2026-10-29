# Autenticação e administração de acessos — especificação

Data: 23/10/2026. Estado: proposta documentada para revisão; nenhuma implementação autorizada por este documento.

## Objetivo e limite desta entrega

Permitir que uma instalação individual do ERP identifique funcionários, aplique as permissões dos cargos e administre sessões, convites e recuperação de senha. Entregar as telas de acesso e administração necessárias ao primeiro fluxo local protegido.

Esta especificação registra requisitos aprovados na conversa e escolhas técnicas propostas. Revisar o documento não autoriza instalar dependências, modificar código ou migrations, executar testes/build/lint ou fazer commits. AGENTS.md continua prioritário.

## Decisões confirmadas com o usuário

- Uma instalação individual por distribuidora, com aplicação, banco e configurações próprios. Não implementar arquitetura multi-tenant, tenant_id, seleção de empresa ou cadastro público de distribuidoras.
- Um CNPJ por instalação e possibilidade de vários depósitos. Usuários são funcionários da empresa dessa instalação.
- Sem portal do comprador. WhatsApp é evolução prioritária posterior à conclusão do ERP interno e não entra nesta entrega.
- Cinco modelos iniciais de cargo: Administrador, Vendas, Compras, Estoque e Financeiro. A empresa cria e configura cargos, e cada usuário possui um cargo.
- Catálogo técnico de permissões mantido pelo sistema; concessões por cargo administradas pela empresa. Visibilidade de todos os registros das funções autorizadas, sem filtro por vendedor.
- Sessões no servidor, login por e-mail/senha e identificador em cookie protegido. Sem adoção de JWT nesta entrega.
- Valores iniciais: 60 minutos de inatividade, 720 minutos de duração máxima e duas sessões simultâneas por usuário. Administrador com permissão específica configura esses valores.
- Aviso antes da expiração. Atividade legítima renova o prazo de inatividade, nunca a duração máxima. Atualizações automáticas de telas não mantêm a sessão indefinidamente.
- Terceiro login exige senha válida e permite escolher uma sessão anterior para encerrar; não desconecta uma sessão automaticamente. Abas no mesmo contexto de cookies compartilham uma sessão; navegadores e contextos isolados contam separadamente.
- Convite por e-mail para o funcionário definir a senha; recuperação por link temporário de uso único; administrador pode iniciar reenvio/redefinição sem conhecer a senha do funcionário.
- Redefinição de senha encerra sessões anteriores. Primeiro administrador preparado de forma controlada na implantação.
- Operador de ações obtido da sessão. Responsável comercial é informação de negócio distinta.
- Começar com frontend, backend e PostgreSQL locais. Depois apresentar com frontend/banco hospedados e backend local pelo ngrok. Produção exige planejamento próprio de implantação.
- Banco atual contém dados de desenvolvimento. Preservar arquivos e alterações locais; isso não autoriza apagar ou recriar o banco.
- Financeiro e fiscal permanecem no roteiro geral: recebíveis na saída comercial, pagáveis por cobrança conferida e lançada pela equipe; baixas manuais; NF-e/boletos externos. Transferências e devolução integrada ficam posteriores; devolução inicial terá procedimento manual controlado. Esses módulos não são implementados aqui.

## Base observada em 23/10/2026

- Backend com Java 21, Spring Boot 4.1.0, JPA, PostgreSQL, Flyway e validação. Há spring-security-crypto e Argon2; faltam o suporte completo de segurança web, Spring Session JDBC e envio de e-mail.
- User, Role, Permission e RolePermission têm entidades, repositories, services e controllers. User possui senha obrigatória e cargo único; o hash não é serializado no JSON.
- UserService.update já bloqueia o usuário e é transacional. Preservar essa correção e os campos finais do PATCH.
- A V2 possui usuários/permissões fictícios, inadequados como catálogo de produção. Não convertê-los em administradores nem substituir seus hashes por senha padrão.
- Nenhuma estrutura de sessão, convite, token de recuperação, configuração administrativa de segurança ou auditoria foi encontrada na V1.
- Frontend preserva React/Vite/Tailwind e lucide-react. Falta main.jsx e a aplicação funcional. O proxy local /api já está definido no Vite.
- Registros da auditoria anterior documentam as correções de concorrência de usuários, serialização/retry de recebimentos e rejeição de números fracionários em inteiros. Não refazer essas correções nem afirmar nova validação em execução.

## Escolhas técnicas propostas para revisão

### Persistência e configuração

Usar Spring Security e Spring Session JDBC com PostgreSQL; versões gerenciadas pelo Spring Boot existente, sem atualização geral de framework. Flyway será o único responsável por criar as tabelas; desabilitar inicialização automática concorrente do schema de sessões.

Propor V3 para persistência de acesso e V4 para o catálogo e modelos de cargos. Confirmar a próxima versão livre ao implementar. Não editar V1/V2 nem executar repair/recriação. A inclusão de novos objetos não corrige checksums de bancos já divergentes: levantar esse estado antes de qualquer aplicação futura.

As tabelas padrão do Spring Session guardarão apenas o contexto de segurança mínimo. Não serializar entidades JPA, senha/hash, lista permanente de permissões ou grafo de domínio na sessão.

Adicionar metadados próprios de sessão com usuário, identificador público aleatório distinto da credencial de sessão, instante de autenticação, última atividade interativa e versão das credenciais. O identificador público serve para listar/revogar; nunca permite autenticar.

Manter uma configuração única de segurança por instalação, inicialmente 60/720 minutos e duas sessões. Faixas propostas: inatividade entre 15 e 240 minutos; duração máxima entre 60 e 1440 minutos; simultaneidade entre 1 e 5; inatividade não pode exceder a duração máxima. Valores fora da faixa retornam 400. Esses limites adicionais ainda são proposta, não preferência já confirmada.

Alterações aplicam-se na próxima validação das sessões existentes. Reduzir simultaneidade revoga o excedente por menor atividade, preservando preferencialmente a sessão do administrador que alterou a política. A tela mostra o impacto e pede confirmação da alteração; aumentar limites não reativa sessões revogadas.

Usar instantes UTC na persistência nova; exibir em horário local no frontend. Não converter em massa os timestamps históricos dos módulos existentes.

### Ciclo de sessão e concorrência

Cookie de sessão HttpOnly, host-only e SameSite=Lax. Secure apenas no ambiente HTTPS; a exceção HTTP pertence exclusivamente ao perfil local. Não guardar credenciais de sessão em localStorage. Manter CSRF ativo também no login, logout e operações públicas de recuperação/aceitação.

Autenticar com mensagens genéricas para e-mail inexistente, senha incorreta, usuário inativo ou cargo inativo. Usar verificação de custo equivalente para conta inexistente e contenção de tentativas. Atualizar lastLoginAt somente após concluir o login.

Trocar o identificador da sessão ao autenticar, persistir explicitamente o contexto e renovar o token CSRF. A sessão anônima usada no CSRF não ocupa vaga de sessão autenticada.

Login repetido para a mesma conta no mesmo contexto autenticado não cria outra vaga. Para trocar de conta naquele contexto, exigir logout antes de novo login; retornar 400 específico se outro usuário for informado com sessão ainda ativa.

Na validação de cada requisição privada: verificar sessão, prazo de inatividade, prazo absoluto, versão das credenciais, usuário ativo, cargo ativo e permissões atuais. Uma requisição já em execução pode concluir sob a autorização validada na entrada; não prometer cancelamento retroativo de transações em andamento.

O relógio lógico de inatividade será separado do lastAccess do framework. O frontend envia sinal de atividade, com CSRF, apenas com interação recente em aba visível, limitado a um envio por minuto. Polling e consulta de sessão não atualizam esse relógio. Isso é um sinal de uso do cliente, não prova de presença humana; a duração absoluta continua obrigatória. Expiração usa horário do servidor.

O prazo técnico do repositório de sessões não pode eliminar uma sessão antes do prazo lógico permitido, inclusive após aumento administrativo. Configurar limpeza técnica compatível com o teto de 24 horas, mantendo as validações lógicas independentes; liberar vagas pela validade lógica, sem aguardar limpeza física.

Após senha válida com limite ocupado, devolver conflito específico com desafio de substituição de curta duração e metadados mínimos das sessões desse usuário. Não conceder autoridades nem acesso privado nesse estado. Desafio aleatório, hash persistido, uso único, duração proposta de dois minutos e vinculado ao contexto anônimo do navegador.

Concluir substituição sob coordenação transacional: revalidar desafio, usuário/cargo, versão das credenciais, política e propriedade da sessão escolhida; revogar a escolhida e admitir a nova sessão uma única vez. Se houve alteração concorrente, devolver novo conflito para atualizar a escolha. Se surgiu uma vaga, completar a admissão de forma atômica. Não confiar em contagem feita antes do lock.

Operações de administração de acesso e admissão/revogação serão coordenadas por uma linha única de controle de segurança, com transações curtas. Ordem: controle de segurança, cargos por ID, usuários por ID, convites/tokens/desafios e sessões. Verificação Argon2 e chamadas SMTP ficam fora desses locks; revalidar hash/versão e estado após adquirir o controle. O custo é serializar essas pequenas mutações por instalação; não envolver consultas ou transações comerciais nesse controle.

Persistência no JDBC precisa concluir antes de liberar a coordenação de admissão. O filtro que salva sessão ao final da resposta não pode ser a única garantia contra dois logins concorrentes ultrapassarem o limite. Requisição concorrente de uma sessão revogada não poderá ressuscitá-la.

### Convites e senha

Guardar convite pendente com nome, e-mail normalizado, cargo, criador, hash do token, emissão, validade e consumo/cancelamento. Criar User somente ao aceitar e definir senha. Um convite ativo por e-mail; reenvio invalida o token anterior. Cargo ativo e unicidade de e-mail são revalidados no consumo.

Convite proposto de 24 horas e recuperação de 15 minutos. Tokens com aleatoriedade criptográfica, ao menos 32 bytes, armazenados como hash e jamais em logs, respostas administrativas ou parâmetros de telemetria. Token bruto só compõe a mensagem destinada ao titular. Links usam URL-base configurada no servidor, sem confiar no Host recebido. Frontend remove token da URL após capturá-lo em memória; páginas de token não carregam terceiros e usam política de referência restritiva.

Enviar e-mail após commit, com credencial SMTP no servidor e timeout. Falha no envio mantém convite rastreável e permite novo envio autorizado; não desfazer usuário/senha por falha SMTP nem registrar o link no console. Não manter a transação aberta durante SMTP. Não prometer entrega exatamente uma vez. Desenvolvimento utiliza caixa SMTP local de captura ou conta de teste a configurar posteriormente, sem escolher provedor nesta tarefa.

Normalizar e-mail com trim e caixa inferior nos fluxos novos, cadastro administrativo e alteração. Antes do índice correspondente, detectar colisões existentes e interromper a aplicação para resolução explícita; nunca mesclar/apagar usuários automaticamente. Não aplicar trim a senhas.

Política proposta para senhas novas: 15 a 128 caracteres, suporte a espaços/Unicode, sem composição artificial ou troca periódica obrigatória. Preservar o encoder Argon2 atual. Aplicar a política a convite, alteração e recuperação; não invalidar silenciosamente senhas existentes no login.

Alteração própria exige senha atual. Recuperação não altera a conta apenas pela solicitação; consumo válido altera hash, incrementa a versão de credenciais, invalida demais tokens/desafios e revoga todas as sessões atomicamente. Não autenticar automaticamente após redefinição. E-mail de acesso só pode ser alterado por administrador autorizado, com revogação de sessões/tokens e comunicação ao endereço anterior; mudança de e-mail não integra a edição de perfil próprio nesta entrega.

### Contratos administrativos e operador histórico

O catálogo Permission fica consultável, com concessões geridas por RolePermission. Proposta explícita de mudança: POST/PATCH/DELETE em /permission deixam de estar disponíveis no ERP, retornando 405 ao usuário autenticado; inventar uma permissionKey não cria uma regra executável. Não tratar o nome do cargo como autorização.

Proposta explícita de mudança: POST /user deixa de cadastrar uma senha pela API e retorna 405 ao usuário autenticado; novos usuários entram por convite. Manter GET/PATCH/DELETE nos contratos existentes, sujeitos às permissões e às regras de domínio. Durante transição, password não nulo no PATCH administrativo retorna 400 específico, em vez de ser ignorado; a alteração própria possui fluxo separado.

Pelo menos um usuário ativo, com cargo ativo, deve manter conjuntamente access.users.manage, access.roles.manage e security.settings.manage. A instalação vazia só pode sair do estado sem administrador pelo bootstrap controlado; a regra de preservação aplica-se depois desse primeiro cadastro. Verificar sob a mesma coordenação em mudanças de usuário/cargo/concessões/exclusão. Não há exceção pelo nome Administrador nem conta secreta de suporte. Conceder access.roles.manage é delegar poder amplo de administrar acesso; informar essa consequência na tela.

Inativar usuário ou cargo revoga suas sessões e desafios; reativar não recupera acessos antigos. Alterar o cargo ou suas concessões conserva o mecanismo de consulta atualizada, sem conservar autoridades antigas no principal.

Em movimentos manuais, saída e novos recebimentos, obter o operador autenticado. Campos legados de operador podem ser omitidos; se presentes na criação de um efeito, devem coincidir com o usuário autenticado. Não substituem autenticação. userId de responsável comercial continua sendo campo de domínio.

Recebimento já registrado é histórico: autenticação e permissão atuais são obrigatórias antes de devolver um retry. Repetição idêntica pode ser consultada por outro funcionário autorizado, inclusive quando o operador original foi inativado; não alterar esse operador nem criar efeitos. Para esse replay, operador legado informado deve coincidir com o operador histórico; sem o campo, usar o histórico para comparar. Para um lote novo, usar o ator autenticado. Mudança de quantidades/notas com a mesma requestId continua rejeitada. Essa adaptação de contrato deve ser explícita e preservar os grafos LOAD e a serialização corrigida.

### Dependências futuras, fronteira de escopo e interface

Dependências propostas: starters de segurança web, sessão JDBC e mail no backend; roteador no frontend, com versões compatíveis e lockfile no momento da futura instalação. Não trocar Spring Boot nem reformatar o projeto como efeito colateral.

DTOs de entrada e organização seguem customer/address. Entidades de negócio continuam sendo respostas dos CRUDs. Registros de transporte de autenticação/sessão/CSRF são exceção justificada para expor somente dados públicos e nunca a credencial da sessão. ResponseErrorDTO e seu formato permanecem.

Falhas dos filtros de segurança usam o mesmo formato JSON por entry point/access denied handler, com 401 para ausência/expiração em rota privada, 403 para permissão/CSRF, 409 para substituição necessária/conflito e 429 para excesso. Ordenar a validação lógica e o tratamento de erros para que falta de CSRF numa escrita privada sem sessão não mascare o 401; em rota pública, CSRF inválido retorna 403. Não redirecionar APIs para HTML. Sessões/tokens usam Cache-Control: no-store.

Telas objetivas, responsivas, sem dashboard fictício, indicadores inventados ou páginas de módulos ainda não construídas. Após login, abrir a primeira tela administrativa autorizada; funcionário sem tela disponível recebe estado explícito de ausência de módulos disponíveis, com acesso a sua conta/sessões. Rotas internas não são abertas antes de carregar a sessão.

Não implementar aqui financeiro, fiscal, WhatsApp, cadastros operacionais completos, paginação global de CRUDs antigos, inventário ou relatórios comerciais. Incluir somente proteções de entrada necessárias ao novo acesso. Segurança completa de produção, retenção legal, backups e defesa volumétrica continuam marcos próprios do roteiro.

## Propostas de parâmetros ainda sujeitas à revisão

Além das faixas administrativas e validades acima: aviso de expiração com cinco minutos restantes; após cinco falhas por conta em quinze minutos, espera progressiva de 30 segundos até quinze minutos; recuperação até três solicitações por hora/conta com intervalo mínimo de sessenta segundos. Limite inicial de autenticação de 60 tentativas por minuto/IP e recuperação de 30 por hora/IP, ajustáveis pela implantação, considerando NAT. Convites/reenvios: 30 por hora/administrador e intervalo mínimo de sessenta segundos/destinatário. Limitar concorrência de Argon2 e tamanho do corpo de login antes do processamento caro.

Limites por conta usam identidade normalizada e estado limitado/expirável também para contas inexistentes; não criar uma tabela ilimitada a partir de e-mails/IPs arbitrários. Não confiar em X-Forwarded-For recebido diretamente da internet. Parâmetros de abuso pertencem à implantação nesta entrega; ajustes de sessões pertencem à interface administrativa.

## Marcos e critérios de conclusão

1. Persistência: novas estruturas e catálogo documentados; migrations futuras não alteram V1/V2 nem introduzem usuários/senhas padrão.
2. Acesso: login, CSRF, expiração, limite concorrente, substituição e revogação têm contratos completos e revisão direta.
3. Identidade: convite/reenvio/recuperação/alteração de senha preservam unicidade, consumo único e revogação.
4. Autorização: inventário de todos os endpoints existentes mapeado; filtros, serviços compostos e respostas associadas não contornam as permissões.
5. Interface: administrador convida funcionário, configura cargo/política e revoga sessão; funcionário define senha, entra e acessa apenas funções liberadas.
6. Revisão de segurança: sem hash/token/credencial nos JSONs e logs examinados; nenhum administrador perdido por mutações concorrentes; correções anteriores preservadas.

Revisão estática pode ser feita durante implementação autorizada. Execução de aplicação, e-mail e demonstração manual deve ser combinada separadamente; testes automatizados, build e lint exigem solicitação explícita. Critérios listados não são resultados executados. A entrega local não comprova aptidão de produção nem conformidade LGPD.

## Referências

- AGENTS.md e customer/customer.address do checkout atual.
- docs/superpowers/plans/2026-10-22-user-module-progress.md.
- docs/superpowers/plans/2026-10-22-backend-audit-fixes-progress.md.
- [Spring Security: sessões](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- [Spring Security: CSRF e SPA](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).
- [OWASP: autenticação](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
- [OWASP: recuperação](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).

As páginas online podem mostrar versões posteriores ao Spring Boot 4.1.0 do projeto. A implementação futura deve conferir os componentes gerenciados pelo BOM local; estas referências não autorizam atualização de versão.
