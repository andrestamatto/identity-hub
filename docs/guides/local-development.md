# Desenvolvimento local

## 1. Objetivo

O harness local permite executar e verificar as APIs administrativas das
`SLICE-001` e os incrementos `SLICE-002A` a `SLICE-002C` com PostgreSQL 17 e
Keycloak 26.7 reais. A aplicação
continua sendo iniciada pelo Gradle no WSL; somente as dependências de
infraestrutura usam containers.

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

Com a função genérica `local-env` configurada no Git Bash, execute na raiz do
repositório:

```sh
# Sobe PostgreSQL e Keycloak e configura o realm de forma idempotente
local-env up

# Inicia o IdentityHub na porta 8080; mantenha este terminal aberto
local-env run

# Em outro terminal, inicia o login administrativo hospedado
local-env token

# Valida aplicação, API, SPA, BFF, projeção, reconciliação e credencial
local-env smoke

# Exibe os containers ou encerra a infraestrutura
local-env status
local-env down
```

Sem o alias, os mesmos comandos podem ser executados no PowerShell a partir da
raiz do repositório com
`pwsh.exe -NoProfile -File ./scripts/local-dev.ps1 <ação>`.

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

## 5. Projeção dos clientes da aplicação

O bootstrap cria um cliente confidencial interno e concede somente
`realm-management/manage-clients` ao seu service account. O escopo completo do
realm permanece desabilitado. O segredo é gerado localmente, guardado em
`~/.local/state/identityhub/management-client.secret` com modo `0600` e nunca é
impresso ou armazenado no repositório.

A ação `smoke`:

1. cadastra uma `ClientApplication` e confirma seu replay idempotente;
2. configura uma API protegida com projeção `PENDING`;
3. aguarda o worker criar o cliente bearer-only e registrar `APPLIED`;
4. solicita reconciliação explícita e confirma nova aplicação idempotente;
5. configura uma SPA pública com redirects e origins exatos em loopback;
6. aguarda o worker criar o cliente Authorization Code com PKCE `S256` e
   registrar `APPLIED`;
7. configura um BFF confidencial com redirect exato em loopback;
8. aguarda o cliente Authorization Code + PKCE `S256` ficar `APPLIED`;
9. solicita uma credencial gerada ao Keycloak e a descarta sem exibi-la.

O smoke nunca imprime nem persiste a credencial do BFF. Para uma integração
real, a resposta de emissão deve ser copiada diretamente para o secret manager
do consumidor. Repetir a operação gera uma nova credencial e invalida a anterior;
ela não funciona como recuperação.

Se a sessão administrativa tiver expirado, execute novamente `local-env token`.

## 6. Dados e reinicialização

O comando `down` remove containers e redes, mas preserva os volumes nomeados dos
dois bancos. A remoção desses volumes apaga dados e credenciais TOTP e, portanto,
deve ser feita manualmente somente após inspeção explícita do alvo.

O console administrativo do Keycloak existe apenas em `127.0.0.1` neste harness.
Ele não integra o contrato público do IdentityHub.
