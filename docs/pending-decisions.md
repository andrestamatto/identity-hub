# Pendências de decisão

Este ledger registra decisões adiáveis encontradas durante a entrega autônoma.
Itens bloqueantes não entram aqui como forma de contornar uma parada obrigatória.

## Estados

- `OPEN`: decisão ainda necessária, sem bloquear a fatia atual;
- `RESOLVED`: decisão tomada e refletida nas fontes de verdade;
- `CANCELLED`: necessidade deixou de existir, com justificativa registrada.

## Modelo

```text
### PD-NNN — Título

- Status: OPEN | RESOLVED | CANCELLED
- Blocking: no
- Detected in: slice ou PR
- Context: por que a decisão existe
- Impact: capacidades ou operação afetadas
- Temporary choice: alternativa segura e reversível adotada
- Resolution condition: o que precisa acontecer para encerrar
- References: documentos, ADRs, issues ou PRs relacionados
```

## Pendências abertas

### PD-003 — Remoção física das estruturas obsoletas de onboarding

- Status: OPEN
- Blocking: no
- Detected in: SLICE-005C
- Context: as migrations V11, V12 e V13 podem ter sido aplicadas e não devem ser
  alteradas ou compensadas sem conhecer os bancos de destino.
- Impact: colunas e tabelas inativas permanecem no PostgreSQL, sem acesso pelo
  runtime e sem ampliar o contrato público.
- Temporary choice: preservar as migrations e deixar as estruturas sem uso.
- Resolution condition: inventariar os ambientes, confirmar que não há dados
  necessários e autorizar explicitamente uma migration compensatória.
- References: ADR-0014 e `assessments/slice-005c-standard-oidc-acquisition.md`.

### PD-001 — Provedor de e-mail de produção

- Status: OPEN
- Blocking: no
- Detected in: SLICE-003A
- Context: o MVP precisa de entrega SMTP em produção, mas nenhum provedor foi
  contratado ou aprovado.
- Impact: verificação de e-mail, recuperação de senha e notificações de segurança
  não podem ser implantadas em produção antes da decisão.
- Temporary choice: porta SMTP neutra e Mailpit 1.30.6 apenas em desenvolvimento
  e testes.
- Resolution condition: selecionar um provedor SMTP compatível, configurar
  credenciais por secret manager e validar entregabilidade no ambiente de destino.
- References: `identityhub-spec.md` IH-MVP-018, `architecture.md` seção 14.5 e
  `roadmap.md` seção 9.6.

### PD-002 — Topologia de proxy confiável para IP de origem

- Status: OPEN
- Blocking: no
- Detected in: SLICE-004D
- Context: o rate limiting público precisa reconhecer o endereço do cliente
  atrás do reverse proxy, mas a topologia final do Coolify e seus endereços
  confiáveis ainda não foram fixados.
- Impact: sem a configuração, todas as requisições vistas pelo mesmo proxy
  compartilham a quota de cadastro; aceitar cabeçalhos encaminhados sem confiança
  explícita permitiria falsificação da origem.
- Temporary choice: usar somente o endereço remoto fornecido pelo servidor HTTP,
  ignorar `X-Forwarded-For` e manter a borda desabilitada por padrão. A escolha
  falha de forma conservadora e é reversível.
- Resolution condition: documentar a rede de proxies do ambiente, aceitar o
  endereço encaminhado somente de proxies em allowlist e validar rate limiting e
  spoofing em staging antes de exposição pública.
- References: `security-model.md` seção 20 e
  `assessments/slice-004d-public-local-registration.md`.
