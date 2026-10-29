# Para rodar o projeto

`npm run dev`

## Acesso e administração

Configurar primeiro o backend pelo application.yaml, conforme backend/README.md, incluindo PostgreSQL e SMTP. Se ainda não houver administrador, criá-lo pelo bootstrap em um terminal interativo e desativar esse modo antes de usar a aplicação. Não é necessário selecionar perfil ou cadastrar variáveis de ambiente. O navegador usa /api pelo proxy existente para localhost:8080. O roteador react-router-dom foi instalado com lockfile atualizado; Clientes está integrado conforme a seção específica abaixo.

Rotas públicas: /login, /accept-invitation, /forgot-password e /reset-password.
Rotas privadas: /customers, /account, /access/users, /access/roles, /access/invitations, /security/sessions, /security/settings e /security/audit.

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


## Clientes e informações do registro

A rota `/customers` e o menu Clientes exigem `customers.read`. A busca usa paginação real do backend (25/50/100), pesquisa Nome/CPF-CNPJ/Contato e filtros de tipo/situação. O backend deve estar atualizado para `Page<Customer>` e para os contratos descritos em `backend/README.md`.

Cadastro exige `customers.create`; edição e ativação/inativação exigem `customers.update`. A tela não exclui clientes. Endereços possuem inclusão/edição com essas permissões e exclusão separada com `customers.delete`. Edição mantém PF/PJ fixo. Endereço inicial opcional faz parte do POST único; complemento é opcional e requer a V6 aplicada pelo fluxo autorizado.

Tabela existente é opcional. As opções ativas são consultadas somente com `prices.read`; sem permissão ou com erro, o vínculo atual é preservado e cadastro sem tabela continua disponível. A tabela atual inativa permanece identificada. Remover tabela e nascimento usa flags explícitas no PATCH; os metadados não são enviados. Rascunhos de Clientes/endereços ficam somente em memória por usuário/registro, descartados ao salvar/cancelar/logout/trocar conta/reload. Não há garantia de recuperação após reload.

Consultar CEP é uma ação explícita pelo ViaCEP, com HTTPS público, sem credenciais ou CSRF do ERP, timeout de oito segundos, cancelamento e proteção contra respostas antigas/correções manuais. Correções preenchidas pedem confirmação antes de substituir; número e complemento permanecem manuais. Falha não impede preenchimento manual. A hospedagem futura precisa permitir `https://viacep.com.br` na política connect-src, se houver CSP.

Detalhes de Clientes e Funcionários e a área do Cargo selecionado mostram Informações do registro com as datas da API. Cargo possui somente criação. Timestamp sem offset conserva o calendário/hora recebido; instante com offset usa o fuso do navegador. Data nula de atualização significa Sem atualização registrada. Isso não altera permissões de auditoria nem cria histórico/autoria.

A entrega recebeu revisão direta, sem execução no navegador, testes automatizados, build ou lint. Resultados anteriores de autenticação não comprovam estes fluxos. Roteiro manual futuro: `docs/superpowers/plans/2026-10-29-customers-progress.md`, na raiz.

Em conferência posterior solicitada pelo usuário, a tela em branco foi reproduzida no navegador: havia conflito de resolução entre CustomerForm.jsx e o auxiliar customerForm.js no Windows. O auxiliar agora é customerFormData.js e a importação do componente usa a extensão .jsx. Após a correção, a instância local exibiu a tela de login. Os fluxos autenticados de Clientes ainda não foram validados; não houve execução de testes automatizados, build ou lint.
