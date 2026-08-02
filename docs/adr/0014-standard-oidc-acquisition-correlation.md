# ADR-0014: Correlação de aquisição com OpenID Connect padrão

- **Status:** Accepted
- **Data:** 2026-08-02
- **Substitui:** [ADR-0013](0013-backend-initiated-onboarding-session.md)

## Contexto

O backend de um SaaS precisa associar uma pessoa autenticada à sua própria
aquisição antes de provisionar uma `Membership`. O desenho anterior criou
`OnboardingSession`, `OnboardingIdentityProof`, endpoint, scope, persistência e
validações próprias para essa correlação.

Esses artefatos duplicam garantias já oferecidas por OpenID Connect Authorization
Code com PKCE e aumentam a superfície de ataque, o contrato público e o custo de
operação antes de existir uma necessidade comprovada.

## Decisão

Aquisições usarão o fluxo OpenID Connect Authorization Code com PKCE. Um backend
ou BFF do SaaS:

1. mantém a referência de compra em sua sessão server-side;
2. inicia a autorização com `state`, `nonce`, PKCE S256 e redirect URI registrada;
3. recebe o authorization code exclusivamente em seu callback;
4. troca o código e valida issuer, audience, assinatura, tempo, `state`, `nonce` e
   PKCE antes de aceitar o `sub` opaco;
5. após sua decisão comercial, provisiona a `Membership` por Client Credentials,
   com idempotência.

O navegador não informa `sub`, aplicação ou aquisição como autoridade. O
IdentityHub não recebe pagamento, plano ou assinatura. Autenticar uma pessoa sem
`Membership` não autoriza APIs de negócio nem concede acesso automaticamente.

Não existirão no MVP `OnboardingSession`, `OnboardingIdentityProof`, handoff code,
segundo PKCE ou endpoint proprietário equivalente.

## Consequências positivas

- reduz código sensível, estados temporários e endpoints expostos;
- usa protocolo consolidado e bibliotecas maduras;
- mantém credenciais humanas e tokens fora do navegador quando houver BFF;
- preserva a separação entre autenticação e decisão comercial.

## Consequências negativas

- a aquisição segura exige backend ou BFF consumidor;
- uma SPA sem backend não pode provisionar acesso diretamente;
- o SaaS precisa manter a correlação comercial em sessão server-side.

## Compatibilidade de dados

As migrations Flyway V11, V12 e V13 permanecem imutáveis para preservar o
histórico de bancos que já as aplicaram. As estruturas de onboarding criadas por
elas ficam inativas e não são acessadas pelo runtime. Sua remoção física exige
decisão operacional separada e inspeção dos ambientes.

## Validação

- o endpoint proprietário de onboarding não existe;
- clientes de máquina não expõem `onboarding:write`;
- o build e os testes de integração permanecem verdes com as migrations
  históricas presentes;
- documentos públicos descrevem apenas OIDC e a API de `Membership`.

## Documentos relacionados

- [Especificação do MVP](../identityhub-spec.md)
- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [Integration Mode](../integration-mode.md)
