# SLICE-006C — Desabilitação global de conta

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-02
>
> **Branch:** `feat/slice-006c-disable-global-account`

## 1. Resultado observável

Um `PLATFORM_ADMIN`, autenticado por TOTP há no máximo cinco minutos, desabilita
uma conta global por identificador opaco e motivo explícito. A conta deixa de
iniciar ou renovar sessões em qualquer aplicação, suas sessões existentes são
revogadas e a operação permanece auditável e repetível com segurança.

## 2. Rastreabilidade

- `identityhub-spec.md`: `IH-MVP-017` e partes administrativas de
  `IH-MVP-019` e `IH-MVP-023`;
- `architecture.md`: seções 8.2, 11, 14.4, 14.6 e 15.2;
- `security-model.md`: seções 11, 12.4, 18, 21, 22, 25 e 27;
- ADR-0001, ADR-0002, ADR-0004 e ADR-0010.

## 3. Contrato da fatia

- endpoint privado `POST /internal/admin/user-accounts/{userAccountRef}/disable`;
- corpo estrito com `reason` obrigatório de 10 a 500 caracteres;
- header `Idempotency-Key` obrigatório, opaco e limitado;
- resposta contém somente o identificador e estado da operação, sem perfil ou
  dados pessoais;
- apenas `PLATFORM_ADMIN` com `MFA_TOTP` e `auth_time` recente pode executar;
- a intenção é persistida antes da chamada ao motor e a mesma chave com o mesmo
  comando retoma ou devolve o mesmo resultado;
- a mesma chave com comando semanticamente diferente é rejeitada;
- a conta é desabilitada antes da revogação de suas sessões;
- repetição no motor é idempotente e ainda executa logout quando a conta já está
  desabilitada;
- a operação registra ator, alvo, motivo, instante, resultado e correlação;
- uma conta com `PLATFORM_ADMIN` não pode ser desabilitada se isso deixar o
  ambiente sem outro administrador cotidiano habilitado;
- nenhuma exclusão de usuário ocorre.

## 4. Fora do escopo

- reabilitação ou exclusão de conta;
- desabilitação iniciada por aplicação consumidora;
- suspensão de `Membership`;
- console/UI administrativa;
- gestão completa de papéis administrativos;
- alteração autenticada de senha.

## 5. Falhas, observabilidade e rollback

A operação local durável usa os estados `PENDING`, `COMPLETED`, `REJECTED` e
`FAILED`. Falha transitória deixa evidência e permite repetir a mesma chave; uma
repetição após queda entre a mutação externa e o commit local converge porque
desabilitar e fazer logout são operações idempotentes.

Rollback de código é a reversão do PR antes de produção. A migration é apenas
aditiva. Reverter a desabilitação de uma conta é uma operação de negócio distinta
e não será executada automaticamente por rollback técnico.

## 6. Evidência de recuperação de contexto

Após a compactação, foram relidas as fontes normativas, a sequência de migração,
o contrato autônomo, as ADRs e as pendências; branch e última fatia também foram
inspecionadas. Segurança continua sendo a primeira prioridade, seguida de
simplicidade. O conflito sobre a fonte das atribuições administrativas foi levado
a brainstorming antes de qualquer entrega.

## 7. Decisão que desbloqueou a revisão

A implementação provou em Keycloak e PostgreSQL reais a desabilitação, revogação,
idempotência, autenticação administrativa recente e preservação do único
`PLATFORM_ADMIN`. A proteção precisou consultar realm roles no Keycloak, enquanto
a redação anterior de `architecture.md` atribuía esses papéis ao IdentityHub.

O brainstorming decidiu manter no Keycloak as atribuições autoritativas dos papéis
de plataforma durante o MVP. Não haverá réplica local ou reconciliação desse
estado. O IdentityHub permanece responsável pelos contratos administrativos,
políticas de acesso, allowlist de authorities e auditoria. A decisão está
formalizada no ADR-0015 e permite concluir os gates desta fatia.

## 8. Evidências

- o teste inicial do caso de uso ficou vermelho pela ausência dos contratos de
  desabilitação; testes HTTP, JDBC e do adapter avançaram em ciclos vermelhos e
  verdes proporcionais às respectivas bordas;
- testes unitários demonstram intenção persistida antes do efeito externo,
  idempotência, conflito semântico, retry de falha transitória e rejeição
  terminal do último administrador;
- testes HTTP demonstram `PLATFORM_ADMIN`, TOTP e `auth_time` recente, além da
  negação de auditor, MFA ausente, autenticação antiga, JSON desconhecido e chave
  de idempotência ausente;
- PostgreSQL 17 real demonstrou a migration V15 e a preservação de ator, alvo,
  motivo, correlação e estados da operação;
- Keycloak 26.7 real demonstrou desabilitação antes do logout, revogação de sessão
  e refresh token, replay seguro, rejeição de login posterior e preservação do
  único `PLATFORM_ADMIN` habilitado;
- a revisão de segurança encontrou que roles atribuídas por composição também
  concedem administração; um teste vermelho exigiu consulta ao endpoint oficial
  de realm roles efetivas antes de a correção ficar verde;
- o primeiro gate WSL expôs reutilização não determinística do mesmo TOTP entre
  dois logins da suíte; a reprodução conjunta confirmou a causa e o helper passou
  a aguardar nova janela sem tornar a política do Keycloak mais permissiva;
- `.\gradlew.bat clean build`: verde em 1m46s no Windows, incluindo `bootJar`,
  Checkstyle, JaCoCo, ArchUnit e regressões, com suites de container ignoradas
  pela ausência de Docker direto no host;
- `./gradlew --no-daemon clean build`: verde em 12m09s no Linux/WSL com 270
  testes e PostgreSQL e Keycloak reais;
- `git diff --check` ficou verde e nenhum artefato gerado ou configuração local
  foi selecionado para versionamento.

## 9. Segurança, operação e próximo passo

A chamada externa permanece dentro de uma transação protegida por advisory lock
para serializar mutações globais iniciadas pelo IdentityHub. Isso evita que duas
desabilitações concorrentes aprovem simultaneamente a remoção dos últimos
administradores. Indisponibilidade ou resposta inesperada do Keycloak falha de
forma fechada e deixa a operação durável como `FAILED` para retry com a mesma
chave.

A migration V15 é somente aditiva. Antes de produção, rollback técnico é o revert
do PR; reabilitar uma conta é uma operação de negócio distinta e não acontece
automaticamente. `PD-001`, `PD-002` e `PD-003` continuam adiáveis. A próxima
capacidade da sequência aprovada é aquisição e acesso por `Membership`.
