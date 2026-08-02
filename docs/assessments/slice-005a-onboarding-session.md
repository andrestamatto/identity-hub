# SLICE-005A — Sessão de onboarding iniciada pelo SaaS

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-01
>
> **Branch:** `feat/slice-005a-onboarding-session`
>
> **Base:** `develop` em `65eefc6`

## 1. Objetivo

Criar a intenção segura e idempotente que vinculará uma futura autenticação à
aquisição mantida pelo SaaS, sem emitir `OnboardingIdentityProof`, `Membership` ou
token humano nesta fatia.

A fundação mínima de `IH-MVP-011` é antecipada porque a autenticação de máquina é
pré-condição de segurança para `IH-MVP-006`, conforme a ordem adaptável da
estratégia de migração.

## 2. Contrato observável

`POST /integration/v1/onboarding-sessions` recebe:

- `Idempotency-Key` em header;
- UUID do browser client;
- referência opaca da aquisição;
- redirect URI exata;
- PKCE code challenge S256.

A aplicação e o cliente de máquina são derivados do access token. A resposta
contém somente identificador opaco da sessão e expiração. A primeira criação
responde `201 Created`; replay idêntico responde `200 OK` com o mesmo conteúdo.

## 3. Critérios de aceitação

- token deve possuir assinatura RS256, issuer, audience
  `identityhub-integration-api`, validade e scope `onboarding:write` válidos;
- token ausente ou inválido recebe `401`; token sem scope recebe `403`;
- cliente autenticado desabilitado, não projetado ou sem scope local não inicia
  sessão;
- browser client deve estar ativo, projetado, pertencer à mesma aplicação e
  conter a redirect URI exata;
- aplicação e cliente de máquina não são aceitos no corpo;
- acquisition reference, idempotency key e PKCE são validados e não entram em
  logs ou resposta;
- replay idêntico devolve a mesma sessão; mesma chave com payload diferente
  recebe conflito sem alterar a sessão;
- sessão expira em dez minutos e não contém usuário, pagamento ou plano;
- criação não gera `Membership`, prova ou token de negócio;
- concorrência não cria duas sessões para a mesma chave e cliente;
- PostgreSQL e Keycloak reais comprovam o contrato persistente e o token de
  máquina, incluindo ausência de refresh token.

## 4. Limites e rollback

Ficam fora da fatia: página hospedada, login, consumo da sessão, emissão e troca
da prova, pagamento e provisionamento de `Membership`.

Antes de produção, rollback é revert do PR. As migrations são aditivas; tabelas e
linhas não serão removidas automaticamente. Após uso real, rollback de código
preserva os registros até uma política de retenção aprovada.

O edge permanece desabilitado por padrão e pode ser interrompido imediatamente
com `IDENTITYHUB_ONBOARDING_ENABLED=false`. A V11 adiciona somente os scopes dos
clientes de máquina; a V12 adiciona somente `onboarding_session` e seu índice.
Nenhuma migration destrutiva ou backfill foi introduzido.

## 5. Implementação

- `MachineSettings` aceita somente scopes conhecidos e persiste sua ordem;
- a projeção Keycloak associa `onboarding:write` apenas ao cliente de máquina e
  o client scope acrescenta a audience `identityhub-integration-api`;
- uma cadeia Spring Security dedicada valida RS256, issuer, audience, validade,
  cliente `ih-machine-{uuid}` e authority `SCOPE_onboarding:write`;
- `ResolveOnboardingOrigin` exige máquina e browser ativos, projeção aplicada,
  mesma aplicação e redirect URI exata;
- `OnboardingSession` mantém somente bindings e digests necessários, usa 256
  bits aleatórios no identificador e expira em dez minutos;
- `insert ... on conflict do nothing` torna criação e replay atômicos no
  PostgreSQL; colisão semântica falha com `409 Conflict`;
- o contrato HTTP não aceita aplicação ou máquina no corpo, responde com
  `Cache-Control: no-store` e não devolve aquisição ou IDs internos.
- timer de iniciação registra somente outcomes limitados, sem aplicação,
  cliente, aquisição, idempotency key ou sessão como tags.

Não foram adicionados eventos: iniciar uma intenção temporária não produz fato
externo nem side effect assíncrono nesta fatia. Eventos serão avaliados quando a
identidade for vinculada ou uma `Membership` mudar de estado.

## 6. Evidência TDD e gates

O ciclo iniciou vermelho por ausência dos validadores/conversores da Integration
API e passou após a implementação mínima. Testes de domínio e aplicação cobrem
validade, PKCE, input, binding e conflito. Testes HTTP negativos cobrem `401`,
`403`, schema estrito e ausência de autoridade no corpo.

Evidência real adicional demonstra:

- duas inserções PostgreSQL concorrentes com a mesma chave produzem uma criação,
  um replay e uma única linha;
- Keycloak 26.7 projeta SPA e máquina, emite Client Credentials com audience e
  scope exatos e sem refresh token;
- o JWT real atravessa a cadeia do IdentityHub, cria a sessão no PostgreSQL e o
  replay retorna o mesmo corpo sem expor aquisição, aplicação ou máquina.

Gates executados em 2026-08-01:

- Windows: `.\gradlew.bat clean build` verde em 1m29s, incluindo `bootJar`,
  Checkstyle, ArchUnit e JaCoCo; 241 testes, zero falhas/erros e 33 integrações
  Docker ignoradas no host, como esperado;
- Linux/WSL: `./gradlew clean build` verde em 10m27s com 241 testes, zero
  falhas, erros ou skips contra PostgreSQL 17.10, Keycloak 26.7.0 e Mailpit
  1.30.6 reais;
- Linux/WSL focado durante o TDD: PostgreSQL concorrente e round-trip
  Keycloak/HTTP verdes em 6m11s;
- harness: `python3 -m unittest scripts/test_local_dev.py` — quatro testes
  verdes; o realm local inclui o client scope e `local-env run` habilita a
  borda somente no ambiente de desenvolvimento.
