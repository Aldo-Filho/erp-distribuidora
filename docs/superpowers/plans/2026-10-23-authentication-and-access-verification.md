# Verificação executada — autenticação e administração de acessos

O usuário autorizou explicitamente executar os testes após a implementação. Foram acrescentadas suítes específicas, sem executar as baterias antigas de outros módulos.

## Isolamento

- PostgreSQL 18.4 exclusivo de verificação, dentro de backend/target/auth-verification/postgres, escutando apenas 127.0.0.1:55433.
- Cada execução cria um banco novo com prefixo auth_verify_; nenhum banco é apagado ou reutilizado automaticamente. O banco de desenvolvimento erp não é acessado.
- Aplicação de teste em 127.0.0.1:18081. A suíte exige URL isolada antes de inicializar os beans/Flyway.
- SMTP de captura em 127.0.0.1:11025, implementado na fixture. Mensagens/tokens são examinados em memória; não há envio para contas externas.
- Primeiro administrador de teste é fixture explícita, criada somente no banco isolado com hash Argon2 e senha aleatória. Isso não substitui o procedimento interativo de bootstrap da instalação.
- PostgreSQL, HTTP e SMTP são encerrados ao final. Bancos e relatórios locais são preservados para diagnóstico.

## Comandos reproduzíveis

Somente mediante autorização do usuário:

- Backend: em backend, executar tools/run-auth-verification.ps1. Requer Java/PostgreSQL/Maven nos caminhos indicados pelo executor. O Maven usa cache offline e roda somente AuthenticationAccessIntegrationTests.
- Frontend: em frontend, executar npm test.
- Compilação frontend: em frontend, executar npm run build.

O executor ajusta jdk.net.unixdomain.tmpdir=target/tmp no fork do Surefire: o caminho temporário padrão do Java 25 causava erro de socket Unix no Windows antes de iniciar os testes HTTP. Caminho relativo também evita a divisão do nome de usuário com espaços no comando da JVM.

## Cobertura

- Flyway V1–V4 em banco novo, validação Hibernate e persistência real do contexto no JDBC.
- Login, erros genéricos, CSRF, contratos 401/403/405/409/429, cache e sigilo dos metadados públicos.
- Nova linha física na autenticação, cookie HttpOnly/SameSite=Lax/host-only, escrita obsoleta sem restauração do contexto anônimo e reutilização de vaga no mesmo navegador.
- Três logins concorrentes para duas vagas, desafio vinculado ao navegador, substituição e consumo único.
- Permissões atualizadas na próxima requisição, inativação/reativação de cargo e propriedade das sessões próprias/aninhadas.
- Expiração por inatividade/limite absoluto e aumento posterior de política sem reativar sessão; polling sem atividade e limitação do sinal interativo.
- Convite normalizado, duplicidade, reenvio, link antigo/usado/expirado, consumo concorrente e cargo inativo.
- Falha SMTP mantém o convite rastreável; recuperação pública genérica, reset e alteração própria revogam sessões e preservam espaços da senha.
- Contenção por conta, Retry-After e consulta de auditoria com paginação/período.
- Recebimento real e retry por outro funcionário autorizado após inativação do operador original, preservando lote, ator e unicidade.
- Estoque inicial, preço manual, desconto e transições exigem permissões adicionais, sem efeitos persistidos quando negados.
- Duas mutações concorrentes de administradores preservam pelo menos um administrador ativo.
- Cliente frontend: CSRF compartilhado, resposta antiga descartada após troca de geração, ausência de repetição automática, expiração, conflito de substituição, erros de rede e retorno de navegação restrito.

Datas expiradas são simuladas por timestamps nas fixtures, sem aguardar horas. O intervalo imediato de reenvio é verificado com 429; o estado do limitador é então reinicializado na fixture para exercitar o fluxo de rotação sem esperar um minuto. Isso não constitui um ensaio de duração real.

## Correção descoberta durante os testes

O Spring criava um UserDetailsService padrão em memória e registrava uma senha aleatória de desenvolvimento no startup, apesar do fluxo próprio e de Basic/Form estarem desabilitados.

Um teste novo reproduziu a presença indevida desse bean (1 teste, 1 falha). SecurityConfig passou a fornecer um AuthenticationManager explícito que recusa autenticação fora do fluxo do ERP, fazendo a autoconfiguração padrão recuar. A suíte verifica ausência da conta padrão, e o log final deixou de anunciar a senha gerada. Nenhuma conta de negócio ou contrato de login foi alterado.

## Resultados e evidências

| Verificação | Resultado |
| --- | --- |
| Compilação backend, Java 21 com JDK 25 | BUILD SUCCESS |
| AuthenticationAccessIntegrationTests | 17 executados, 0 falhas, 0 erros, 0 ignorados |
| npm test, cliente frontend | 9 executados, 9 aprovados |
| npm run build | Concluído com Vite 8.3.2 |
| Flyway e Hibernate no banco isolado | V1–V4 aplicadas; schema validado |

Total: 26 casos de teste aprovados, contando métodos JUnit e casos node:test; não é uma contagem de requisições HTTP ou de asserções.

Último banco: auth_verify_1792732970807. A rodada final Maven terminou em BUILD SUCCESS, com 17 métodos em 23 segundos (35 segundos incluindo preparação/compilação). Nenhum servidor de teste foi mantido ativo.

Evidências locais, sob backend/target e ignoradas pelo Git:

- auth-verification/maven.log — rodada final.
- auth-verification/default-account-red.log — reprodução da conta padrão antes da correção.
- auth-verification/database.txt — nome do banco exclusivo da rodada.
- surefire-reports/com.pi.erp.auth.AuthenticationAccessIntegrationTests.txt e XML correspondente — resultado JUnit.
- auth-verification-compile.log — compilação inicial.

Os casos de cliente foram executados por npm test e o build por npm run build nesta sessão. As suítes reproduzíveis estão em backend/src/test/java/com/pi/erp/auth/AuthenticationAccessIntegrationTests.java e frontend/tests/auth-api.test.mjs. O banco de teste permanece disponível para diagnóstico, com o PostgreSQL isolado encerrado.

## Limites

Não foi feita navegação visual/E2E em navegador, teste de acessibilidade, entrega em provedor SMTP real ou bootstrap positivo por console protegido. Não houve auditoria completa da base comercial anterior, implantação, teste de carga ou prova de todos os interleavings concorrentes.

O teste usa o mesmo PostgreSQL/servlet/JDBC/Argon2 da aplicação, mas fixtures e portas são exclusivas. Os nove testes do cliente usam fetch simulado; não comprovam a interface completa.

Avisos de PageImpl, posição de @Valid em coleção e instrumentação Mockito/JDK foram observados sem falhas nos cenários exercitados; não foi feita refatoração geral para remover avisos. O alerta de dependência existente no Vite permanece fora desta correção.
