# ADR-0003: Modelo de Token e Sessão

- Status: accepted
- Data: 2026-05-14

## Contexto
A autenticação precisa ser segura, performática e compatível com diferentes arquiteturas.
Também é necessário suportar logout seguro e revogação.

## Decisão
- `AccessToken` JWT de curta duração (stateless para autorização de requests).
- `RefreshToken` com ciclo de vida e revogação explícita.
- Logout invalida o refresh token/sessão associada.

No MVP embedded, o estado de revogação pode ser delegado ao consumidor por adaptador.
No modo serviço, será obrigatório storage mínimo para revogação e auditoria.

## Consequências
- Ganho: bom equilíbrio entre desempenho e controle de sessão.
- Ganho: caminho claro para logout seguro e rotação de token.
- Custo: necessidade de políticas de expiração/rotação bem definidas por configuração.
- Risco: implementação inconsistente de revogação por consumidores sem testes de contrato.

