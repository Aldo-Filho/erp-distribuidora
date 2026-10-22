# Exception Handling Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans after approval. Follow AGENTS.md. Preserve existing changes and implement directly in the project. Do not delegate, create or execute tests, run build, lint or SQL without explicit authorization.

**Goal:** Completar a cobertura de erros da API mantendo o contrato ResponseErrorDTO, os status existentes e a organização atual da pasta exception.

**Architecture:** Manter GlobalExceptionHandler como @RestControllerAdvice, com handlers específicos e respostas explícitas em ResponseEntity. Acrescentar tratamento de leitura/conversão de entrada e conflitos de bloqueio. Substituir o bloco genérico comentado por um fallback que preserve erros HTTP reconhecidos pelo framework e registre falhas inesperadas no servidor.

**Tech Stack:** Java 21, Spring Boot, Spring MVC, Spring Data e SLF4J já disponíveis no projeto. Nenhuma dependência nova.

**Spec:** Feedback anterior da pasta exception e AGENTS.md. Plano aprovado pelo usuário e implementado diretamente no projeto, com revisão estática.

## Global Constraints

- Conferir Git e preservar as alterações existentes, especialmente os handlers já adicionados em tarefas anteriores.
- Não criar nem executar testes automatizados, build, lint ou comandos SQL. A verificação prevista é leitura direta dos trechos e do diff.
- Usar customer como referência de apresentação: quatro espaços, imports explícitos, assinaturas em múltiplas linhas, records simples e mensagens em inglês com ponto final.
- Preservar o contrato JSON: status int, error String, message String e timestamp LocalDateTime. Não acrescentar campos ou mudar o formato temporal.
- Manter ResourceNotFoundException e seu uso nos services. Não criar BusinessException, hierarquias de exceções ou utilitários genéricos apenas para uniformizar o código.
- Manter 404 para ResourceNotFoundException, 400 para IllegalArgumentException e validações, e 409 para DataIntegrityViolationException.
- Não trocar automaticamente as exceções lançadas nos módulos, mover regras de negócio para o handler ou alterar o contrato de findOne ausente retornar 204.
- Não retornar mensagens brutas de desserialização, SQL, classes Java ou stack traces em erros de entrada, persistência e falhas inesperadas.
- Não alterar controllers, services, frontend, configuração global, dependências ou migrations nesta implementação.

## Arquivos previstos

1. `backend/src/main/java/com/pi/erp/exception/GlobalExceptionHandler.java`: handlers novos, fallback e apresentação dos métodos existentes.
2. `backend/src/main/java/com/pi/erp/exception/ResponseErrorDTO.java`: apenas apresentação do record, um campo por linha.

ResourceNotFoundException permanece sem alterações. Nenhum arquivo novo de produção é necessário.

## Contrato de respostas

| Origem | Status | Mensagem |
| --- | --- | --- |
| ResourceNotFoundException | 404 | Preservar mensagem específica atual |
| IllegalArgumentException dos services | 400 | Preservar mensagem específica atual |
| MethodArgumentNotValidException | 400 | Preservar mensagens de campos, ordenadas como hoje |
| HttpMessageNotReadableException | 400 | `Request body is missing or contains invalid data.` |
| MethodArgumentTypeMismatchException | 400 | `Invalid value for parameter '<name>'.` |
| PessimisticLockingFailureException | 409 | `Operation conflicts with another transaction. Please try again.` |
| DataIntegrityViolationException | 409 | Preservar mensagem genérica atual |
| Outros erros HTTP reconhecidos pelo Spring | Status original | Mensagem segura correspondente ao status, sem detalhes internos |
| Exceção inesperada sem status HTTP reconhecido | 500 | `An unexpected error occurred.` |

- Conflitos de bloqueio não devem ser classificados como entrada inválida. Não executar retries automáticos no handler.
- Na conversão de parâmetro, usar somente o nome do parâmetro; não repetir o valor rejeitado ou o nome interno do tipo Java.
- Preservar headers dos erros HTTP do Spring, como Allow em 405 e os headers fornecidos em 415. Não transformar esses erros em 500.
- O fallback também deve respeitar exceções anotadas com @ResponseStatus, se encontradas. Não expor seu reason automaticamente.

