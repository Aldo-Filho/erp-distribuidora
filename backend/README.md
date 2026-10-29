# Para rodar o Projeto:

- Clone o repositório e abra o projeto na sua IDE;
- Certifique-se de ter o Java 21+ e PostgreSQL instalados;
- Crie um banco de dados PostgreSQL para o projeto (nome = "erp");
  Configure a conexão diretamente em src/main/resources/application.yaml;
  Execute o projeto Spring Boot pela IDE no arquivo: ErpApplication.java;
  Após iniciar o projeto, utilize Postman ou Insomnia para realizar as requisições;

## O arquivo com as requisições para teste está localizado na raiz da pasta principal do projeto, com o nome:

`testeRequisições`

- Esse arquivo pode ser importado diretamente no Postman ou Insomnia para facilitar os testes da API.

## Acesso local — configuração antes de executar

Após autorização específica, a compilação e 17 testes de integração passaram em PostgreSQL isolado, com V1–V4 aplicadas e Hibernate validado. O banco de desenvolvimento não foi acessado. Não iniciar contra um banco existente sem revisar os checksums atuais e preparar backup. V1/V2 não foram reescritas por esta implementação; eventuais diferenças locais anteriores continuam existentes. Não usar Flyway repair para ocultá-las.

Use Java 21 e o Spring Boot 4.1.0 existente. O BOM local gerencia Security 7.1.0 e Session 4.1.0. Os starters security, session-jdbc e mail foram declarados no pom e compilados na verificação, com JDK 25 gerando target Java 21.

Por solicitação do usuário, a configuração local fica somente em src/main/resources/application.yaml, com valores diretos. Não é necessário ativar perfil nem cadastrar variáveis ERP_* no IntelliJ. Os arquivos application-local.yaml e application-production.yaml foram removidos.

| Propriedade no YAML | Uso |
| --- | --- |
| spring.datasource.url | URL JDBC local jdbc:postgresql://localhost:5432/erp |
| spring.datasource.username / password | Credenciais locais do PostgreSQL, restauradas da configuração anterior |
| erp.auth.public-base-url | URL do frontend: http://localhost:5173 |
| erp.auth.mail-from | Remetente configurado: aldinhuuu145@gmail.com |
| spring.mail.host / port | Gmail SMTP: smtp.gmail.com:587 |
| spring.mail.username / password | Autenticação SMTP quando necessária |
| spring.mail.properties.mail.smtp.auth / starttls.enable | Ativados; STARTTLS obrigatório e verificação de identidade TLS habilitada |
| erp.auth.login-per-ip-per-minute / recovery-per-ip-per-hour | Limites locais: 60/min e 30/h |
| erp.auth.argon-concurrency | Máximo de operações caras de senha: 2 |

O cookie permite HTTP no ambiente local por server.servlet.session.cookie.secure=false. A configuração de hospedagem/HTTPS será tratada na etapa de implantação; não há perfil de produção nesta entrega.

O envio real foi configurado para o Gmail com a senha de app fornecida pelo usuário. O valor da senha não é reproduzido nesta documentação. Reiniciar o backend é necessário para carregar as propriedades novas. Ainda é necessário confirmar autenticação/entrega no provedor; a configuração, por si só, não comprova entrega.

Enquanto erp.auth.public-base-url permanecer em http://localhost:5173, os links recebidos devem ser abertos no computador em que o frontend está rodando. A publicação para acesso por outros dispositivos continua na etapa futura.

Flyway é o único criador do schema de sessões. A V3 interrompe em colisões de lower(btrim(email)); resolver as contas explicitamente, sem apagar/mesclar automaticamente. Confirmar o histórico Flyway antes da primeira inicialização autorizada. A V4 cria cinco novos cargos; não promove usuários/cargos fictícios nem assume IDs fixos.

### Primeiro administrador

O bootstrap interativo cria o primeiro administrador somente quando não houver administrador ativo. No application.yaml, erp.auth.bootstrap-enabled permanece false para o uso normal; erp.auth.bootstrap-role-id só é necessário ao criar a conta inicial.

Execute os comandos abaixo no PowerShell, a partir da raiz do projeto. Use Java 21 ou superior e mantenha o PostgreSQL em execução, com a conexão configurada no application.yaml.

