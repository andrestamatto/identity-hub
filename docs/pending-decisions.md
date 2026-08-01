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
