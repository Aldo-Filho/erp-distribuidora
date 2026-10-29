# Administrador inicial por migration — plano de implementação

**Status: revertido por solicitação do usuário em 28/10/2026.** A V5 foi removida e o bootstrap interativo foi restaurado, desativado por padrão. Os itens abaixo registram a alternativa anterior; não representam o procedimento atual. Nenhuma conta foi alterada nesta reversão.

**Objetivo:** substituir o bootstrap interativo por uma V5 SQL com e-mail e hash Argon2 editáveis antes da primeira aplicação.
**Arquitetura:** Flyway cria o usuário inicial apenas quando não houver administrador ativo com as três permissões de gestão. Manter a guarda de segurança, buscar cargo por permissões e preservar contas existentes. Campos não preenchidos interrompem um banco novo com erro claro, sem criar conta com senha inválida.
**Stack:** PostgreSQL/Flyway e autenticação Argon2 já existentes.
**Autorização:** proposta confirmada pelo usuário nesta conversa, incluindo marcadores para preenchimento manual. Execução no checkout atual, sem descartar alterações, testes/build/lint, aplicação SQL ou commits.

- [x] Criar V5__insert-initial-administrator.sql, com marcadores REPLACE_WITH_ADMIN_EMAIL e REPLACE_WITH_ARGON2_PASSWORD_HASH; não editar V1–V4.
- [x] Remover AdministratorBootstrap, campos/configurações e bloqueio HTTP exclusivo do bootstrap.
- [x] Adaptar fixtures existentes para uma cópia de migrations com credenciais geradas apenas para testes isolados e atualizar a expectativa de versão para V5; não criar nova bateria nem executar testes.
- [x] Atualizar instruções e progresso; revisar diretamente referências, fluxo de preservação e preenchimento obrigatório.

**Revisão:** banco já possui administrador → nenhuma troca de credenciais; banco novo com marcadores → falha antes de inserir; e-mail colidindo com outro usuário → falha, sem promoção; cargo ausente → falha; senha precisa ser hash compatível com o encoder atual. Após a aplicação, não editar a V5 daquele banco.

**Resultado:** implementação em arquivos e revisão direta concluídas. Nenhum build/lint, teste, SQL ou startup executado. Os resultados de testes anteriores não validam esta alteração.