1. Entre na pasta do backend e gere o JAR atualizado, sem executar os testes:

```powershell
Set-Location .\backend
.\mvnw.cmd -DskipTests package
```

2. Se o banco ainda não tiver as tabelas e os cargos das migrations, inicie normalmente para o Flyway criá-los:

```powershell
java -jar .\target\erp-0.0.1-SNAPSHOT.jar --erp.auth.bootstrap-enabled=false
```

Após a inicialização, encerre com Ctrl+C antes de executar o bootstrap. Se as migrations já estiverem aplicadas, pule este passo.

3. No pgAdmin, abra o Query Tool do banco configurado no YAML e consulte os cargos ativos com as três permissões exigidas:

```sql
SELECT r.role_id, r.name
FROM roles r
JOIN role_permissions rp ON rp.role_id = r.role_id
JOIN permissions p ON p.permission_id = rp.permission_id
WHERE r.active = true
  AND p.permission_key IN (
      'access.users.manage',
      'access.roles.manage',
      'security.settings.manage'
  )
GROUP BY r.role_id, r.name
HAVING count(DISTINCT p.permission_key) = 3
ORDER BY r.role_id;
```

Copie o role_id do cargo desejado. Se a consulta não retornar cargos, confira a aplicação da V4 antes de continuar. O bootstrap exige um cargo existente; não cria cargos nem concede permissões.

4. De volta ao PowerShell, ainda na pasta backend, execute o bootstrap. **Substitua 21 pelo role_id retornado na consulta**; esse número é apenas um exemplo:

```powershell
java -jar .\target\erp-0.0.1-SNAPSHOT.jar --erp.auth.bootstrap-enabled=true --erp.auth.bootstrap-role-id=21
```

O terminal solicita nome, e-mail, senha de 15 a 128 caracteres e confirmação. A senha é digitada sem exibir caracteres, nem asteriscos; digite normalmente e pressione Enter. Não é necessário gerar o hash manualmente. Use um terminal interativo, pois o console de execução da IDE pode não oferecer a leitura protegida de senha exigida pelo bootstrap. O bootstrap recusa e-mails já cadastrados e não promove contas existentes.

Ao mostrar Administrator created, a conta foi criada. O encerramento com Bootstrap finished. Disable bootstrap mode and restart. é intencional. Reiniciar normalmente, sem os argumentos de bootstrap; se o modo foi ativado no YAML, voltar bootstrap-enabled para false antes de reiniciar. Durante o bootstrap, as requisições HTTP ficam bloqueadas.

5. Inicie o backend com o bootstrap desativado:

```powershell
java -jar .\target\erp-0.0.1-SNAPSHOT.jar --erp.auth.bootstrap-enabled=false
```

Entre no frontend com o e-mail e a senha informados. A conta e o hash da senha ficam no PostgreSQL. O bootstrap serve apenas para o primeiro administrador: se já houver um administrador ativo, ele interrompe sem criar outro usuário. Cadastros posteriores devem usar o fluxo de convites da aplicação.

O usuário já confirmou a criação do administrador anteriormente. Se continuar usando aquele banco e a conta ainda existir, não repetir o bootstrap. A restauração deste procedimento não modifica contas no banco.

### Auditoria de acesso

A consulta /security/audit/search continua restrita à permissão security.audit.read. A tela mostra ações e resultados em português, responsável e alvo com nome e ID, e filtros por nome registrado do responsável, ID, período, ação e resultado.

A V5__enrich-security-audit.sql acrescenta somente os campos de identificação histórica na auditoria. Ela não cria administradores nem altera credenciais; o primeiro administrador continua pelo bootstrap. A migration será aplicada pelo Flyway na próxima inicialização com o backend atualizado; não foi aplicada durante esta alteração.

Novos eventos guardam os nomes no momento da ação, preservando-os após renomeações e exclusões. Eventos antigos continuam disponíveis com os IDs e a indicação Nome não registrado, pois os nomes históricos não foram armazenados. O filtro por nome considera o nome gravado no evento. Operações automáticas aparecem como Sistema; solicitações sem identidade comprovada aparecem como Não identificado.

