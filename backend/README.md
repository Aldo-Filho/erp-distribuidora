# JamsHub — Backend

API do JamsHub, desenvolvida com Java 21, Spring Boot e PostgreSQL. Concentra as regras de cadastros, compras, vendas, estoque e controle de acesso.

## Requisitos

- JDK 21 ou superior.
- PostgreSQL em execução.

O Maven Wrapper está incluído no projeto.

## Como rodar

1. Crie um banco PostgreSQL chamado `erp`.
2. Em `src/main/resources/application.yaml`, ajuste `spring.datasource.url`, `username` e `password` para sua instalação.
3. No PowerShell, a partir da raiz do repositório:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

A API fica em `http://localhost:8080`. O Flyway aplica as migrations na inicialização; para uma instalação nova, utilize um banco vazio.

Para convites e recuperação de senha, configure também `spring.mail`, `erp.auth.mail-from` e `erp.auth.public-base-url` (frontend local: `http://localhost:5173`). Use suas próprias credenciais.

## Primeiro acesso

Se já houver um administrador cadastrado, utilize essa conta. Em um banco novo, após a primeira inicialização, encerre a aplicação e consulte no PostgreSQL:

```sql
SELECT role_id FROM roles WHERE name = 'Administrador' AND active = true;
```

Na pasta `backend`, gere o JAR sem executar testes:

```powershell
.\mvnw.cmd -DskipTests package
```

Em um terminal interativo, execute o comando abaixo, substituindo `ID_DO_CARGO` pelo resultado da consulta:

```powershell
java -jar .\target\erp-0.0.1-SNAPSHOT.jar --erp.auth.bootstrap-enabled=true --erp.auth.bootstrap-role-id=ID_DO_CARGO
```

Informe nome, e-mail e senha de 15 a 128 caracteres. A senha não aparece durante a digitação. Após `Administrator created`, o encerramento do bootstrap é esperado. Inicie novamente com `.\mvnw.cmd spring-boot:run`, mantendo `bootstrap-enabled: false` no YAML. Os demais funcionários entram por convite.
