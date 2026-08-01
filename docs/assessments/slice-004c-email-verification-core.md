# SLICE-004C — Núcleo durável de verificação de e-mail

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-004c-email-verification-core`
>
> **Base:** `develop` em `d26f581`

## 1. Objetivo

Provar o núcleo seguro de `IH-MVP-004` entre a identidade pendente e a entrega
durável de e-mail, sem antecipar o endpoint público de cadastro. O fluxo cria um
desafio de finalidade única, entrega um link oficial e confirma a posse antes de
habilitar a conta no Keycloak.

## 2. Resultado observável

- solicitação interna gera segredo CSPRNG e validade exata de 30 minutos;
- somente SHA-256 do segredo permanece na tabela de desafios;
- o outbox conserva o link em claro apenas enquanto a entrega ainda precisa dele
  e o remove ao entregar ou falhar definitivamente;
- nova solicitação invalida o desafio ativo anterior;
- no máximo três solicitações por identidade e aplicação são aceitas em 15 minutos;
- segredo válido marca o desafio como usado e torna o usuário habilitado e com
  e-mail verificado no Keycloak;
- confirmação exige que o e-mail atual no Keycloak ainda corresponda exatamente
  à identidade normalizada vinculada ao challenge;
- segredo inválido incrementa tentativas e o quinto erro encerra o desafio;
- segredo expirado, usado, substituído ou de outra finalidade falha fechado;
- replay depois do sucesso não produz segundo efeito.

## 3. Limites arquiteturais

- `identity` possui challenge, finalidade, digest, validade, tentativas e
  orquestração da confirmação;
- `communication` possui somente a solicitação e entrega durável do e-mail;
- Keycloak continua proprietário de `enabled`, `emailVerified` e credenciais;
- a colaboração `identity -> communication` usa contrato público pequeno;
- nenhum módulo acessa repositório ou entidade interna do outro;
- o schema privado do Keycloak nunca é consultado.

## 4. Segurança e consistência

- tokens, links e digests não entram em logs, erros, métricas ou diagnóstico
  administrativo;
- comparação do digest usa tempo constante;
- reenvio e tentativas são impostos no domínio/aplicação, independentemente do edge;
- a transação PostgreSQL grava challenge e outbox de forma atômica;
- confirmação remota é idempotente: crash após o Keycloak e antes do commit pode
  ser repetido sem reverter a verificação;
- resposta pública futura continuará genérica e receberá limitação complementar
  por IP antes do cadastro ser exposto.

## 5. Persistência e rollback

Flyway V10 adicionará `email_verification_challenge` e o payload efêmero de
verificação ao outbox. Migrations aplicadas permanecem forward-only. Antes de
produção, rollback é revert do PR e recriação do ambiente descartável; nenhuma
limpeza automática de banco é autorizada.

## 6. Fora de escopo

- formulário ou endpoint público de cadastro;
- criação automática de `Membership`;
- telefone, login social ou `OnboardingIdentityProof`;
- escolha do provedor SMTP de produção (`PD-001`);
- branding final do e-mail e da página hospedada;
- WAF/captcha e limitação por IP, que serão gate da borda pública.

## 7. Evidência

Os testes cobrem:

- challenge de 30 minutos, finalidade implícita exclusiva, comparação constante,
  expiração, uso único e encerramento após cinco erros;
- composição obrigatória entre cadastro pendente e início da verificação;
- três solicitações por 15 minutos e substituição do challenge anterior;
- transação única para challenge e outbox, incluindo rollback sintético;
- digest SHA-256 de segredo CSPRNG de 256 bits sem persistência do valor original;
- entrega SMTP real no Mailpit e limpeza do link após sucesso ou falha terminal;
- recusa de reprocessamento quando o link já foi eliminado;
- Keycloak 26.7 real habilitando e verificando a conta de forma idempotente;
- recusa permanente quando o e-mail atual diverge do challenge;
- negação cruzada entre as service accounts de usuários e clients;
- redação de challenge, segredo, comando, entrega e mensagem em `toString`.

Gates executados em 2026-08-01:

- Windows: `.\gradlew.bat clean build` — verde em 1m37s, incluindo `bootJar`,
  Checkstyle, ArchUnit e regressões; 198 testes, zero falhas/erros e 30
  Testcontainers ignorados por ausência de Docker direto no host;
- Linux/WSL: `./gradlew clean build` — verde em 10m45s com os 198 testes, zero
  falhas, erros ou ignorados, usando Keycloak, PostgreSQL e Mailpit reais;
- harness: `python3 -m unittest scripts/test_local_dev.py` — 3 testes verdes;
- bootstrap local idempotente: `local-env up` verde sobre o realm preexistente.

## 8. Gate autônomo

A fatia satisfaz seu contrato interno e está autorizada para publicação segundo
`docs/autonomous-delivery.md`. O cadastro público permanece fechado. A próxima
fatia deve aplicar resposta anti-enumeração e limitação por IP/destino antes de
expor `BeginLocalRegistration` à internet.
