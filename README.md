# JamsHub

Sistema de gestão para distribuidoras, com cadastros, compras, estoque e vendas integrados.

O projeto está em desenvolvimento, com o núcleo do MVP implementado e melhorias em andamento.

## Funcionalidades

- Cadastro de clientes, fornecedores, produtos e depósitos.
- Produtos simples e com variações, apresentações e opções de compra por fornecedor.
- Controle de estoque por depósito, com saldos, reservas e histórico de movimentações.
- Pedidos de venda com preços, descontos, reserva de estoque e despacho.
- Pedidos de compra com recebimento parcial ou total e entrada no estoque.
- Autenticação, cargos, permissões, gerenciamento de sessões e auditoria de segurança.

## Tecnologias

- **Backend:** Java 21, Spring Boot, Spring Security e Spring Data JPA.
- **Frontend:** React, Vite e Tailwind CSS.
- **Banco de dados:** PostgreSQL, com migrations gerenciadas pelo Flyway.

## Estrutura

- `backend/` — API e regras de negócio.
- `frontend/` — interface web.
- `docs/` — documentação e registros de desenvolvimento.

## Próximas etapas

- Módulo financeiro.
- Previsão de demanda e sugestões de compra.
- Integração com atendimento automatizado pelo WhatsApp.
- Integração com API para emissão de NF-e.

O despacho de vendas atual registra a saída comercial e de estoque, sem emissão de NF-e. Os fluxos da interface continuam em validação.
