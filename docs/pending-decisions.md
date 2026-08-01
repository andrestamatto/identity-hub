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

Nenhuma.