Aceite de convite e redefinição com token válido identificam o funcionário que comprovou acesso ao link. Solicitar uma recuperação pública não identifica automaticamente o solicitante como dono da conta; o funcionário correspondente, quando encontrado, é apenas o alvo. Conteúdo de e-mail, senhas e tokens não são expostos na auditoria.

Revisão desta mudança feita por leitura direta. Testes, build, lint, inicialização e aplicação da V5 não foram executados. A suíte existente teve apenas a expectativa de versões atualizada para V1–V5; os resultados anteriores continuam referentes a V1–V4.

### Contratos e limites

O cliente local usa /api pelo proxy do Vite. Antes de qualquer escrita, GET /auth/csrf devolve token/headerName. Enviar o header e manter o cookie HttpOnly. Após login/logout, obter novo CSRF. Não repetir gravações automaticamente. Os contratos completos estão em docs/superpowers/plans/2026-10-23-authentication-and-access.md, relativo à raiz do repositório.

401 indica ausência/expiração, 403 indica permissão/CSRF, 409 indica conflito (incluindo desafio de substituição) e 429 indica contenção com Retry-After. POST /user e escritas de catálogo /permission retornam 405 para autenticados; novos funcionários entram por convite.

Política inicial: 60 min de inatividade, 720 min absolutos e 2 sessões. Faixas administrativas: 15–240, 60–1440 e 1–5; inatividade não ultrapassa o prazo absoluto. /auth/session e polling não renovam atividade; /auth/activity exige sessão/CSRF e só atualiza uma vez por minuto.

O principal JDBC contém identidade/versão, sem entidades, senhas ou concessões permanentes. Revogações mantêm tombstones até limpeza técnica; a autorização exige admissão lógica válida. Argon2 ocorre fora da guarda; mutações de acesso e admissão são serializadas por security_settings.

Ao autenticar, o contexto anônimo é invalidado e substituído por uma sessão com nova linha física no JDBC. A ampliação dos prazos revoga antes as sessões expiradas pelos limites anteriores; os novos limites não reativam acesso encerrado. Esses caminhos receberam revisão independente e testes de integração na rodada autorizada.

Convites, recuperação e alteração administrativa normalizam e validam o e-mail no service, usando a regra @Email sobre o valor final. O PATCH mantém o bloqueio de usuário e as demais validações; senha administrativa continua recusada.

Convites duram 24 h, recuperação 15 min e desafios 2 min. Tokens são hashes no banco; links usam fragmento e URL-base configurada. E-mail é enfileirado em memória após commit, com fila limitada e timeout; SUBMITTED significa aceitação pelo SMTP, não entrega na caixa. Falha mantém evento/estado para reenvio. Reiniciar durante o envio pode deixar PENDING; não há outbox durável ou garantia de entrega única.

Em movimentos, saídas e novos recebimentos, operador vem da sessão. Campo legado é opcional; se presente, deve coincidir. Retry de recebimento exige purchases.receive, usa o ator histórico (mesmo inativo) e preserva quantidades/notas/requestId. Responsável comercial de pedidos/compras continua separado.

products.read/suppliers.read incluem custos nos grafos retornados. Não há sigilo de custo/margem por cargo nesta entrega. Financeiro não tem permissões/telas fictícias. Auditoria usa 25 registros por página (máximo 100), sem edição/exclusão e sem retenção automática.

Os testes autorizados anteriores exercitaram sessões JDBC, cookies/CSRF, concorrência pontual, ORM/Flyway V1–V4, convites/recuperação e SMTP de captura local. O relatório está em docs/superpowers/plans/2026-10-23-authentication-and-access-verification.md, na raiz. Isso não valida SMTP externo, teste de carga ou implantação. Não houve nova execução após a restauração do bootstrap.

Para repetir somente com autorização: executar tools/run-auth-verification.ps1 a partir de backend. O executor cria banco novo auth_verify_ na porta 55433 e encerra os processos; não apaga bancos anteriores. O AuthenticationManager explícito impede a criação da conta padrão de desenvolvimento do Spring; credenciais do ERP continuam exclusivamente no fluxo AuthenticationService.

A suíte mantém o bootstrap desativado e cria suas próprias contas no banco isolado, sem usar credenciais reais. A expectativa atual de migrations inclui V1–V5, ainda sem execução após a melhoria da auditoria.
