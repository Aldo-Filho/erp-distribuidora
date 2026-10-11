# Módulo de clientes — desenho da primeira versão

Data: 2026-10-10
Status: desenho aprovado no chat; documento aguardando revisão do usuário.

## Objetivo e escopo

Implementar o módulo de clientes no front-end do ERP, integrado à API existente e seguindo o visual e a organização de Produtos e Armazéns. A primeira versão permite listar, pesquisar, cadastrar e editar clientes PF/PJ, ativar/desativar e gerenciar seus endereços.

Ficam fora desta versão: exclusão de clientes, seleção/gestão de tabelas de preços, integração com pedidos, consulta automática de CEP, novas bibliotecas e validação de dígitos verificadores de CPF/CNPJ.

## Navegação e listagem

- Reconhecer `#clientes` na inicialização e nas mudanças do fragmento da URL, mantendo Produtos e Armazéns acessíveis e a navegação pelo histórico do navegador.
- Usar o item Clientes já existente na barra lateral. Preservar o comportamento responsivo e as margens do conteúdo.
- Mostrar título, quantidade de clientes cadastrados e botão Novo cliente.
- Tabela com nome/razão social, CPF/CNPJ, telefone, situação e ações.
- Busca única por nome/razão social, nome fantasia, documento, telefone, e-mail ou WhatsApp. Combinar a busca com filtros de tipo (todos/PF/PJ) e situação (todos/ativos/inativos).
- Filtrar localmente a coleção retornada por `/customer/search`, como nos módulos existentes. Comparação textual sem diferença entre maiúsculas e minúsculas; documentos e telefones comparados sem pontuação.
- Exibir carregamento, falha com opção de tentar novamente, coleção vazia e ausência de resultados para os filtros.
- Ações por cliente: editar, abrir endereços e ativar/desativar. Confirmar mudanças de situação e impedir envios repetidos durante a operação.

## Formulário de cliente

Cadastro e edição usam modal responsivo. Os campos são apresentados conforme o tipo de pessoa escolhido:

| Campo | PF | PJ | Propriedade da API |
| --- | --- | --- | --- |
| Tipo de pessoa | Obrigatório | Obrigatório | `personType` |
| Nome / razão social | Obrigatório | Obrigatório | `legalName` |
| CPF / CNPJ | Obrigatório | Obrigatório | `taxId` |
| Nome fantasia | Oculto | Obrigatório | `tradeName` |
| Nascimento | Opcional | Oculto | `birthDate` |
| Inscrição municipal/estadual | Ocultas | Opcionais | `municipalReg`, `stateReg` |
| Telefone | Obrigatório | Obrigatório | `phone` |
| E-mail e WhatsApp | Opcionais | Opcionais | `email`, `whatsapp` |

- Cliente novo inicia como PF e ativo. A situação é alterada pela ação da tabela, evitando depender do campo `active` que o construtor atual ignora no cadastro.
- Remover espaços nas extremidades e enviar CPF/CNPJ e telefones sem caracteres de formatação. Respeitar os comprimentos das colunas existentes.
- Validar preenchimento obrigatório, formato de data e e-mail quando preenchido. Não apresentar a validação de preenchimento do documento como validação de autenticidade.
- Na troca PF/PJ dentro do formulário, preservar valores digitados em estado local. Enviar no cadastro apenas os campos pertinentes ao tipo escolhido.
- Na edição, não apagar silenciosamente campos ocultos ao trocar o tipo. Enviar as alterações dos campos apresentados; a API valida o tipo e os dados resultantes.
- Campos opcionais de texto podem ser limpos com string vazia na edição. O `PATCH` atual não permite remover nascimento enviando `null`; manter a data existente quando não houver nova data e informar essa limitação no formulário de edição quando pertinente.
- Não enviar `priceTableId` nesta versão; preservar o vínculo existente ao editar.

## Primeiro endereço opcional

