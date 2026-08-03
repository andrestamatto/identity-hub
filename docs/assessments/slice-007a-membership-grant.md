# SLICE-007A — Intenção de concessão de Membership por aplicação

> **Status:** implementation and local delivery gates complete
>
> **Data:** 2026-08-02
>
> **Branch:** `feat/slice-007a-membership-grant`

## 1. Resultado observável

Um cliente de máquina explicitamente autorizado com audience
`identityhub-integration-api` e scope `membership:write` solicita a concessão de
acesso a uma referência opaca de usuário. O IdentityHub deriva a aplicação pelo
`azp` validado, persiste uma única `Membership` como `PENDING` e devolve uma
operação rastreável sem conceder acesso antes da projeção.

## 2. Rastreabilidade

- `identityhub-spec.md`: primeira parte de `IH-MVP-016` e restrição de
  `IH-MVP-010` sem `Membership` ativa;
- `architecture.md`: seções 8.3, 11, 13.2 e 14.2;
- `security-model.md`: seções 8, 9, 10.3, 19 e 22;
- `integration-mode.md`: seções 11.1 e 14;
- ADR-0002, ADR-0010, ADR-0014 e ADR-0016.

## 3. Dentro do escopo

- permissão explícita `membership:write` em machine client;
- projeção OAuth da audience e do scope no Keycloak;
- resource server separado para `/api/v1/**`;
- resolução interna de `azp` para um machine client habilitado e projetado;
- endpoint `POST /api/v1/memberships` sem application id;
- corpo estrito contendo somente `userAccountRef`;
- `Idempotency-Key` obrigatória e conflito para payload diferente;
- aggregate `Membership` único por usuário e aplicação;
- persistência PostgreSQL aditiva de membership e operação;
- resposta `202` com identificadores opacos e estado `PENDING`;
- correlação e evidência da solicitação aceita ou repetida.

## 4. Fora do escopo

- projeção da `Membership` no Keycloak;
- estado `ACTIVE`, audience ou roles em token humano;
- suspensão, remoção e papéis da aplicação;
- endpoint administrativo equivalente;
- validação de pagamento, plano ou assinatura;
- cliente tipado do Integration Mode.

## 5. Invariantes e falhas

- a aplicação alvo nunca é aceita do solicitante;
- token de uma aplicação não materializa membership em outra;
- machine client sem permissão explícita não prossegue;
- mesma chave e comando retornam a mesma operação;
- mesma chave com outro usuário é rejeitada;
- chaves diferentes para a mesma combinação usuário/aplicação não duplicam a
  `Membership`;
- `PENDING` não autoriza API de negócio;
- nenhum dado comercial ou credencial humana é recebido ou persistido.

## 6. Testes planejados

- domínio: criação e unicidade sem estado ativo prematuro;
- aplicação: idempotência, conflito e replay por membership existente;
- HTTP/security: issuer, audience, scope, `azp`, campos desconhecidos e ausência
  de application id;
- PostgreSQL real: constraints, concorrência e round-trip;
- Keycloak real: Client Credentials contém somente a audience e o scope
  configurados para o provisionador;
- isolamento real entre duas aplicações e dois machine clients.

## 7. Migration, observabilidade e rollback

As migrations serão somente aditivas. Antes de produção, rollback é o revert do
PR; registros `PENDING` não concedem acesso e podem permanecer inertes até uma
decisão operacional explícita. Métricas usam resultado e duração sem UUID, `sub`,
token ou idempotency key. Correlation ID acompanha a operação persistida.

A V16 cria `application_client_machine_permission` para as permissões atuais e
não altera nem reutiliza `application_client_machine_scope`, criada pela V11 para
o onboarding abandonado. Um teste PostgreSQL real comprova que um registro
histórico `onboarding:write` permanece inerte e não chega ao modelo atual.

## 8. Evidência de recuperação de contexto

Após a compactação, foram relidos os documentos normativos, ADRs, estratégia de
migração, contrato autônomo e pendências; branch, working tree e última slice
concluída também foram inspecionados antes de retomar o TDD. A implementação
permaneceu limitada à decisão aprovada no ADR-0016.

## 9. Evidências de implementação

- o teste inicial de machine scope ficou vermelho pela ausência do value object,
  persistência e contrato administrativo; depois ficou verde no domínio, HTTP e
  PostgreSQL 17 real;
- o projetor Keycloak ficou vermelho antes de criar o client scope, audience
  mapper e vínculo default; o teste verde também prova idempotência, remoção de
  scopes herdados e recusa de recurso homônimo não gerenciado;
- Keycloak 26.7 real emitiu Client Credentials com exatamente a audience
  `identityhub-integration-api`, scope `membership:write` e `azp` esperado;
- domínio e aplicação provam criação exclusivamente `PENDING`, replay da mesma
  chave e conflito semântico com comparação constante do fingerprint;
- PostgreSQL real provou migration V17, unicidade por aplicação/usuário,
  concorrência, múltiplas operações sobre a mesma membership e rollback integral
  de chave idempotente conflitante;
- testes HTTP provam `401` sem autenticação, `403` sem scope ou vínculo válido,
  `400` para campo de aplicação, `409` para conflito e `202` sem exposição do
  identificador da aplicação;
- o teste vertical real persistiu uma única membership e uma única operação após
  dois requests idênticos, e rejeitou payload adulterado sem novo registro;
- o mesmo cenário rejeitou com `401` um token administrativo de audience
  diferente e provou que dois machine clients materializam somente memberships
  pertencentes às respectivas aplicações;
- métricas registram apenas duração e outcome estável; correlation ID fica na
  operação, sem token, chave idempotente ou identificadores em tags.

## 10. Gates locais

- `gradlew.bat clean build`: verde no Windows em 1m43s;
- `./gradlew --no-daemon clean build`: verde no WSL em 12m25s, com Docker e
  Testcontainers habilitados;
- 290 testes executados, sem falha, erro ou teste ignorado;
- Checkstyle, JaCoCo, ArchUnit e `bootJar`: verdes;
- teste focado PostgreSQL real após a separação da V16: verde em 3m43s;
- teste vertical Keycloak 26.7 real: verde em 5m36s.

## 11. Risco residual e próximo incremento

Esta fatia não concede acesso: `PENDING` é fail-closed e não participa de tokens
humanos. A próxima subfatia de `IH-MVP-016` deve projetar a membership no motor,
ativá-la somente após sucesso e provar que audience e papéis humanos permanecem
isolados por aplicação. `PD-001`, `PD-002` e `PD-003` continuam adiáveis e não
bloqueiam esta entrega.