## Review Focus

1. JSON inválido, enum/data incompatível e corpo obrigatório ausente devem retornar 400 no formato existente.
2. ID textual em endpoint com Long deve ter mensagem segura e não ser confundido com recurso inexistente.
3. Falhas de bloqueio traduzidas pelo Spring devem receber 409; os handlers atuais de integridade e validação devem continuar aplicáveis.
4. Fallback não pode converter 404, 405, 415 ou outros status HTTP reconhecidos em 500 nem descartar seus headers.
5. Erros inesperados devem ter diagnóstico no servidor e mensagem genérica ao cliente, sem criar handlers ambíguos ou recursão no fallback.

## Task 1: Alinhar apresentação e preservar os handlers existentes

- [x] Ler os records e controllers equivalentes de customer para confirmar o estilo de apresentação, sem copiar suas regras de negócio.
- [x] Formatar ResponseErrorDTO com um campo por linha, mantendo nomes, ordem e tipos.
- [x] Padronizar as assinaturas e as linhas em branco de GlobalExceptionHandler, sem alterar os quatro handlers existentes ou a ordenação das mensagens de validação.
- [x] Manter a montagem explícita de ResponseEntity e ResponseErrorDTO; não introduzir base class ou fábrica de respostas para reduzir poucas linhas repetidas.

## Task 2: Tratar leitura e conversão de entrada

**Interfaces:** Adicionar `ResponseEntity<ResponseErrorDTO> handleUnreadableMessage(HttpMessageNotReadableException exception)` e `ResponseEntity<ResponseErrorDTO> handleTypeMismatch(MethodArgumentTypeMismatchException exception)`.

- [x] Usar @ExceptionHandler específico para cada uma das duas classes, com imports explícitos do Spring.
- [x] Em handleUnreadableMessage, retornar 400 com a mensagem definida na tabela, sem usar exception.getMessage() nem a mensagem da causa.
- [x] Em handleTypeMismatch, retornar 400 usando exception.getName() para identificar o parâmetro, sem incluir exception.getValue() ou detalhes de conversão.
- [x] Manter MethodArgumentNotValidException como o tratamento das violações de constraints, sem substituí-lo pelos handlers novos.

## Task 3: Tratar conflitos de bloqueio

**Interface:** Adicionar `ResponseEntity<ResponseErrorDTO> handlePessimisticLocking(PessimisticLockingFailureException exception)`.

- [x] Usar a exceção traduzida de org.springframework.dao, preservando a abstração já usada para DataIntegrityViolationException.
- [x] Cobrir suas subclasses, incluindo CannotAcquireLockException, sem registrar handlers duplicados para a mesma hierarquia.
- [x] Retornar 409, error Conflict, a mensagem definida na tabela e timestamp atual.
- [x] Não capturar toda DataAccessException como conflito; falhas de conexão e outros problemas de persistência não são automaticamente falhas de bloqueio.
- [x] Não alterar os bloqueios ou as transações de stock e price. A tarefa é tratar a resposta quando essas operações falharem.

## Task 4: Substituir o bloco comentado por fallback seguro

**Interface:** Adicionar `ResponseEntity<ResponseErrorDTO> handleUnexpected(Exception exception)` como último handler lógico, com @ExceptionHandler(Exception.class).

