# Git - Guia Rápido

Este documento contém os comandos básicos utilizados no dia a dia do projeto.

---

# Primeira configuração (apenas uma vez)

Verificar se o Git está instalado:

```bash
git --version
```

Configurar nome e e-mail:

```bash
git config --global user.name "Seu Nome"
git config --global user.email "seuemail@email.com"
```

Verificar configuração:

```bash
git config --list
```

---

# Criar um repositório local

Entrar na pasta do projeto:

```bash
cd caminho/do/projeto
```

Inicializar o Git:

```bash
git init -b main
```

---

# Conectar ao GitHub

Adicionar o repositório remoto:

```bash
git remote add origin https://github.com/usuario/repositorio.git
```

Verificar se foi conectado:

```bash
git remote -v
```

Enviar pela primeira vez:

```bash
git push -u origin main
```

---

# Fluxo diário

## Verificar alterações

```bash
git status
```

---

## Adicionar arquivos

Adicionar tudo:

```bash
git add .
```

Adicionar apenas um arquivo:

```bash
git add arquivo.java
```

---

## Criar um commit

```bash
git commit -m "Descrição da alteração"
```

Exemplos:

```text
Cria módulo de clientes
Implementa autenticação
Corrige cálculo do estoque
Adiciona migrations do banco
```

---

## Enviar alterações ao GitHub

```bash
git push
```

---

# Atualizar o projeto

Baixar alterações do GitHub:

```bash
git pull
```

---

# Ver histórico

```bash
git log
```

Versão resumida:

```bash
git log --oneline
```

---

# Branches

Ver branches:

```bash
git branch
```

Criar uma branch:

```bash
git branch feature/clientes
```

Trocar de branch:

```bash
git switch feature/clientes
```

Criar e trocar ao mesmo tempo:

```bash
git switch -c feature/clientes
```

Voltar para a principal:

```bash
git switch main
```

---

# Comandos úteis

Ver diferenças antes do commit:

```bash
git diff
```

Remover arquivo da área de commit:

```bash
git restore --staged arquivo.java
```

Descartar alterações de um arquivo:

```bash
git restore arquivo.java
```

---

# Fluxo recomendado

1. Altere os arquivos.
2. Execute:

```bash
git status
```

3. Adicione as alterações:

```bash
git add .
```

4. Crie um commit:

```bash
git commit -m "Descrição da alteração"
```

5. Envie para o GitHub:

```bash
git push
```

---

# Convenção de commits

Use mensagens curtas e objetivas.

✅ Bons exemplos

- Cria módulo de produtos
- Implementa autenticação JWT
- Corrige validação de CPF
- Atualiza documentação
- Adiciona migrations do PostgreSQL
- Refatora serviço de pedidos

Evite mensagens como:

- teste
- alteração
- update
- mudanças

---

# Estrutura recomendada de branches

```
main
develop
feature/clientes
feature/produtos
feature/pedidos
feature/financeiro
feature/estoque
```

---

# Dica

Faça commits pequenos e frequentes.

É muito melhor fazer:

```
Cria entidade Cliente
Cria repository Cliente
Cria service Cliente
Cria controller Cliente
```

do que um único commit chamado:

```
Finaliza sistema
```

---

# Referências

- Documentação oficial do Git: https://git-scm.com/docs
- Git Cheat Sheet oficial: https://git-scm.com/cheat-sheet