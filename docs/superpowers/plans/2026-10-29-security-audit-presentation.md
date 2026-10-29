# Auditoria de acesso — plano de implementação

**Objetivo:** implementar a apresentação aprovada de responsáveis, alvos e rótulos legíveis, mantendo nomes históricos nos novos eventos.
**Arquitetura:** migration aditiva, snapshots no SecurityAuditService, campos adicionais no EventView e rótulos locais no frontend.
**Stack:** Spring/JDBC, PostgreSQL/Flyway, React.
**Especificação:** docs/superpowers/specs/2026-10-29-security-audit-presentation-design.md.
**Execução:** no checkout atual, preservando alterações existentes. Revisão direta, sem testes/build/lint, banco ou commits, por instrução do usuário.

- [x] Acrescentar actor_name, actor_type, target_name e target_type em V5__enrich-security-audit.sql. A V5 anterior de administrador foi removida e não será restaurada.
- [x] Centralizar snapshots por tipo de ação no serviço existente; identificar operações de sistema e ações com identidade comprovada. Preservar nome dos alvos excluídos e manter o registro na transação original.
- [x] Acrescentar campos à consulta e filtro de nome, preservando filtros por códigos/ID, paginação e permissão.
- [x] Traduzir todas as ações/resultados existentes, filtros e alvos na tela; manter ID e indicação de nomes históricos indisponíveis.
- [x] Ajustar expectativa de migrations na suíte existente sem executá-la; revisar SQL, chamadas, referências e apresentação; documentar limites.

Revisão: eventos antigos sem nomes; exclusão de usuário/cargo; usuário renomeado; falhas anônimas; recuperação pública versus administrativa; envio assíncrono; ações desconhecidas; filtros/paginação existentes. Não registrar senhas, tokens, corpos de requisição ou conteúdo do e-mail.

Revisão final: leitura direta e revisão independente somente leitura, sem defeitos bloqueantes identificados. Verificados tipos de alvo, parâmetros JDBC, campos da consulta, códigos emitidos versus traduções, snapshots nas exclusões e permissão de leitura. Isso não comprova execução ou aparência no navegador.

Decisão de revisão: SESSION_LIMIT_REACHED identifica o usuário após senha/token de desafio válidos e rechecagem da conta, usando recordAsUser; as tentativas de login sem credencial válida continuam não identificadas. O desafio ainda não concede sessão; a auditoria registra a identidade comprovada, sem modificar o fluxo de admissão. Não foram executados testes para esta mudança.

Limite histórico: nomes não armazenados em eventos anteriores não serão reconstruídos usando dados atuais. A migration nova será aplicada no fluxo normal de inicialização, não nesta entrega. Não houve acesso ou alteração do banco por ferramentas nesta tarefa.
