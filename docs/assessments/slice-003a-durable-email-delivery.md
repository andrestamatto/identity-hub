# SLICE-003A — Entrega durável de e-mail transacional

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-003a-durable-email-delivery`
>
> **Base:** `develop` em `a21c15d`

## 1. Objetivo

Entregar o primeiro incremento de `IH-MVP-018`: uma solicitação interna de
confirmação de alteração de senha é persistida, processada por worker e entregue
por SMTP sem depender da disponibilidade imediata do provedor.

O incremento prova a infraestrutura de entrega com conteúdo informativo que não
carrega token ou código utilizável. Verificação de e-mail e recuperação de senha
serão acrescentadas junto das capacidades de identidade que produzem suas provas.

## 2. Resultado observável

- `RequestPasswordChangedEmail` aceita uma entrega com UUID rastreável,
  `ClientApplication` de origem, destinatário e correlação;
- PostgreSQL persiste a intenção antes de qualquer chamada SMTP;
- um worker concorrente reserva e envia a entrega;
- sucesso produz estado `DELIVERED`;
- falha temporária agenda retry com backoff;
- falha permanente ou limite de tentativas produz `FAILED`;
- admin e auditor consultam diagnóstico sanitizado;
- somente admin pode reprocessar falha terminal;
- Mailpit real recebe o e-mail em teste de integração.

## 3. Invariantes

- finalidade inicial explícita: `PASSWORD_CHANGED`;
- replay idêntico não cria nova linha nem novo efeito após `DELIVERED`;
- mesmo UUID com conteúdo diferente é conflito;
- origem é resolvida pela aplicação cadastrada e armazenada como snapshot;
- ambiente e aplicação aparecem no conteúdo de forma determinística;
- destinatário e corpo não aparecem em logs, métricas ou diagnóstico HTTP;
- lease impede processamento concorrente da mesma entrega;
- SMTP é adapter; domínio e aplicação não dependem de Spring Mail;
- Mailpit é proibido como provedor de produção.

## 4. Falhas e bordas

- aplicação inexistente rejeita a solicitação antes da persistência;
- e-mail malformado é rejeitado no domínio;
- autenticação ou mensagem inválida é falha permanente;
- indisponibilidade e erro SMTP transitório são retentáveis;
- exceção inesperada no ciclo não registra dado pessoal;
- perda da confirmação SMTP pode repetir o e-mail informativo, mas não duplica
  efeito de segurança porque esta finalidade não contém prova utilizável.

## 5. Persistência e rollback

Flyway V8 cria uma outbox específica da capacidade `communication`. Ela contém
snapshot mínimo da origem, finalidade, destinatário, estado, tentativas, lease e
correlação. Não existe broker.

Antes de produção, rollback é revert do PR e recriação do ambiente descartável.
A migration é forward-only e não altera tabelas já aplicadas.

## 6. Observabilidade

O scheduler registra contadores por resultado sem labels de aplicação,
destinatário ou correlação. Erros de ciclo usam mensagem sanitizada. O diagnóstico
expõe identificadores, finalidade, estado, tentativas e código de falha, nunca o
endereço ou o conteúdo.

## 7. Fora de escopo

- cadastro ou alteração real de senha;
- verificação de e-mail e recuperação com prova temporária;
- HTML, branding e templates configuráveis;
- anexos, marketing, SMS ou WhatsApp;
- seleção e credenciais do provedor de produção;
- broker, DLQ externa ou entrega exatamente uma vez pelo SMTP;
- deploy na VPS.

## 8. Evidência

Os testes cobrem:

- validação de destinatário e correlação;
- criação, replay idempotente, conflito e corrida de inserção concorrente;
- sucesso, retry exponencial, falha permanente e limite de tentativas;
- round-trip, lease exclusivo, transições e reprocessamento no PostgreSQL 17;
- classificação sanitizada de falhas do adapter SMTP;
- percurso PostgreSQL/outbox → worker → SMTP → Mailpit 1.30.6 real;
- leitura sanitizada por auditor e reprocessamento exclusivo por admin com TOTP;
- regras ArchUnit do novo limite de capacidade;
- composição local com Mailpit fixado e restrito ao loopback.

Gates executados em 2026-07-31:

- Windows: `.\gradlew.bat clean build` — verde, incluindo `bootJar`, estilo,
  arquitetura e regressões; Testcontainers indisponíveis foram ignorados pelo
  mecanismo explícito da suíte;
- Linux/WSL: `./gradlew clean build` — verde com 160 testes, sem falhas, erros
  ou ignorados, usando PostgreSQL 17, Keycloak 26.7 e Mailpit 1.30.6 reais;
- confirmação incremental Linux/WSL: `./gradlew build` — verde, todos os gates
  atualizados;
- harness: `python3 -m unittest scripts/test_local_dev.py -v` — 3 testes verdes;
- Compose: `docker compose ... config --services` — banco do IdentityHub, banco
  do Keycloak, Keycloak e Mailpit resolvidos sem expor valores do ambiente.

## 9. Gate autônomo

A fatia satisfaz os critérios documentados e está autorizada para publicação
segundo `docs/autonomous-delivery.md`. A escolha do provedor de produção permanece
registrada como `PD-001`, sem bloquear este incremento local e reversível.
