# Optional PATCH Flags Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans after approval. Follow AGENTS.md, preserve local changes and implement directly in the project. Do not delegate. Limit verification to the targeted retests described here, continuing the user's explicit testing request.

**Goal:** Permitir omissão ou null nas flags de remoção de customer, product e stock sem provocar HTTP 400 ou NullPointerException.

**Architecture:** Trocar apenas as três flags de ação de boolean para Boolean nos records. Interpretar somente Boolean.TRUE como remoção, nas verificações de contradição e nas operações dos services. Preservar a configuração global de desserialização e as demais regras de negócio.

**Tech Stack:** Java 21, Spring Boot, Jackson e ferramentas locais de verificação já existentes, sem dependências novas.

**Spec:** Defeito reproduzido em backend/target/module-verification-feedback.md e solução discutida com o usuário. Plano aprovado pelo usuário, implementado e verificado por retestes direcionados.

## Global Constraints

- Conferir Git e preservar as alterações existentes, inclusive bloqueios de stock, regras de histórico, validações e melhorias dos outros módulos.
- Manter nomes e posições dos campos nos records, URLs e formatos de resposta. Não adicionar @NotNull às flags opcionais.
- Seguir customer na apresentação e distribuição de responsabilidades: records transportam dados; services decidem a ação.
- Não criar construtores de normalização nos DTOs nem alterar a configuração global do Jackson.
- Não alterar migrations, frontend, dependências ou outras flags sem relação com o defeito.
- A verificação proposta é um reteste direcionado do problema já autorizado na rodada anterior, em banco isolado. Não usar o banco erp, executar lint ou repetir toda a auditoria.
- Compilar o backend atualizado para os retestes; não reutilizar classes compiladas antes da correção. Não criar uma suite ampla ou testes sem relação com o problema.

## Comportamento esperado

| Flag | Valor associado | Resultado |
| --- | --- | --- |
| Omitida, null ou false | Omitido ou null | Preservar o valor atual |
| Omitida, null ou false | Informado | Validar e atualizar normalmente |
| true | Omitido ou null | Remover |
| true | Informado, inclusive zero | Rejeitar a combinação antes dos setters |

As associações são priceTableId e categoryId; o limite é maxQuantity. Remover algo que já está ausente permanece permitido. Os campos comuns do PATCH continuam preservados quando omitidos ou nulos.

## Review Focus

1. Flag ausente deve permitir desserialização e entrada no service.
2. Flag null não pode sofrer unboxing em if, && ou outra expressão.
3. Boolean.TRUE deve controlar tanto a remoção quanto a rejeição de contradição.
4. PATCH vazio deve preservar os dados; recurso inexistente deve retornar 404 em vez de falhar na desserialização.
5. Retestes devem usar classes atualizadas e banco isolado, sem repetir expectativas antigas de supplier.

## Task 1: Corrigir os DTOs e as seis verificações

**Modificar:**
- `backend/src/main/java/com/pi/erp/customer/PatchCustomerDTO.java`
- `backend/src/main/java/com/pi/erp/customer/CustomerService.java`
- `backend/src/main/java/com/pi/erp/product/PatchProductDTO.java`
- `backend/src/main/java/com/pi/erp/product/ProductService.java`
- `backend/src/main/java/com/pi/erp/stock/PatchStockItemDTO.java`
- `backend/src/main/java/com/pi/erp/stock/StockItemService.java`

- [x] Trocar os componentes removePriceTable, removeCategory e removeMaxQuantity de boolean para Boolean, sem modificar os demais componentes dos records.
- [x] Em CustomerService, usar Boolean.TRUE.equals(data.removePriceTable()) nas duas verificações: conflito com priceTableId e remoção efetiva.
- [x] Em ProductService, usar Boolean.TRUE.equals(data.removeCategory()) nas duas verificações correspondentes.
- [x] Em StockItemService, usar Boolean.TRUE.equals(data.removeMaxQuantity()) nas duas verificações correspondentes.
- [x] Preservar o fluxo dos branches, as mensagens, as buscas de associações, as validações dos valores finais e o momento anterior aos setters.
- [x] Buscar todas as referências às três flags e conferir que não restam condições com unboxing implícito. Revisar os seis arquivos e o diff.

## Task 2: Registrar a regra no AGENTS.md

- [x] Substituir somente a orientação que permite presumir false como padrão de flags primitivas opcionais.
- [x] Determinar Boolean para flags opcionais de ação no PATCH, sem @NotNull, interpretadas com Boolean.TRUE.equals(...) no service. Omissão, null e false não executam a ação.
- [x] Manter a orientação de rejeitar remoção junto com um novo valor e as regras gerais de preservação do PATCH. Não proibir boolean primitivo em todo o projeto.

## Task 3: Repetir os cenários afetados

- [x] Compilar as classes atuais e iniciar a aplicação contra banco dedicado de verificação. Manter logs e scripts temporários em backend/target.
- [x] Para cada um dos três módulos, criar fixture com a associação ou máximo preenchido; conferir PATCH vazio e edição de outro campo sem informar a flag.
- [x] Para cada flag, conferir omissão, null e false preservando o valor atual, além de true removendo-o.
- [x] Conferir nova associação ou máximo com flag omitida, null e false; todas essas formas devem permitir a atualização válida.
- [x] Conferir true junto com novo valor: retornar 400 e manter o registro intacto. Incluir maxQuantity=0 para não confundir zero com ausência.
- [x] Conferir remoção repetida de associação ou máximo já ausente.
- [x] Conferir PATCH vazio com ID inexistente nos três módulos: retornar 404.
- [x] Encerrar os processos iniciados para a verificação e registrar resultados, comandos e limites em backend/target/module-verification-feedback.md, preservando o histórico da rodada anterior.
- [x] Atualizar o progresso deste plano e informar os arquivos alterados. Se houver falha, registrar o caso real em vez de afirmar aprovação geral.

## Handoff

Plano implementado em seis arquivos de produção e uma orientação no AGENTS.md. Compilação atualizada e 46 verificações HTTP direcionadas passaram, em banco isolado.

## Registro da execução

- Task 1: concluída. As três flags são Boolean e as seis verificações usam Boolean.TRUE.equals(...). Nenhuma referência direta às flags com unboxing permaneceu nos services verificados.
- Task 2: concluída. AGENTS.md orienta flags opcionais de ação com Boolean, sem @NotNull, e comparação explícita com true, preservando a rejeição de combinações contraditórias.
- Task 3: concluída. Maven 3.9.16 executou compile em modo offline e recompilou o código atualizado com sucesso. Os 46 casos HTTP, incluindo fixtures, passaram: PATCH vazio, omissão/null/false, atualização com cada variante, remoção, remoção repetida, contradição sem mutação, máximo zero contraditório e recurso inexistente.
- Aplicação executada na porta 18080 contra o banco dedicado backend_verify_* na porta 55432. O banco erp não foi usado. Aplicação e PostgreSQL de verificação foram encerrados ao final.
- Scripts, logs e resultados estão em backend/target: verification-flags.cjs, verification-flags-compile.log, verification-flags.log e verification-flags-results.json. O relatório anterior foi preservado e recebeu uma seção adicional.
- Decisão de execução: seguir a implementação direta e os retestes direcionados aprovados; não criar worktree, commits ou delegação. Não repetir a auditoria completa nem executar lint.
- Limite: esta rodada cobre os cenários das flags; não representa repetição integral dos 229 casos anteriores.
