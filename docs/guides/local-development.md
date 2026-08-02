# Desenvolvimento local

## 1. Objetivo

O harness local permite executar e verificar as APIs administrativas das
`SLICE-001`, os incrementos `SLICE-002A` a `SLICE-002C`, a entrega SMTP da
`SLICE-003A`, a fundação de identidade local da `SLICE-004B`, a baseline de
login hospedado da `SLICE-005D` e a solicitação de recuperação da `SLICE-006A`
com PostgreSQL 17, Keycloak 26.7 e Mailpit 1.30.6 reais. A aplicação
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
# Sobe PostgreSQL, Keycloak e Mailpit e configura o realm de forma idempotente
local-env up

# Inicia o IdentityHub na porta 8080; mantenha este terminal aberto
local-env run

# Em outro terminal, inicia o login administrativo hospedado
local-env token

# Valida aplicação, API, SPA, BFF, máquina, projeção e credenciais
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

O endereço da interface do Mailpit é informado por `local-env up` e permanece
restrito ao loopback. No arquivo local padrão, ele é
`http://127.0.0.1:8025`. O Mailpit captura e-mails sem entregá-los na internet e
nunca deve ser configurado em produção.

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

Esse fluxo é exclusivo do administrador local. O login de usuário final ocorre
na página hospedada do realm por Authorization Code com PKCE. O comando `up`
mantém idempotentemente a política de 15 a 64 caracteres, perfil que não exige
nomes além do e-mail, mensagens públicas genéricas, espera progressiva após cinco
falhas e eventos `LOGIN`/`LOGIN_ERROR`. Não existe endpoint do IdentityHub que
receba a senha nem comando de Resource Owner Password Credentials.

A solicitação de recuperação usa
`POST /public/v1/applications/{applicationIdentifier}/password-recoveries` e
recebe somente `email`. Conta elegível e inexistente retornam o mesmo `202`, sem
IDs ou prova. Quando elegível, o link de finalidade única chega ao Mailpit, expira
em 15 minutos e permanece no outbox apenas até a entrega. A definição da nova
senha pertence à próxima subfatia e ainda não está disponível neste ponto.

## 5. Projeção dos clientes da aplicação

O bootstrap cria um cliente confidencial interno e concede somente
`realm-management/manage-clients` ao seu service account. O escopo completo do
realm permanece desabilitado. O segredo é gerado localmente, guardado em
`~/.local/state/identityhub/management-client.secret` com modo `0600` e nunca é
impresso ou armazenado no repositório.

Uma segunda service account, `identityhub-identity-management`, é isolada da
primeira e recebe somente `manage-users`, `view-users` e `query-users`. Seu
segredo fica em
`~/.local/state/identityhub/identity-management-client.secret`, também com modo
`0600`. A conta de identidades não gerencia clients, enquanto a conta de clients
não consulta usuários. O bootstrap também mantém a política local de senhas em
15 a 64 caracteres.

O processo local também recebe `IDENTITYHUB_PUBLIC_BASE_URI` como
`http://127.0.0.1:8080`. Somente loopback pode usar HTTP; qualquer host externo
exige HTTPS. Links de verificação são persistidos no outbox apenas enquanto a
entrega está pendente e são removidos após sucesso ou falha terminal. O Mailpit
permite inspecionar a mensagem sem enviar e-mail à internet. A ação `run` também
habilita a borda pública local, que permanece desabilitada por padrão em qualquer
outro ambiente.

A ação `smoke`:

1. cadastra uma `ClientApplication` e confirma seu replay idempotente;
2. habilita explicitamente a política de autocadastro da aplicação;
3. inicia um cadastro público com resposta genérica e confirma que o e-mail de
   verificação chegou ao Mailpit sem expor credenciais;
4. configura uma API protegida com projeção `PENDING`;
5. aguarda o worker criar o cliente bearer-only e registrar `APPLIED`;
6. solicita reconciliação explícita e confirma nova aplicação idempotente;
7. configura uma SPA pública com redirects e origins exatos em loopback;
8. aguarda o worker criar o cliente Authorization Code com PKCE `S256` e
   registrar `APPLIED`;
9. configura um BFF confidencial com redirect exato em loopback;
10. aguarda o cliente Authorization Code + PKCE `S256` ficar `APPLIED`;
11. solicita uma credencial gerada ao Keycloak e a descarta sem exibi-la;
12. configura um cliente de máquina confidencial sem redirects ou origins;
13. confirma somente Service Accounts habilitado e descarta sua credencial sem
    exibi-la.

O smoke nunca imprime nem persiste credenciais confidenciais. Para uma integração
real, a resposta de emissão deve ser copiada diretamente para o secret manager
do consumidor. Repetir a operação gera uma nova credencial e invalida a anterior;
ela não funciona como recuperação.

Se a sessão administrativa tiver expirado, execute novamente `local-env token`.

## 6. Dados e reinicialização

O comando `down` remove containers e redes, mas preserva os volumes nomeados dos
dois bancos. A remoção desses volumes apaga dados e credenciais TOTP e, portanto,
deve ser feita manualmente somente após inspeção explícita do alvo.

O console administrativo do Keycloak e a interface do Mailpit existem apenas em
`127.0.0.1` neste harness.
Ele não integra o contrato público do IdentityHub.
