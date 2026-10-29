# Fornecedores — cadastro e endereços

Data: 29/10/2026. Estado: desenho aprovado pelo usuário para elaboração do plano técnico. A aprovação do desenho não autoriza iniciar implementação.

## Objetivo e divisão da entrega

Entregar o cadastro operacional de fornecedores no frontend atual, integrado ao backend e às permissões existentes. Reutilizar a experiência de Clientes, preservando as diferenças de domínio.

Abordagens discutidas: entregar cadastro e vínculos juntos, com maior escopo imediato, ou dividir em duas etapas, alinhando os vínculos ao módulo de Produtos. O usuário escolheu duas etapas.

1. Agora: cadastro, edição, detalhes, consulta paginada, situação e endereços.
2. Na etapa de Produtos: gestão dos vínculos com produtos, marcas e categorias; custo, código do produto no fornecedor, preferência e prazo específico por produto.

Preservar os vínculos existentes. Não reescrever seus endpoints nem criar telas de produtos nesta entrega.

## Evidência atual — leitura estática

- SupplierController/Service oferecem consulta, cadastro, PATCH e exclusão; a busca retorna List<Supplier>, sem paginação.
- Cadastro de fornecedor e primeiro endereço usam uma transação no service.
- Razão social e documento já são obrigatórios. Telefone é opcional atualmente e passará a obrigatório por decisão explícita do usuário.
- O schema identifica tax_id como CNPJ; não existe personType em fornecedores.
- Complemento do endereço já é opcional no schema, DTOs e service. Não repetir a migration de Clientes.
- Endereços permitem repetir o tipo, mas possuem verificação e índice contra duplicidade da identidade do endereço.
- A navegação atual possui Clientes; não possui a tela de Fornecedores.
- Este levantamento não comprova funcionamento em execução. Nenhum teste, build, lint, SQL ou migration foi executado.

## Decisões confirmadas pelo usuário

- Somente fornecedores pessoa jurídica, identificados por CNPJ; sem cadastro de pessoa física nesta entrega.
- Telefone obrigatório. E-mail e WhatsApp opcionais.
- Interface oferece ativação/inativação, sem exclusão definitiva do fornecedor.
- Primeiro endereço opcional no mesmo formulário de cadastro; demais endereços gerenciados posteriormente.
- Cadastro e endereços agora; vínculos na etapa de Produtos.

## Proposta de comportamento para aprovação

### Lista e navegação

- Rota e item de navegação de Fornecedores, disponíveis com suppliers.read.
- Lista compacta: razão social/nome fantasia, CNPJ, telefone, situação e ações permitidas.
- Busca por Nome (razão social ou nome fantasia), Documento ou Contato; filtro de situação Todos/Ativos/Inativos.
- Paginação real no backend: 25 por página, opções 50 e 100, máximo 100; ordenação estável por razão social e ID.
- Mudança explícita de contrato: GET /supplier/search passa de lista para Page<Supplier>. Preservar os filtros atuais, inclusive produto, marca, categoria e endereço, embora os controles de vínculos fiquem para depois. Conferir consumidores no plano técnico.
- Reaproveitar as proteções de Clientes contra respostas antigas, envio repetido, falha confundida com lista vazia e perda de formulário durante erro.

### Cadastro, edição e detalhes

- Formulários em modal responsivo, seguindo Clientes. Obrigatórios: razão social, CNPJ e telefone. Opcionais: nome fantasia, e-mail, WhatsApp e prazo médio de entrega.
- Prazo médio em dias inteiros e não negativos, preservando a validação existente. Zero é diferente de não informado. A possibilidade de limpar um prazo existente precisa de intenção explícita no contrato; detalhar no plano técnico, sem reinterpretar null do PATCH como remoção.
- Aplicar obrigatoriedade de telefone também no backend. PATCH omitido/null preserva; tentativa de limpar telefone é rejeitada. Em registros legados sem telefone, exigir o preenchimento ao salvar a edição cadastral; ativação/inativação isolada deve continuar possível, sem exigir corrigir campos não relacionados.
- Não regravar documentos históricos em lote nem introduzir silenciosamente novas regras de dígito verificador. O plano deve explicitar o formato de entrada/envio de CNPJ compatível com o armazenamento atual e tratar duplicidade sem aceitar CPF como novo cadastro.
- Cancelar não grava. Formulário sem mudanças não envia PATCH. Confirmar descarte de alterações e preservar rascunho em memória conforme Clientes.
- Detalhes somente de leitura, incluindo endereço(s), prazo e bloco Informações do registro.
- Datas conforme 2026-10-29-record-metadata-design.md: Criado em e Última atualização, com a mesma permissão de consulta. Não inventar datas para endereços nem apresentar atualização do fornecedor como histórico de seus filhos.

