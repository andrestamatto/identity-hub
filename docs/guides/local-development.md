# Desenvolvimento local

## 1. Objetivo

O harness local permite executar e verificar a API administrativa da `SLICE-001`
com PostgreSQL 17 e Keycloak 26.7 reais. A aplicação continua sendo iniciada pelo
Gradle no WSL; somente as dependências de infraestrutura usam containers.

O harness é exclusivo de desenvolvimento. Ele não define topologia, credenciais
ou procedimentos de produção.

## 2. Pré-requisitos

- Ubuntu no WSL;
- Java 21 disponível no Ubuntu;
- Docker Engine acessível pelo usuário do WSL;
- Python 3, sem bibliotecas externas.

Quando `docker compose` não existe no WSL, o harness usa automaticamente a imagem
oficial e fixada `docker:29.1.3-cli` como executor do Compose. A imagem é baixada
uma única vez pelo Docker. A instalação nativa opcional no Ubuntu 24.04 é:

```sh
sudo apt update
sudo apt install docker-compose-v2
```

O arquivo local padrão fica fora do repositório em:

```text
C:\Users\re040282\dev\env\identity-hub.local.env
```

Ele contém segredos locais e não deve ser commitado, compartilhado ou usado em
outro ambiente.

## 3. Comandos

Execute os comandos abaixo no PowerShell, a partir de qualquer diretório:

```powershell
# Sobe PostgreSQL e Keycloak e configura o realm de forma idempotente
& C:\Users\re040282\dev\repo\projects\identity-hub\scripts\local-dev.ps1 up

# Inicia o IdentityHub na porta 8080; mantenha este terminal aberto
& C:\Users\re040282\dev\repo\projects\identity-hub\scripts\local-dev.ps1 run

# Em outro terminal, inicia o login administrativo hospedado
& C:\Users\re040282\dev\repo\projects\identity-hub\scripts\local-dev.ps1 token

# Cadastra e consulta uma ClientApplication pela API real
& C:\Users\re040282\dev\repo\projects\identity-hub\scripts\local-dev.ps1 smoke

# Exibe os containers ou encerra a infraestrutura
& C:\Users\re040282\dev\repo\projects\identity-hub\scripts\local-dev.ps1 status
& C:\Users\re040282\dev\repo\projects\identity-hub\scripts\local-dev.ps1 down
```

O parâmetro `-EnvironmentFile` permite selecionar outro arquivo local sem mudar
o conteúdo versionado.

## 4. Primeiro login

A ação `token` usa o OAuth 2.0 Device Authorization Grant do cliente público de
desenvolvimento. Ela mostra uma URL e um código, mas nunca imprime o access token.

No primeiro acesso:

1. abra a URL exibida;
2. informe o usuário e a senha presentes no arquivo local;
3. cadastre o TOTP no Google Authenticator ou em outro aplicativo compatível;
4. conclua o login e aguarde a confirmação no terminal.

O token fica em `~/.local/state/identityhub/local-admin.token`, no filesystem do
WSL, com modo `0600`. A ação `down` remove esse arquivo. O cliente público
não aceita Resource Owner Password Credentials e o acesso administrativo exige
`PLATFORM_ADMIN` e `amr=totp`.

## 5. Dados e reinicialização

O comando `down` remove containers e redes, mas preserva os volumes nomeados dos
dois bancos. A remoção desses volumes apaga dados e credenciais TOTP e, portanto,
deve ser feita manualmente somente após inspeção explícita do alvo.

O console administrativo do Keycloak existe apenas em `127.0.0.1` neste harness.
Ele não integra o contrato público do IdentityHub.
