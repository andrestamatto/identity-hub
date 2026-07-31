# SLICE-002C — BFF confidencial e credencial de uso único

> **Status:** approved
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-002c-confidential-web-client`
>
> **Base:** `develop` em `c8cb0c5`

## 1. Objetivo

Entregar o terceiro incremento interno de `IH-MVP-002`: um `PLATFORM_ADMIN`
configura um `ApplicationClient` do tipo BFF e o IdentityHub projeta no Keycloak
um cliente confidencial para Authorization Code, autenticado por client secret.

Esta fatia também entrega o momento seguro de emissão da credencial inicial sem
persistir seu valor no IdentityHub. Ela não implementa ainda a sessão do usuário
final, cookies do BFF nem troca de authorization code.

## 2. Contrato administrativo

Configuração do BFF:

```json
{
  "type": "BFF",
  "key": "auto-radar-bff",
  "redirectUris": [
    "https://app.example.com/login/oauth2/code/identityhub"
  ]
}
```

O tipo `BFF` exige redirects e rejeita `audience` e `webOrigins`. O navegador
conversa com o backend por cookie de sessão; ele não chama o Keycloak diretamente
por CORS.

Após a projeção ficar `APPLIED`, um admin com TOTP solicita a credencial em:

```text
POST /internal/admin/client-applications/{applicationId}/clients/{clientId}/credentials/client-secret
```

A resposta é marcada com `Cache-Control: no-store` e `Pragma: no-cache`. O valor
não aparece no diagnóstico comum e não é gravado no PostgreSQL, na outbox, em
logs ou no harness.

## 3. Invariantes

- cliente confidencial e não bearer-only;
- Authorization Code habilitado com PKCE `S256`;
- client authentication por `client-secret`;
- implicit, direct access, service account e authorization services desabilitados;
- redirects absolutos, exatos, sem wildcard, userinfo ou fragment;
- produção aceita somente HTTPS;
- desenvolvimento aceita HTTP somente em loopback;
- de 1 a 10 redirects únicos, em ordem determinística;
- emissão rejeitada antes de `APPLIED`, para outro tipo ou outro application id;
- cada chamada de emissão regenera a credencial; nunca recupera a anterior;
- representações textuais internas e HTTP redigem o segredo.

## 4. Persistência e projeção

A migration `V6__add_bff_application_client.sql` amplia o constraint de tipo e
renomeia a tabela de redirects SPA para redirects browser compartilhados. As
origens permanecem exclusivas da SPA. Configuração e operação `PENDING` continuam
atômicas e reutilizam a outbox, leases, retry e reconciliação existentes.

O adapter usa apenas a Admin REST oficial do Keycloak. O segredo gerado permanece
armazenado no motor; o IdentityHub mantém somente a configuração engine-neutral.
A reconciliação não altera a credencial já emitida.

## 5. Segurança da credencial

A criação assíncrona não transporta segredo na outbox. A operação separada chama
o endpoint de regeneração do Keycloak somente depois de confirmar o estado local
`APPLIED`, entrega o valor naquela resposta e o descarta em seguida.

Uma repetição invalida imediatamente a credencial anterior no perfil estável do
Keycloak usado pelo MVP. Portanto, esta operação serve para emissão inicial ou
recuperação antes do deploy do consumidor. Rotação de cliente já em produção com
sobreposição controlada permanece fora desta fatia; ela não deve ser automatizada
até existir um procedimento compatível com o secret manager do consumidor. A
rotação nativa com duas credenciais está em preview no Keycloak 26.7 e não foi
habilitada silenciosamente.

## 6. Evidência automatizada

Os testes cobrem:

- invariantes de redirects e transporte por ambiente;
- criação, replay e projeção `PENDING`;
- round-trip no PostgreSQL 17 real com Flyway V1–V6;
- representação confidencial e idempotente no adapter;
- bloqueio de segredo para client pendente ou application id diferente;
- resposta não cacheável e `toString()` redigido;
- falha externa sanitizada;
- negação explícita para anônimo, admin sem TOTP e auditor;
- fluxo real Admin API → PostgreSQL/outbox → Keycloak 26.7 → `APPLIED`;
- duas credenciais reais distintas comparadas somente por hash;
- reconciliação sem alterar a credencial atual.

Gates executados em 2026-07-31:

- Windows: `.\gradlew.bat clean build --console=plain` — verde em 1m04s;
- Linux/WSL: `./gradlew clean build --no-daemon --console=plain` — verde em
  6m19s, com PostgreSQL 17 e Keycloak 26.7 reais via Testcontainers;
- resultado da suíte Gradle: 133 testes, sem falhas, erros ou ignorados;
- harness: `python3 -m unittest scripts/test_local_dev.py -v` — 3 testes
  verdes.

## 7. Harness para revisão manual

```sh
local-env run
local-env token
local-env smoke
```

O smoke configura o BFF, aguarda `APPLIED`, solicita uma credencial e a descarta
sem imprimir ou persistir o valor. Esse caminho prova segurança operacional; ele
não fornece uma credencial para uso por aplicação real.

## 8. Fora de escopo

- sessão do usuário e cookie opaco do BFF;
- login hospedado e troca de authorization code;
- tokens, refresh e logout do consumidor;
- rotação de secret em produção com sobreposição;
- integração automática com Coolify ou outro secret manager;
- BFF starter ou exemplo no projeto consumidor;
- cliente de máquina.

## 9. Revisão humana

Aprovada em 2026-07-31. A fatia está autorizada para commit e push da branch.
