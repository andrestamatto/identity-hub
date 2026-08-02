# ADR-0015: Keycloak como fonte das atribuições de papéis de plataforma no MVP

- **Status:** Accepted
- **Data:** 2026-08-02

## Contexto

O IdentityHub reconhece `PLATFORM_ADMIN` e `PLATFORM_AUDITOR` nos tokens do
cliente administrativo e precisa impedir que uma desabilitação deixe o ambiente
sem administrador cotidiano recuperável.

A arquitetura atribuía genericamente os papéis de plataforma ao IdentityHub, mas
o MVP ainda não possui lifecycle local para suas atribuições. Criar uma réplica
autoritativa apenas para essa invariável introduziria bootstrap, projeção,
reconciliação e risco de decisão incorreta em caso de drift. O Keycloak já mantém
as contas administrativas habilitadas e suas realm roles usadas na autenticação.

## Decisão

No MVP, o Keycloak é a fonte de verdade das atribuições de papéis de plataforma.

- `PLATFORM_ADMIN` e `PLATFORM_AUDITOR` são realm roles do ambiente;
- o IdentityHub aceita somente esses papéis documentados ao converter o token
  administrativo em authorities;
- tokens administrativos usam cliente e audience próprios, MFA obrigatório e os
  controles de autenticação recente definidos pelo IdentityHub;
- invariantes que dependam do conjunto atual de administradores consultam a Admin
  REST API suportada e falham de forma fechada quando não puderem verificá-lo;
- não existe réplica autoritativa nem reconciliação dessas atribuições no banco do
  IdentityHub;
- aplicações consumidoras não podem conceder, remover nem herdar papéis de
  plataforma;
- administração cotidiana continua pelos contratos do IdentityHub, sem exposição
  pública do console ou da Admin REST API do Keycloak;
- bootstrap e recuperação emergencial permanecem procedimentos internos e
  restritos.

A documentação do IdentityHub continua definindo a semântica e as políticas dos
papéis. A decisão trata da propriedade operacional das atribuições, não transfere
ao Keycloak os conceitos de produto, `Memberships` ou roles por aplicação.

## Consequências positivas

- elimina uma segunda fonte de verdade para privilégio administrativo;
- a proteção do último administrador usa o mesmo estado que autoriza o login;
- reduz código, migrations, projeção e reconciliação no MVP;
- preserva a fronteira pública e as políticas do IdentityHub.

## Consequências negativas

- esse estado administrativo depende do modelo suportado de realm roles do
  Keycloak;
- operações que verificam a invariável dependem da disponibilidade da Admin REST
  API e devem falhar de forma fechada;
- uma futura troca de motor exigirá migrar atribuições administrativas por um
  procedimento interno.

## Alternativas consideradas

### IdentityHub como autoridade imediata

Rejeitada no MVP porque exigiria implementar lifecycle, bootstrap e reconciliação
antes de existir outro caso de uso que justificasse essa complexidade. Drift entre
a atribuição local e a efetivamente usada no login poderia enfraquecer a proteção
do último administrador.

### Aceitar o papel apresentado pelo token sem consultar o estado atual

Rejeitada porque um token anterior não prova quantos outros administradores
habilitados permanecem no ambiente no instante da mutação.

### Usar diretamente o console do Keycloak para administração cotidiana

Rejeitada porque viola o encapsulamento do motor, amplia privilégio e não oferece
os contratos e a auditoria do IdentityHub.

## Validação

- testes negativos comprovam allowlist de authorities, audience, MFA e
  autenticação recente;
- teste com Keycloak real impede desabilitar o único `PLATFORM_ADMIN` habilitado;
- teste com Keycloak real permite a operação quando outro administrador
  habilitado permanece;
- indisponibilidade ou resposta inesperada da autoridade não concede a mutação;
- nenhum schema privado do Keycloak é acessado.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [Especificação do MVP](../identityhub-spec.md)
- [ADR-0001](0001-keycloak-as-internal-identity-engine.md)
- [ADR-0010](0010-data-ownership-and-runtime-projections.md)
