# Para rodar o projeto

`npm run dev`

## Acesso e administração

Configurar primeiro o backend pelo application.yaml, conforme backend/README.md, incluindo PostgreSQL e SMTP. Se ainda não houver administrador, criá-lo pelo bootstrap em um terminal interativo e desativar esse modo antes de usar a aplicação. Não é necessário selecionar perfil ou cadastrar variáveis de ambiente. O navegador usa /api pelo proxy existente para localhost:8080. O roteador react-router-dom foi instalado com lockfile atualizado; os módulos operacionais não foram recriados.

Rotas públicas: /login, /accept-invitation, /forgot-password e /reset-password.
Rotas privadas: /account, /access/users, /access/roles, /access/invitations, /security/sessions, /security/settings e /security/audit.

Menus e rotas administrativas seguem as permissões da sessão. Todo funcionário acessa a própria conta e sessões. Sem tela administrativa liberada, o acesso abre Minha conta e informa a ausência de módulos disponíveis.

A auditoria de acesso mantém a permissão security.audit.read. Ações, resultados e filtros são apresentados em português; responsável e alvo exibem nomes registrados no evento e seus IDs. O filtro por nome considera o nome histórico. Eventos anteriores à melhoria não possuem esses nomes e mostram Nome não registrado. Atualizar o backend e aplicar a migration de auditoria pelo fluxo normal do Flyway antes de usar os novos campos. Não houve validação no navegador nesta alteração.

Cookies são HttpOnly e não são lidos pelo JavaScript. O cliente obtém CSRF antes das escritas, renova após autenticação e não repete mutações automaticamente. Senhas/desafios/tokens ficam somente em memória; links removem o token da URL após captura. Nenhum segredo é salvo em localStorage/sessionStorage. BroadcastChannel sinaliza mudança de sessão entre abas, sem transmitir credenciais.

O aviso surge cinco minutos antes do prazo calculado pelo servidor. Polling atualiza permissões, mas não a inatividade. Atividade só é sinalizada com interação recente em aba visível, no máximo uma vez por minuto. O servidor sempre decide a validade.

Formulários não prometem recuperação após recarregar a página; copie dados não salvos antes da expiração. A interface possui estilos para 320 px. Após autorização, os 9 testes do cliente de API e o build passaram; ainda não houve validação visual/E2E ou acessibilidade em navegador. Não foi executado lint.

Rascunhos não secretos da edição de funcionário e criação de convite sobrevivem em memória da aba à expiração, para retomar ao entrar com o mesmo usuário. Logout, troca de conta e reload descartam esses rascunhos. A mudança de sessão em outra aba limpa o CSRF local; a próxima escrita obtém o token atual, sem repetir automaticamente a operação.

SMTP do Gmail está configurado; autenticação e entrega real ainda precisam ser confirmadas. “Envio solicitado” e “submetido ao SMTP” não significam entrega confirmada.

# Formatação do projeto

O projeto usa 4 espaços em Java e XML e 2 espaços em JavaScript, JSX, CSS,
HTML, JSON e YAML. As regras estão em `.editorconfig` e `.prettierrc.json`
na raiz do repositório.

A partir da pasta `frontend`, execute `npm run format` para formatar o projeto
ou `npm run format:check` para conferir a formatação.

As migrações existentes do Flyway, os scripts do Maven Wrapper, os arquivos
gerados e o arquivo de dependências `package-lock.json` ficam fora da formatação.
As migrações são preservadas para manter seus checksums.
