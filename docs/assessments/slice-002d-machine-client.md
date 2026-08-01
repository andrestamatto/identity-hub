# SLICE-002D — Cliente de máquina confidencial

> **Status:** approved
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-002d-machine-client`
>
> **Base:** `develop` em `54bcf52`

## 1. Objetivo

Fechar os tipos de `ApplicationClient` de `IH-MVP-002` permitindo que um
`PLATFORM_ADMIN` configure e projete no Keycloak um cliente de máquina
confidencial, preparado para Client Credentials.

Esta fatia configura o canal e emite sua credencial inicial. Ela não emite nem
valida access tokens, não define scopes de negócio e não gerencia memberships.

## 2. Contrato

```json
{
  "type": "MACHINE",
  "key": "auto-radar-membership-provisioner"
}
```

`audience`, `redirectUris` e `webOrigins` são incompatíveis e, se presentes,
causam rejeição. Após a projeção `APPLIED`, a credencial usa o mesmo endpoint
administrativo não cacheável dos demais clientes confidenciais.

## 3. Invariantes

- cliente confidencial, não público e não bearer-only;
- Service Accounts habilitado;
- Authorization Code, Implicit, Direct Access e Authorization Services
  desabilitados;
- full scope desabilitado;
- nenhum redirect, origin ou PKCE;
- segredo não entra no banco, outbox, logs ou harness;
- emissão exige client habilitado, projeção `APPLIED`, application id correto,
  `PLATFORM_ADMIN` e TOTP.

## 4. Limites de segurança

A habilitação técnica de Service Accounts não concede scopes ou permissões de
negócio. `IH-MVP-011` definirá tokens Client Credentials, audience, scopes,
ausência de refresh token e claims sem usuário. `IH-MVP-010` definirá a gestão
de memberships limitada à própria aplicação.

Repetir a emissão regenera o segredo e invalida o anterior. Rotação em produção
com sobreposição continua fora do MVP atual.

## 5. Persistência e consistência

Flyway V7 amplia os constraints para `MACHINE`, cuja configuração não possui
linhas filhas. Configuração e operação `PENDING` permanecem na mesma transação.
A projeção reutiliza outbox, lease, retry, idempotência e reconciliação sem broker.

## 6. Evidência automatizada

- domínio e replay idempotente;
- contrato HTTP tipado e rejeição de campos incompatíveis;
- round-trip PostgreSQL 17 com Flyway V1–V7;
- representação e reconciliação no Keycloak 26.7;
- emissão real não cacheável sem persistência do segredo;
- negações administrativas existentes;
- harness local sem impressão da credencial;
- builds canônicos Windows e Linux/WSL.

Gates executados em 2026-07-31:

- Windows: `.\gradlew.bat clean build --console=plain` — verde em 1m06s;
- Linux/WSL: `./gradlew clean build --no-daemon --console=plain` — verde em
  6m57s, com PostgreSQL 17 e Keycloak 26.7 reais via Testcontainers;
- resultado da suíte Linux/WSL: 141 testes, sem falhas, erros ou ignorados;
- harness: `python3 -m unittest scripts/test_local_dev.py -v` — 3 testes
  verdes.

## 7. Revisão humana

Aprovada em 2026-07-31. A fatia está autorizada para commit e push da branch.
