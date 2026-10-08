# Para rodar o projeto

`npm run dev`

# Formatação do projeto

O projeto usa 4 espaços em Java e XML e 2 espaços em JavaScript, JSX, CSS,
HTML, JSON e YAML. As regras estão em `.editorconfig` e `.prettierrc.json`
na raiz do repositório.

A partir da pasta `frontend`, execute `npm run format` para formatar o projeto
ou `npm run format:check` para conferir a formatação.

As migrações existentes do Flyway, os scripts do Maven Wrapper, os arquivos
gerados e o arquivo de dependências `package-lock.json` ficam fora da formatação.
As migrações são preservadas para manter seus checksums.