- [x] Usar Logger e LoggerFactory de SLF4J, com campo estático final associado a GlobalExceptionHandler. Não adicionar dependência ou serviço de logging.
- [x] Antes de classificar como 500, verificar se a exceção implementa org.springframework.web.ErrorResponse: preservar getStatusCode() e getHeaders(), montar o DTO existente e não copiar automaticamente os detalhes de getBody().
- [x] Para exceções com @ResponseStatus, resolver a anotação com o mecanismo de annotations do Spring e preservar o código indicado. Não reutilizar reason como mensagem pública.
- [x] Para erros HTTP reconhecidos abaixo de 500, usar mensagem segura derivada da descrição padrão do status; se não houver descrição conhecida, usar `Request could not be processed.`. Preservar o status numérico mesmo quando não houver HttpStatus correspondente.
- [x] Para status 5xx reconhecidos e para exceções inesperadas, registrar a exceção no servidor e retornar `An unexpected error occurred.`. Preservar o status 5xx original quando reconhecido; usar 500 para falha sem status.
- [x] Registrar a falha com mensagem fixa e a exceção para diagnóstico; não acrescentar corpo da requisição, credenciais ou headers aos logs.
- [x] Remover o bloco antigo comentado em português. Manter ResourceNotFoundException, IllegalArgumentException, validação, integridade e bloqueio nos handlers específicos.
- [x] Não converter o handler em ResponseEntityExceptionHandler nesta tarefa; preservar o estilo atual e evitar conflito entre overrides e @ExceptionHandler.

## Task 5: Revisão estática e registro

- [x] Revisar os dois arquivos e o diff, preservando alterações anteriores.
- [x] Conferir assinaturas, pacotes de imports, precedência dos handlers específicos e ausência de anotações ambíguas.
- [x] Conferir a hierarquia disponível das exceções de bloqueio e as interfaces do Spring utilizadas, por leitura das fontes/documentação compatíveis com as dependências locais, sem compilar.
- [x] Percorrer por leitura os casos da tabela: JSON inválido, corpo ausente, ID textual, validação existente, recurso ausente, duplicidade e falha de bloqueio.
- [x] Conferir explicitamente as ramificações de 405/415 com preservação dos headers do ErrorResponse e a resolução de @ResponseStatus.
- [x] Conferir que mensagem pública e log são distintos nos erros inesperados e que nenhuma causa interna é devolvida ao cliente.
- [x] Confirmar que DTO, status existentes e regras dos services não foram alterados.
- [x] Atualizar este plano com os arquivos modificados e registrar que testes, build, lint e verificação HTTP em runtime não foram executados.

## Handoff

Plano implementado em dois arquivos existentes na pasta exception. A revisão estática não substitui a verificação HTTP em execução, que não foi realizada.

## Registro da execução

- Task 1: concluída. ResponseErrorDTO tem um campo por linha, mantendo nomes, ordem e tipos. Assinaturas dos handlers e chamadas encadeadas foram alinhadas à apresentação de customer. Os handlers existentes de recurso ausente, argumento inválido, validação e integridade foram preservados.
- Task 2: concluída. HttpMessageNotReadableException e MethodArgumentTypeMismatchException recebem HTTP 400 com mensagens próprias; valor rejeitado e detalhes internos não são devolvidos pelos handlers novos.
- Task 3: concluída. PessimisticLockingFailureException recebe HTTP 409, abrangendo suas subclasses, sem converter toda falha de acesso a dados em conflito.
- Task 4: concluída. O fallback reconhece ErrorResponse e @ResponseStatus, preserva status e headers disponíveis, e registra falhas 5xx no servidor com mensagem genérica ao cliente. O bloco comentado antigo foi removido.
- Task 5: concluída por revisão direta do código e do diff. APIs e hierarquia foram conferidas nas fontes locais do Spring 7.0.8: ErrorResponse, ResponseEntity, HttpStatusCode, ResponseStatus, AnnotationUtils, CannotAcquireLockException e exceções HTTP de método/tipo de conteúdo. Foram conferidos Allow em 405, Accept/Accept-Patch em 415 e a resolução dos aliases de ResponseStatus.
- Foram modificados somente GlobalExceptionHandler e ResponseErrorDTO, além deste registro. ResourceNotFoundException, os services, controllers, frontend, dependências e migrations não foram alterados.
- Decisão de execução: seguir o plano aprovado no checkout atual, sem worktree, commits ou arquivos auxiliares; manter o registro neste documento.
- Decisão de verificação: respeitar AGENTS.md e o plano aprovado, com revisão pelo próprio implementador, sem delegação e sem criar ou executar testes, build, lint ou SQL.
- Limites: não houve compilação, inicialização do Spring ou requisições HTTP. As conclusões sobre os fluxos resultam da leitura do código e das fontes locais das dependências.
