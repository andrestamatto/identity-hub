# ADR-0002: Identidade global e acesso por aplicação

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

Os primeiros SaaS não precisam compartilhar acesso, mas existe possibilidade real
de catálogo integrado, SSO percebido e aquisição de múltiplos produtos no futuro.

Criar uma identidade por SaaS duplicaria credenciais e dificultaria essa evolução.
Por outro lado, uma conta global não pode conceder automaticamente acesso a todos
os produtos.

## Decisão

Cada ambiente possui um `UserAccount` global por pessoa.

O acesso a uma `ClientApplication` existe somente por uma `Membership` explícita,
independente e ativa.

Regras:

- autenticar ou cadastrar não cria membership automaticamente;
- uma aplicação concede acesso somente à própria aplicação;
- a concessão ocorre depois da decisão comercial do SaaS;
- papéis são limitados à aplicação;
- desabilitação global da conta é diferente de suspensão de uma membership;
- `OnboardingIdentityProof` correlaciona aquisição sem autorizar API de negócio.

## Consequências positivas

- base para experiência integrada entre produtos;
- uma pessoa gerencia menos credenciais;
- isolamento preservado por application, audience, membership e role;
- aquisição continua sob responsabilidade do SaaS;
- B2B futuro pode evoluir sem duplicar identidade humana.

## Consequências negativas

- associação de contas exige controles contra account takeover;
- incidentes globais podem afetar múltiplas aplicações;
- privacidade exige minimização e finalidade por produto;
- lifecycle global e lifecycle de acesso precisam permanecer distintos.

## Alternativas consideradas

### Usuário independente por SaaS

Rejeitado por duplicar identidade e bloquear o catálogo integrado.

### Conta global concede acesso a todos os produtos

Rejeitado por violar isolamento e confundir identidade com direito comercial.

### Usuário solicita membership diretamente ao IdentityHub

Rejeitado no fluxo padrão. O SaaS decide aquisição, pagamento e plano; seu backend
solicita a materialização do acesso.

## Validação

- usuário autenticado sem membership não acessa a API;
- mesma identidade recebe memberships independentes;
- token de uma aplicação não contém role da outra;
- repetição do provisionamento não duplica membership;
- suspensão em uma aplicação não desabilita a identidade global.

## Documentos relacionados

- [Especificação do MVP](../identityhub-spec.md)
- [Arquitetura](../architecture.md)
- [Roadmap](../roadmap.md)
