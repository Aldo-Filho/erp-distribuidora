# JamsHub — Frontend

Interface web do JamsHub, desenvolvida com React, Vite e Tailwind CSS. Permite gerenciar cadastros, compras, vendas, estoque e acessos conforme as permissões do usuário.

## Requisitos

- Node.js 22.12 ou superior e npm.
- Backend em execução em `http://localhost:8080`.

## Como rodar

No terminal, a partir da raiz do repositório:

```powershell
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173` ou o endereço indicado pelo Vite. Entre com uma conta cadastrada; para criar o primeiro administrador, consulte o [README do backend](../backend/README.md#primeiro-acesso).

## Conexão com a API

As chamadas usam `/api`, encaminhado pelo proxy em `vite.config.js` para `http://localhost:8080`. Se mudar a porta do backend, ajuste esse destino.

Para os links de convite e recuperação, mantenha `erp.auth.public-base-url` no backend igual ao endereço do frontend. Não é necessário criar um arquivo `.env` para a configuração local atual.