### Situação e histórico

- Ativar/inativar com confirmação e suppliers.update. Preservar cadastro, vínculos e histórico.
- Não remover o DELETE existente da API como efeito colateral da ausência dessa ação na tela.
- Não alterar as regras existentes de compras e vínculos nesta entrega; eventuais lacunas sobre fornecedor inativo devem ser registradas para a etapa correspondente.

### Endereços e CEP

- Cadastro inicial opcional, enviado junto ao fornecedor. Se habilitado, exigir os campos já obrigatórios da API: país, estado, cidade, bairro, logradouro, número, CEP e tipo. Complemento opcional.
- Gerenciamento posterior em modal próprio: consultar, incluir, editar e excluir endereço, conforme permissões. Não oferecer transferência de endereço entre fornecedores.
- Tipos Principal, Cobrança e Entrega. Permitir mais de um endereço do mesmo tipo, respeitando a identidade e a proteção contra duplicidades do backend.
- Reutilizar a consulta ViaCEP já entregue em Clientes: preenchimento assistido, edição manual, erro/CEP inexistente/indisponibilidade distintos, timeout, cancelamento e proteção de edição contra resposta atrasada. Não enviar sessão ou CSRF ao serviço público.
- ViaCEP se aplica a endereços brasileiros. Preservar país e preenchimento manual; não tentar consultar endereços estrangeiros no ViaCEP.
- Complemento omitido/null no PATCH preserva; vazio limpa, conforme contrato atual. Não alterar V1 nem criar migration para uma nulabilidade já existente.

### Permissões

- suppliers.read: consultar lista, detalhes e endereços.
- suppliers.create: cadastrar fornecedor com endereço inicial e incluir endereços posteriormente.
- suppliers.update: editar fornecedor, situação e endereços existentes.
- suppliers.delete: excluir endereço; jamais exibir exclusão física do fornecedor nesta tela.
- Mapeamento conferido por leitura de auth/config/SecurityConfig.java. Preservar essa divisão; não ampliar permissões nem fazer da ocultação de botão a única proteção.

## Critérios de conclusão e limites

- Lista paginada com busca/filtros combinados, ordenação estável e estados de carregamento/erro/vazio distinguíveis.
- Cadastro e edição com telefone obrigatório, PJ, duplicidade tratada, opcionais e PATCH preservados; tratamento explícito dos registros legados.
- Endereço inicial atômico com o cadastro e gerenciamento posterior sem copiar a restrição de um endereço por tipo de Clientes.
- CEP com preenchimento manual preservado e acesso protegido nas telas e na API.
- Metadados reais visíveis com permissão de consulta; nenhum vínculo existente perdido.
- Revisão direta dos trechos e consumidores alterados. Preparar roteiro de validação manual; executar verificações somente quando autorizado, respeitando AGENTS.md. Não confundir implementação com validação em produção.
- Registrar dependências de schema apenas se a análise técnica comprovar necessidade; usar migrations novas, sem aplicar automaticamente.
- Sem alterações em autenticação, financeiro, fiscal, WhatsApp ou regras de compras nesta entrega. Sem novas dependências, commits ou mudanças de ambiente por padrão.

## Próximo passo

Revisar este desenho com o usuário. Após aprovação, elaborar o plano técnico com arquivos, contratos, tratamento do legado, permissões verificadas e roteiro manual. Implementação em outro chat quando solicitada.