- No cadastro, colocar a seção Endereço abaixo dos dados do cliente, no mesmo modal, com uma opção explícita para incluir endereço, inicialmente desativada.
- Ao ativá-la, mostrar tipo (principal por padrão), estado, cidade, rua, bairro, número, complemento e CEP.
- Exigir os campos previstos no contrato atual, inclusive complemento. Não inventar valores para satisfazer campos obrigatórios.
- Enviar o endereço como objeto `address` no `POST /customer`. Quando desativado, omitir `address`.
- Cliente e endereço são salvos na mesma transação. Se a requisição falhar, preservar o formulário completo para nova tentativa. Não implementar o fluxo anterior de duas requisições nem um estado de cliente parcialmente criado.
- Na edição de cliente, gerenciar endereços exclusivamente pelo modal próprio.

## Endereços existentes

- O botão Endereços abre modal e carrega `/customerAddress/search?customerId=...`.
- Abrir em visualização. Cada endereço oferece edição e exclusão com confirmação; oferecer adicionar quando houver tipo disponível.
- Traduzir `MAIN` como Principal, `BILLING` como Cobrança e `SHIPPING` como Entrega.
- Permitir somente um endereço por tipo, conforme a restrição do banco. Ao adicionar, oferecer os tipos disponíveis; ao editar, incluir o tipo do endereço atual.
- Enviar `customerId` no cadastro de endereço e usar `/customerAddress/{id}` para editar/excluir, sem permitir transferir um endereço para outro cliente pela interface.
- Atualizar a lista depois das operações. Se o carregamento falhar, apresentar nova tentativa e não tratar a falha como lista vazia.
- Não fechar modal nem permitir operações concorrentes durante salvamento/exclusão. Erros preservam os valores digitados.

## Organização do código

- `CustomersPage.jsx`: carregamento, filtros, tabela, ações e abertura dos modais.
- `CustomerForm.jsx`: formulário de cadastro/edição e adaptação PF/PJ.
- `CustomerAddressModal.jsx`: visualização e gerenciamento de endereços existentes.
- `CustomerAddressFields.jsx`: campos de endereço compartilhados entre cadastro inicial e gerenciamento.
- `customerConfig.js`: valores iniciais, opções, limites e construção dos dados enviados à API.
- `customerFilters.js`: busca e filtros locais.
- Reutilizar `Modal`, `Dropdown`, `api` e ícones pelos nomes originais de `lucide-react`.
- Usar React e Tailwind já instalados. Inputs com rótulos, botões com identificação acessível e mensagens de erro identificadas como alertas. Campos em uma coluna em telas pequenas e duas onde houver espaço; tabela com rolagem horizontal quando necessário.

## API: mudanças realizadas e ajuste restante

Já implementado nesta conversa:

- `RequestCustomerDTO.AddressDTO` opcional, validado quando presente.
- `CustomerService.register` transacional, registrando o endereço com o ID gerado do cliente.
- Validação de nome/documento por PF/PJ e nome fantasia obrigatório para PJ, aplicada ao cadastro e aos dados resultantes da edição.
- Nome fantasia e inscrições opcionais para PF; inscrições opcionais para PJ; limpeza de campos opcionais de texto já adaptados.
- O usuário retirou a obrigatoriedade de e-mail e WhatsApp do DTO; telefone continua obrigatório.

Ajuste restante aprovado no desenho: permitir limpar e-mail e WhatsApp no `PATCH`, interpretando string vazia como remoção e ausência/`null` como preservação, seguindo o padrão dos demais textos opcionais. Não alterar contratos de outros módulos nem migrações existentes.

## Revisão e critérios de conclusão

Não criar nem executar testes automatizados, build ou lint por padrão, conforme as preferências do projeto. Fazer revisão direta das alterações e conferir o diff sem afirmar validação em execução.

A implementação deverá contemplar a navegação, todos os fluxos descritos, validação coerente com a API, proteção contra operações repetidas e tratamento de carregamento/erro/vazio. Preservar alterações preexistentes no projeto e limitar as mudanças ao módulo de clientes, sua integração no App e o ajuste restante da API.

Após a aprovação deste documento, preparar um plano de implementação para revisão do usuário e escolha da forma de execução.
