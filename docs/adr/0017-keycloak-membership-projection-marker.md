# ADR-0017: Marcador operacional de Membership no Keycloak

- **Status:** Accepted
- **Data:** 2026-08-03

## Contexto

Uma `Membership` pertence ao IdentityHub, mas o motor precisa de um estado
operacional confirmado antes que tokens futuros possam refletir o acesso. Essa
projeção não pode expor memberships de outras aplicações, antecipar o contrato
público de claims nem transformar conceitos internos do Keycloak no modelo do
produto.

Atribuir desde já um client role ao usuário faria o scope padrão `roles` do
Keycloak poder incluir representações privadas de outras aplicações em tokens
anteriores à configuração isolada de `IH-MVP-012`. Usar um atributo multivalorado
do usuário exigiria atualização read-modify-write e criaria risco de perda em
concessões concorrentes.

## Decisão

No MVP, a projeção operacional inicial de uma `Membership` ativa é o
pertencimento do usuário a um grupo técnico privado e determinístico do Keycloak,
um por `ClientApplication`.

- o identificador e o nome remoto são detalhes do adapter;
- o grupo possui marcas de ownership do IdentityHub;
- recurso homônimo sem ownership causa falha permanente e não é sobrescrito;
- o grupo não é incluído em access token, ID token, UserInfo ou introspecção;
- não é criado group mapper público;
- a associação do usuário ao grupo usa somente a Admin REST API oficial e é
  idempotente;
- a `Membership` local muda de `PENDING` para `ACTIVE` somente depois da
  confirmação remota;
- falha ou indisponibilidade mantém o acesso fechado e reconciliável.

O grupo é exclusivamente infraestrutura. Ele não representa organização, equipe,
tenant B2B ou grupo administrável do produto. A projeção de roles e o contrato
estável `roles` permanecem para as fatias próprias de papéis e tokens.

## Consequências positivas

- nenhuma role cross-application é introduzida em tokens atuais;
- associação e repetição usam operações atômicas suportadas pelo Keycloak;
- não é necessário atributo compartilhado sujeito a lost update;
- não surge nova credencial técnica nem contrato consumidor;
- futura troca do motor permanece contida no adapter.

## Consequências negativas

- o realm conterá um grupo técnico por aplicação;
- o adapter precisa distinguir ownership e drift;
- o marker sozinho não define o shape do token público;
- uma futura remoção física precisa retirar o usuário do grupo de forma
  reconciliável.

## Alternativas consideradas

### Client role privado por aplicação

Adiado até que os client scopes de tokens humanos estejam isolados. Aplicá-lo
agora poderia expor roles privadas de outras aplicações em claims nativos.

### Atributo de usuário por aplicação

Rejeitado para o marker inicial porque a API de atualização de atributos exige
preservar o mapa existente e aumenta o risco de lost update concorrente.

### Grupo como conceito público de organização

Rejeitado. Organizações e equipes são capacidades futuras e não são inferidas da
estrutura interna do motor.

## Validação

- duas aplicações produzem grupos técnicos diferentes;
- o usuário associado a uma aplicação não é associado à outra;
- replay não duplica grupo nem associação;
- grupo homônimo sem ownership não é alterado;
- usuário inexistente não ativa a `Membership`;
- falha após efeito remoto e antes do commit local converge em retry;
- token humano atual não recebe claim de grupo nem role nova por esta projeção.

## Documentos relacionados

- [ADR-0002](0002-global-identity-and-application-membership.md)
- [ADR-0004](0004-single-realm-per-environment.md)
- [ADR-0007](0007-public-access-token-claims.md)
- [ADR-0010](0010-data-ownership-and-runtime-projections.md)
- [ADR-0016](0016-membership-provisioning-client-authorization.md)
