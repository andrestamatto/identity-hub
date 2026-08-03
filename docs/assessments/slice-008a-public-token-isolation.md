# SLICE-008A — Audience pública condicionada à Membership

> **Status:** complete
>
> **Data:** 2026-08-03
>
> **Branch:** `feat/slice-008a-public-token-isolation`

## 1. Resultado observável

Após uma `Membership` ficar `ACTIVE`, um novo Authorization Code com PKCE por
um cliente de navegador gerenciado pode produzir access token com as audiences
das APIs protegidas da mesma `ClientApplication`. Sem Membership ativa, o mesmo
login não produz token capaz de autorizar essas APIs. Nenhum token contém
audience, papel, grupo ou identificador interno de outra aplicação.

## 2. Requisitos e decisões relacionados

- `IH-MVP-012`, incremento inicial de token público;
- `IH-MVP-010`, login sem Membership não autoriza API de negócio;
- ADR-0004, ADR-0007, ADR-0010, ADR-0017 e ADR-0018;
- `security-model.md`, perfil de token e isolamento por aplicação.

## 3. Dentro do escopo

- role operacional privada que condiciona audience sem integrar o contrato;
- scope técnico determinístico por aplicação, limitado por role scope mapping;
- vínculo somente entre SPA/BFF e APIs protegidas da mesma aplicação;
- remoção dos client scopes herdados que expõem PII ou claims nativos;
- claim público `roles` vazio até a fatia de papéis de negócio;
- token humano com `iss`, `sub`, `aud`, `exp`, `iat`, `jti`, `azp`, `scope`,
  `roles` e `sid` quando fornecido pelo motor;
- provas reais com duas aplicações no mesmo realm.

## 4. Fora do escopo

- definição, atribuição e remoção de papéis de negócio;
- starter Resource Server do Integration Mode;
- refresh, logout e revogação;
- seleção de uma API individual dentro da mesma aplicação;
- mudança no contrato de login hospedado ou no realm por ambiente.

## 5. Invariantes

- token sem Membership ativa não contém audience de API de negócio;
- todas as audiences emitidas pertencem à aplicação do cliente de navegador;
- marker de grupo e role operacional nunca aparecem no contrato público;
- `realm_access`, `resource_access`, `groups`, e-mail e telefone não aparecem;
- configuração ou ownership inesperado falha fechado e não é sobrescrito;
- nenhum provider Java customizado ou acesso ao schema do Keycloak é usado.

## 6. Falhas e bordas

- ausência de API protegida mantém o login sem audience de negócio;
- API adicionada depois da ativação exige reconciliação explícita da Membership;
- browser client, API, scope ou role homônimo sem ownership causa falha
  permanente sanitizada;
- replay após efeito remoto é idempotente;
- indisponibilidade remota mantém a Membership `PENDING` durante reconciliação.

## 7. Testes planejados

- adapter: ownership, escopo por aplicação, replay e remoção de defaults;
- Keycloak 26.7 real: token novo com Membership ativa possui somente audiences
  da aplicação e `roles=[]`;
- Keycloak 26.7 real: aplicação sem Membership não recebe audience de negócio;
- token não contém PII, claims nativos, marker ou role operacional;
- duas aplicações comprovam isolamento cruzado;
- build Windows e Linux/WSL completos.

## 8. Migration, observabilidade e rollback

Não há migration de banco. O resultado da configuração integra as métricas já
existentes da projeção de Membership, sem labels de aplicação ou usuário.
Antes de produção, rollback é o revert do PR; artefatos remotos permanecem
inertes sem vínculo ao cliente e não serão removidos automaticamente sem
inspeção e autorização.

## 9. Implementação e evidência

- `JdbcApplicationTokenClientResolver` seleciona somente API, SPA e BFF
  habilitados, pertencentes à mesma aplicação e já projetados no Keycloak;
- `KeycloakMembershipTokenProjector` cria ou reconcilia a role privada de cada
  API, o scope técnico da aplicação, seus role scope mappings e os scopes de
  navegador permitidos;
- o `client_id` remoto da API coincide com a audience pública estável, pois esse
  é o valor que o Audience Resolve suportado pelo Keycloak emite em `aud`;
- a role e o scope são configurados pela credencial de gestão de clientes; a
  associação de usuário/grupo e role usa a credencial separada de identidade;
- somente o scope nativo `basic` é preservado para que o motor emita `sub`;
  todos os demais defaults e optional scopes de navegador são removidos;
- o scope técnico é reconciliado para conter somente Audience Resolve e o mapper
  `roles=[]`; grupo, role operacional, `realm_access`, `resource_access`, PII e
  claims privados não entram no token;
- `KeycloakAdminTokenIntegrationTest` executado com Keycloak 26.7 e PostgreSQL
  reais comprovou ausência de audience antes da Membership, audience somente da
  aplicação ativa, isolamento entre duas aplicações, `roles=[]`, `sub`, `jti`,
  tempos válidos e ausência dos claims proibidos;
- o teste do adapter cobriu replay, ownership e reconciliação de mapper
  inesperado; a execução focada e Checkstyle ficaram verdes em Windows;
- a recuperação de contexto exigida por `autonomous-delivery.md` foi refeita em
  2026-08-03 antes da consolidação desta implementação.
