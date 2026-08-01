# Estratégia de migração — IdentityHub

> **Status:** approved
>
> **Versão do documento:** 1.1
>
> **Última atualização:** 2026-07-31

## 1. Finalidade

Este documento define como substituir a implementação abandonada do IdentityHub
pela arquitetura aprovada, preservando rastreabilidade, segurança e ciclos curtos
de validação verificável.

A estratégia transforma a baseline técnica e a especificação do MVP em:

- pré-condições verificáveis;
- decisões sobre código e dados legados;
- incrementos preparatórios pequenos;
- fatias funcionais verticais;
- gates de qualidade e segurança;
- regras de commit, revisão, rollback e documentação.

Cada incremento exige escopo explícito, branch própria e um gate final. O gate
pode ser uma aprovação humana ou o mandato autônomo controlado definido em
`autonomous-delivery.md`.

## 2. Entradas normativas

Em caso de dúvida, a implementação deve consultar, nesta ordem:

1. `identityhub-spec.md`, para comportamento público e critérios de aceitação;
2. `security-model.md`, para controles e gates de segurança;
3. ADRs aceitos, para decisões arquiteturais;
4. `architecture.md`, para limites, propriedade e fluxos;
5. `integration-mode.md`, para o starter e o console local;
6. `roadmap.md`, para prioridade e horizonte;
7. `product-vision.md`, para propósito e limites do produto;
8. `assessments/pre-refactor-baseline.md`, como fotografia do legado.

Quando dois documentos normativos divergirem, o código não deve escolher
silenciosamente. A divergência deve ser corrigida ou resultar em novo ADR antes da
fatia afetada.

## 3. Resultado pretendido

A migração estará concluída quando:

- nenhum runtime depender da implementação abandonada;
- o plano de controle for um modular monolith por capacidade;
- Keycloak for o motor interno encapsulado;
- IdentityHub e Keycloak possuírem dados e ciclos de migração separados;
- aplicações consumidoras dependerem somente de contratos públicos;
- todos os cenários `IH-MVP-*` comprometidos estiverem demonstrados;
- o primeiro SaaS operar sem acesso direto ao Keycloak;
- build, testes, análise, segurança e operação satisfizerem os gates aprovados.

## 4. Não objetivos

Esta estratégia não pretende:

- migrar o design antigo de `User`;
- preservar endpoints da especificação abandonada;
- oferecer compatibilidade com dados ou APIs nunca usados em produção;
- introduzir microsserviços;
- introduzir RabbitMQ, Kafka ou outro broker no MVP;
- implementar SMS ou WhatsApp antes de necessidade real;
- criar todos os módulos, interfaces ou adapters antecipadamente;
- manter duas versões implantáveis do IdentityHub;
- entregar todas as capacidades numa única branch;
- definir agora versões sem executar os spikes oficiais;
- transformar o plano de migração em cronograma por datas.

## 5. Decisões de migração propostas

### 5.1 Substituição controlada, não strangler

A implementação será reconstruída no mesmo repositório, com remoção controlada do
source set antigo antes da primeira fatia funcional.

Não haverá coexistência em runtime entre:

- `User` antigo e identidade orquestrada;
- endpoints antigos e experiência hospedada;
- schema antigo e modelo novo;
- eventos Spring antigos e outbox;
- hashing próprio e credenciais no Keycloak.

Justificativas:

- a baseline não possui consumidor em produção;
- o produto antigo foi formalmente abandonado;
- a tag `v0.3.0` e o histórico preservam o código;
- os modelos de propriedade são incompatíveis;
- coexistência exigiria adapters e migração sem valor para usuário real.

Essa escolha não autoriza uma entrega big bang. A substituição estrutural será um
incremento preparatório isolado; o comportamento novo continuará entrando por
fatias verticais.

### 5.2 Mesmo repositório

O repositório continua único.

Gradle evoluirá incrementalmente para suportar as três unidades de distribuição:

```text
identityhub-service
identityhub-spring-boot-starter
identityhub-keycloak-theme
```

Regras:

- `identityhub-service` nasce no reset da fundação;
- starter e tema somente são adicionados quando suas fatias começarem;
- módulos de negócio permanecem packages dentro do service;
- não criar subprojeto para cada bounded context;
- contratos ou test support separados exigem consumidor e duplicação comprovados.

### 5.3 Dados legados

A estratégia assume que a baseline não contém identidades ou dados de produção que
precisem ser migrados.

Consequências:

- hashes e credenciais antigas não serão importados;
- a migração Flyway antiga não será renumerada nem reaplicada;
- o novo plano de controle começa em schema lógico vazio;
- Keycloak usa banco ou schema e usuário próprios;
- dados locais de teste são recriados;
- nenhum processo apaga automaticamente o schema antigo.

Antes de qualquer operação destrutiva em banco, deve existir:

1. inspeção explícita do ambiente alvo;
2. confirmação de que não há dado necessário;
3. backup ou snapshot quando aplicável;
4. alvo absoluto e inequivocamente identificado;
5. aprovação humana específica.

Se for encontrada identidade real, a migração pausa e recebe estratégia própria.
Senha não será exportada ou transformada por conveniência.

### 5.4 Nova história Flyway

O novo schema do plano de controle começa com sua própria `V1`.

Regras:

- Flyway nunca altera o schema do Keycloak;
- migrations aplicadas são imutáveis;
- correções usam migration seguinte;
- releases preservam compatibilidade durante rollback da aplicação;
- mudanças destrutivas usam expansão, transição e contração;
- migration de produção é etapa explícita e observável.

### 5.5 Nome do módulo de comunicação

Existe divergência documental:

- `architecture.md` usa `communication`;
- ADR-0005 usa `notification`.

A estratégia propõe `communication` como linguagem canônica porque a capacidade
abrange entrega de e-mail, prova de contato, tentativas e políticas diferentes, sem
pretender uma abstração universal de canais.

Antes do reset estrutural:

- a decisão deve estar registrada em ADR;
- ADR-0005 deve permanecer vigente, com somente o nome antigo parcialmente
  superseded;
- documentação e packages devem usar o mesmo nome.

### 5.6 Sem compatibilidade pública com a baseline

Os contratos antigos `/users/register` e `/users/confirm` não serão versionados nem
mantidos. Eles nunca foram contratos de produção da redefinição atual.

Compatibilidade passa a ser obrigatória a partir do primeiro contrato público
aprovado da nova implementação.

## 6. Modelo de progressão

```text
Documentação aprovada
        │
        ▼
MIG-001 — restaurar confiança no gate de testes
        │
        ▼
MIG-002 — fixar toolchain por spike e ADR
        │
        ▼
MIG-003 — reset controlado e fundação executável
        │
        ▼
Fatias verticais IH-MVP
        │
        ▼
Checkpoint com primeiro SaaS
        │
        ▼
Hardening, E2E, ZAP e pentest
        │
        ▼
Release do MVP
```

Nenhuma fase autoriza a seguinte sem concluir seu gate. A revisão aplicável segue
o modo supervisionado ou autônomo controlado da seção 11.3.

## 7. Pré-condições de implementação

### 7.1 Gate documental

- estratégia de migração aprovada;
- nome `communication` reconciliado por ADR;
- decisões pendentes classificadas como ADR, spike ou roadmap;
- primeiro incremento definido por resultado verificável;
- nenhum documento vigente orienta manter a arquitetura abandonada.

### 7.2 Gate do harness

- comando canônico de build documentado;
- todos os testes esperados são descobertos;
- teste propositalmente falho torna o pipeline vermelho;
- análise estática bloqueante;
- teste de arquitetura bloqueante;
- scan de secrets no pipeline;
- relatório de dependências e vulnerabilidades;
- nenhuma credencial real nos testes;
- `AGENTS.md` com convenções específicas do repositório;
- CI executa os mesmos comandos disponíveis localmente.

Não será adotado um conjunto sobreposto de linters. O spike de toolchain seleciona
uma ferramenta principal de análise Java, além de:

- JUnit 5 para comportamento;
- ArchUnit para limites;
- JaCoCo para visibilidade de cobertura;
- Testcontainers para integrações reais;
- scanner de secrets e composição de software no CI.

Cobertura não substitui cenários. O limiar inicial será definido depois da
fundação, sem exigir percentual arbitrário sobre configuração e adapters triviais.

### 7.3 Gate de compatibilidade

Spikes devem fixar combinações suportadas de:

- Java;
- Gradle;
- Spring Boot e Spring Security;
- Keycloak;
- PostgreSQL;
- Testcontainers;
- bibliotecas de integração necessárias.

Cada spike registra:

- fontes oficiais consultadas;
- versões;
- cenário;
- resultado observado;
- limitações;
- consequência arquitetural.

O resultado origina ADR antes do reset da fundação.

## 8. Incrementos preparatórios

### 8.1 MIG-001 — Restaurar confiança no gate de testes

#### Objetivo

Fazer o comando padrão descobrir intencionalmente todo teste elegível.

#### Escopo

- reproduzir a aparente omissão das oito classes;
- determinar se a causa está na descoberta ou na medição;
- corrigir somente o harness ou a documentação necessários;
- adicionar verificação que torne nova omissão visível;
- documentar o comando canônico.

#### Verificação

- os 91 testes da baseline são executados ou cada exclusão é justificada;
- uma das classes antes considerada omitida aparece no relatório padrão;
- introduzir falha controlada em teste de prova torna o comando vermelho;
- reverter a falha devolve o build verde;
- nenhuma lógica de produção é refatorada.

#### Fora de escopo

- atualizar todas as dependências;
- remover legado;
- adicionar Keycloak;
- criar módulos novos.

#### Rollback

Reverter a alteração de harness remove o gate explícito sem afetar dados.

### 8.2 MIG-002 — Toolchain e ADR de compatibilidade

#### Objetivo

Escolher uma combinação suportada para a primeira release e comprovar os riscos que
afetam a arquitetura.

#### Spikes mínimos

- Spring Boot, Spring Security, Gradle e Java;
- Keycloak em produção e por Testcontainers;
- PostgreSQL e Flyway;
- Admin API suportada do Keycloak;
- claims públicos por protocol mapper suportado;
- TOTP e autenticação administrativa;
- comportamento necessário de refresh token;
- theme e snapshot de branding, quando sua fatia se aproximar.

Branding e refresh podem permanecer como spikes posteriores, mas precisam concluir
antes de suas respectivas fatias.

#### Verificação

- fontes oficiais registradas;
- aplicação mínima e containers iniciam;
- integração básica ocorre sem acesso a schema privado;
- ADR contém versões e matriz de compatibilidade;
- dependência explícita não rebaixa silenciosamente versão gerenciada.

#### Fora de escopo

- implementar domínio;
- produzir imagem final;
- configurar produção;
- antecipar provider Java de tema.

### 8.3 MIG-003 — Reset controlado e fundação executável

#### Objetivo

Remover a implementação abandonada e estabelecer o menor esqueleto capaz de
receber a primeira fatia vertical com segurança.

#### Remover

- domínio `User` antigo;
- endpoints antigos;
- hashing e pepper próprios;
- persistence e migration `users`;
- eventos e notificações antigas;
- SMS, WhatsApp e Twilio;
- profiles e configurações abandonados;
- testes exclusivos do comportamento antigo;
- dependências sem uso na nova fundação.

#### Preservar ou recriar

- Gradle Wrapper;
- documentação;
- changelog;
- archive `v0.3.0`;
- composition root mínima;
- Java toolchain fixada;
- build e CI;
- testes sentinela;
- estrutura inicial de `identityhub-service`;
- logging seguro;
- configuração tipada;
- `Clock` injetável;
- `AGENTS.md`;
- regras ArchUnit iniciais.

#### Estrutura inicial

```text
identityhub-service/
└── src/
    ├── main/java/br/dev/andrestamatto/identityhub/
    │   └── bootstrap/
    └── test/java/br/dev/andrestamatto/identityhub/
        └── architecture/
```

Packages de capacidade somente aparecem na fatia que os utiliza.

#### Verificação

- build limpo e reproduzível;
- aplicação mínima inicia;
- health básico distingue liveness e readiness quando dependências existirem;
- nenhum endpoint funcional legado responde;
- busca não encontra senha, Twilio ou contratos abandonados;
- regras de package impedem ciclos e imports proibidos;
- imagem ou artefato mínimo é gerado;
- documentação aponta para a tag quando alguém procurar o código antigo.

#### Rollback

Como não existe consumidor, rollback é o revert do PR. A tag e o commit anterior
preservam o estado removido.

## 9. Fundação funcional

### 9.1 SLICE-000 — Ambiente e fronteira administrativa

#### Requisitos relacionados

- `IH-MVP-022`;
- subconjunto inicial verificável de `IH-MVP-023`;
- requisitos transversais de segurança e observabilidade.

#### Resultado

Um ambiente de desenvolvimento descartável inicia plano de controle, Keycloak e
PostgreSQL separados. Uma API administrativa privada aceita somente token com
issuer, audience, papel e nível de autenticação esperados.

#### Deve incluir

- Keycloak encapsulado;
- realm do ambiente;
- cliente administrativo distinto;
- `PLATFORM_ADMIN` e `PLATFORM_AUDITOR`;
- TOTP exigido para conta administrativa cotidiana;
- resource server no plano de controle;
- negação `401` e `403`;
- rede e rotas administrativas não públicas;
- correlation ID;
- health e readiness;
- teste real com Keycloak e PostgreSQL descartáveis;
- auditoria mínima de acesso administrativo permitido e negado.

#### Não inclui

- console próprio completo;
- `ClientApplication`;
- membership;
- usuário final;
- login social;
- produção;
- `BREAK_GLASS_ADMIN` operacional completo.

#### Gate

- admin sem TOTP ou confiança exigida não prossegue;
- auditor não executa mutação de prova;
- token do audience errado é rejeitado;
- credencial de outro ambiente é rejeitada;
- console administrativo do Keycloak não é publicado.

### 9.2 SLICE-001 — Cadastrar `ClientApplication`

#### Requisito

- `IH-MVP-001`.

#### Resultado

Um `PLATFORM_ADMIN` cadastra uma aplicação lógica isolada e um auditor pode
consultá-la sem mutar.

#### Caminho vertical

- aggregate `ClientApplication`;
- invariantes de identificador, nome e estado;
- caso de uso;
- persistência PostgreSQL e migration Flyway;
- API administrativa;
- autorização;
- idempotência apropriada;
- auditoria;
- métricas e correlação;
- testes unitários, integração e contrato.

#### Limite

Não cria `ApplicationClient` nem client no Keycloak. A aplicação fica disponível
para a próxima configuração, conforme `IH-MVP-001`.

#### Gate

- identificador duplicado não altera estado;
- operador sem privilégio é negado e auditado;
- nenhuma aplicação enxerga dados de outra;
- round-trip usa PostgreSQL real;
- domínio não importa Spring, JPA ou Keycloak.

### 9.3 SLICE-002 — Configurar e projetar `ApplicationClient`

#### Requisito

- `IH-MVP-002`.

#### Resultado

Um admin configura um tipo de client por vez e o plano de controle projeta a
intenção no Keycloak de forma durável e reconciliável.

#### Ordem interna

1. API protegida;
2. SPA;
3. BFF ou web confidencial;
4. cliente de máquina.

Cada tipo pode ser um PR separado se seus invariantes não couberem numa revisão
pequena.

#### Entrada da outbox

Esta é a primeira fatia que exige efeito externo após commit. Portanto, introduz:

- operação persistida;
- outbox atômica;
- worker;
- retry limitado com backoff;
- idempotência;
- estados `PENDING`, `APPLIED` e `FAILED`;
- reconciliação;
- diagnóstico administrativo.

Não introduz broker.

#### Gate

- crash entre commit e projeção não perde trabalho;
- repetição não duplica client;
- drift é detectado;
- falha permanente fica visível;
- segredo aparece somente uma vez quando aplicável;
- wildcard e redirect inválidos são rejeitados;
- consumers não recebem modelo privado do Keycloak.

## 10. Sequência funcional do MVP

A ordem abaixo expressa dependência. Cada item ainda será decomposto em fatias
revisáveis.

| Ordem | Capacidade | Requisitos principais | Resultado observável |
|---:|---|---|---|
| 1 | Ambiente e administração inicial | `IH-MVP-022`, `IH-MVP-023` | Fronteira administrativa forte |
| 2 | Aplicação lógica | `IH-MVP-001` | SaaS cadastrado e isolado |
| 3 | Clients e projeção | `IH-MVP-002` | Canais seguros no motor |
| 4 | E-mail durável | `IH-MVP-018` | Entrega essencial rastreável |
| 5 | Identidade local | `IH-MVP-003`, `IH-MVP-004`, `IH-MVP-006` | Cadastro, verificação e login por e-mail |
| 6 | Recuperação e lifecycle | `IH-MVP-015`, `IH-MVP-017` | Conta recuperável e desabilitável |
| 7 | Aquisição e acesso | `IH-MVP-010`, `IH-MVP-016` | Membership explícita |
| 8 | Token público | `IH-MVP-012` | Claims isolados por aplicação |
| 9 | Starter runtime | `IH-MVP-020` | Primeira API Java protegida |
| 10 | Sessão | `IH-MVP-013`, `IH-MVP-014` | Refresh, logout e revogação |
| 11 | Máquina | `IH-MVP-011` | Client Credentials |
| 12 | Telefone de contato | `IH-MVP-005` | Política sem login por telefone |
| 13 | Login social | `IH-MVP-007` | Google, GitHub e Facebook |
| 14 | Experiência e branding | `IH-MVP-008`, `IH-MVP-009` | Login hospedado personalizado |
| 15 | Console local | `IH-MVP-021` | Validate, diff e apply |
| Transversal | Auditoria e administração completa | `IH-MVP-019`, `IH-MVP-023` | Operação rastreável e segura |

### 10.1 Checkpoint do primeiro SaaS

Depois dos itens 1 a 9, o primeiro SaaS pode realizar integração em ambiente de
desenvolvimento para produzir feedback real.

Esse checkpoint:

- não é release do MVP;
- não dispensa sessões, logout, social, branding, console e hardening;
- não autoriza tráfego de produção;
- serve para validar contratos antes de ampliar escopo.

### 10.2 Ordem adaptável

Uma fatia pode ser antecipada quando:

- desbloqueia o primeiro consumidor;
- reduz risco crítico;
- é pré-condição de segurança;
- demonstra uma hipótese arquitetural incerta.

Mudança de ordem deve atualizar este documento ou o roadmap quando material. Não se
antecipa capacidade apenas porque sua implementação parece interessante.

## 11. Protocolo de cada fatia

### 11.1 Contrato antes do código

Antes de alterar código, a interação deve apresentar:

```text
Identificador:
Resultado observável:
Requisitos e critérios relacionados:
Dentro do escopo:
Fora do escopo:
Invariantes:
Falhas e bordas:
Testes planejados:
Migração de dados/schema:
Observabilidade:
Rollback:
```

O contrato pode ficar na descrição do PR e na conversa. Um documento adicional é
criado somente quando houver decisão durável não coberta pelas especificações.

### 11.2 TDD

Para cada comportamento:

1. escrever teste que falha pela razão esperada;
2. demonstrar o estado vermelho;
3. implementar o mínimo;
4. demonstrar o estado verde;
5. refatorar sem alterar comportamento;
6. executar o gate completo;
7. apresentar ou registrar diff e evidências para o gate de entrega aplicável.

Testes de infraestrutura podem começar por contrato ou integração quando um unit
test não oferece confiança relevante.

### 11.3 Modos de revisão e entrega

No modo supervisionado:

- nenhuma branch de implementação é enviada antes da validação final solicitada;
- alterações permanecem locais durante a revisão, salvo autorização diferente;
- objeções alteram código, teste e documento antes do commit;
- aprovação autoriza atualizar documentação, organizar commits e publicar branch.

No modo autônomo controlado:

- o mandato do mantenedor autoriza commits, push, criação e merge de PRs;
- cada fatia continua em branch e PR próprios;
- o agente registra critérios, evidências, riscos e rollback antes do merge;
- todos os checks obrigatórios devem estar verdes;
- falha de CI é investigada e corrigida na mesma branch;
- pendência adiável é registrada em `pending-decisions.md`;
- condição bloqueante de `autonomous-delivery.md` interrompe o ciclo e exige o
  mantenedor.

### 11.4 Commits

- Conventional Commits;
- um contexto lógico por commit;
- teste e implementação do mesmo ciclo podem permanecer juntos;
- refatoração sem comportamento fica separada quando material;
- migration acompanha o comportamento que a exige;
- documentação pequena acompanha a fatia;
- nenhuma credencial, log, artefato de build ou arquivo de IDE entra no commit.

Padrões indicativos:

```text
fix(test): restore complete JUnit discovery
build(platform): establish supported toolchain
refactor(architecture): replace abandoned foundation
feat(client-application): register consumer application
feat(application-client): project protected API client
```

## 12. Estratégia de testes

### 12.1 Pirâmide

- muitos testes unitários para invariantes e casos de uso;
- testes de integração para PostgreSQL, Flyway, Keycloak, outbox e adapters;
- testes de contrato para APIs, claims e manifesto;
- poucos testes E2E para jornadas críticas;
- testes de segurança proporcionais ao risco.

### 12.2 Testcontainers

Testcontainers deve usar versões fixadas para:

- PostgreSQL;
- Keycloak;
- provedor SMTP descartável quando adequado.

Containers não substituem unit tests e não devem ser iniciados em toda classe.
Fixtures compartilhadas não podem compartilhar estado mutável entre testes.

### 12.3 Testes de arquitetura

ArchUnit ou equivalente deve impedir:

- domínio importando Spring, JPA ou adapter;
- aplicação importando adapter;
- acesso a internals de outro módulo;
- ciclos;
- Keycloak fora do adapter autorizado;
- acesso direto ao schema do motor;
- `bootstrap` contendo regra de negócio;
- dependência de consumer em claim privado.

### 12.4 Segurança

Conforme a fatia aplicável:

- `401` e `403`;
- issuer e audience incorretos;
- token expirado, alterado ou com algoritmo proibido;
- privilégio insuficiente;
- CSRF, CORS, cookie e headers;
- redirect, state, nonce e PKCE;
- replay e idempotência;
- isolamento entre aplicações;
- enumeração e brute force;
- TOTP e reautenticação administrativa.

### 12.5 Gate canônico

O comando exato será fixado em MIG-002. Conceitualmente, um PR precisa provar:

- compilação;
- unit tests;
- integration tests da capacidade;
- análise estática;
- arquitetura;
- cobertura visível;
- ausência de secrets;
- dependências avaliadas;
- migration validada;
- documentação consistente.

Suites demoradas podem ser separadas entre PR e staging, mas nenhum teste crítico
pode ficar opcional ou silenciosamente ignorado.

## 13. Estratégia de domínio

### 13.1 Package por capacidade

Cada capacidade segue, quando necessário:

```text
<capability>/
├── domain/
├── application/
└── adapter/
    ├── in/
    └── out/
```

Não criar diretório vazio nem camada sem classe real.

### 13.2 Aggregates iniciais

- `ClientApplication`, em `clientapplication`;
- `Membership`, em `access`;
- referências opacas de usuário em `identity`;
- operação durável e outbox no módulo proprietário da mudança;
- auditoria como capacidade própria, sem modificar aggregates alheios.

### 13.3 Regras

- aggregate protege invariantes;
- caso de uso orquestra;
- adapter traduz;
- controller não contém regra;
- transação pertence ao caso de uso;
- módulo não acessa repository interno de outro;
- evento representa fato;
- comando durável representa trabalho ainda não concluído;
- shared kernel nasce somente por duplicação inevitável.

## 14. Integração com Keycloak

### 14.1 Anti-corruption layer

Somente adapters autorizados conhecem:

- Admin API;
- representações de client e role;
- protocol mappers;
- eventos específicos;
- detalhes de versão.

Domínio e contratos públicos usam linguagem IdentityHub.

### 14.2 Configuração desejada e projeção

IdentityHub persiste intenção. Keycloak mantém estado operacional.

Toda projeção:

- usa chave estável;
- é idempotente;
- possui timeout;
- distingue falha transitória e permanente;
- registra correlação;
- pode ser reconciliada;
- não acessa banco do Keycloak;
- falha fechada antes de conceder acesso.

### 14.3 Bootstrap

Bootstrap inicial pode criar somente recursos indispensáveis à administração do
ambiente. Ele não transforma realm export manual em fonte de verdade do produto.

Secrets:

- vêm do ambiente ou secret manager;
- não entram em YAML versionado;
- são exclusivos por finalidade e ambiente;
- nunca aparecem em logs ou respostas comuns.

## 15. Outbox e consistência

Outbox entra na primeira fatia que atravessa a transação local:
`SLICE-002`.

Contrato mínimo:

- registro na mesma transação da intenção;
- claim concorrente seguro pelo worker;
- payload versionado e mínimo;
- idempotency key;
- número de tentativas;
- próxima tentativa;
- resultado e erro sanitizado;
- correlação;
- estado terminal reprocessável;
- limpeza e retenção explícitas.

Testes simulam:

- queda depois do commit;
- dois workers;
- timeout depois de efeito remoto bem-sucedido;
- mensagem duplicada;
- falha permanente;
- reprocessamento;
- drift.

Broker somente será considerado pelos gates do roadmap.

## 16. Observabilidade desde a fundação

Cada fluxo novo define:

- evento ou log necessário;
- correlation ou trace ID;
- contador de sucesso e falha;
- duração;
- estado de dependência;
- dado proibido em labels e logs.

Regras:

- nenhum e-mail, token, código ou segredo como label;
- logs estruturados;
- liveness não depende de serviço externo;
- readiness reflete capacidade real de atender;
- erro externo possui timeout e contexto;
- auditoria não é substituída por log;
- health público é agregado e mínimo.

## 17. Rollout e rollback

### 17.1 Antes de produção

Enquanto não houver consumidor em produção:

- rollback de código é revert do PR ou versão anterior;
- ambientes são descartáveis;
- migrations novas ainda seguem disciplina de compatibilidade;
- não se preserva comportamento legado por precaução abstrata.

### 17.2 Com primeiro consumidor

- contratos públicos são versionados;
- migration é forward-only;
- release anterior deve funcionar durante janela de rollback;
- mudanças incompatíveis usam expansão e contração;
- projeções suportam retry e reconciliação;
- configuração arriscada entra primeiro em desenvolvimento;
- feature flag existe somente quando permitir desativação segura real.

### 17.3 Produção

Uma release não avança sem:

- backup ou recuperação aplicável;
- migration ensaiada;
- health verde;
- smoke test;
- observabilidade;
- rollback documentado;
- nenhum finding crítico ou alto aberto;
- spikes obrigatórios aprovados.

## 18. Documentação viva

Cada fatia deve avaliar atualização de:

- `identityhub-spec.md`, somente se comportamento aprovado mudar;
- `architecture.md`, quando limite ou fluxo mudar;
- `security-model.md`, quando fronteira ou ameaça mudar;
- `integration-mode.md`, para contrato consumidor;
- ADR, para decisão difícil de reverter;
- `roadmap.md`, para ordem ou gate material;
- `CHANGELOG.md`;
- `AGENTS.md`, quando surgir regra recorrente;
- guia operacional, quando houver ação de deploy ou recuperação.

O assessment de baseline não é reescrito. Resultados futuros são comparados a ele.

## 19. Definition of Done por fatia

Uma fatia está pronta para seu gate final quando:

- resultado observável foi demonstrado;
- critérios relacionados estão rastreados;
- fora de escopo não foi implementado;
- testes falharam antes e passam depois;
- integrações reais necessárias foram exercitadas;
- segurança negativa foi testada;
- logs, métricas e correlação foram avaliados;
- schema e rollback foram avaliados;
- arquitetura não possui violação;
- gate local está verde;
- CI está verde, quando a branch for publicada;
- diff não contém secret ou artefato;
- documentação está coerente;
- riscos residuais estão explícitos.

Uma aprovação visual, mandato autônomo ou “parece funcionar” não substitui esses
itens.

## 20. Riscos e controles

| Risco | Controle |
|---|---|
| Reset grande demais | Incremento MIG-003 isolado e sem feature |
| Corrigir legado sem valor | MIG-001 limita mudança ao harness |
| Acoplamento ao Keycloak | ACL, testes de arquitetura e contratos |
| Infraestrutura horizontal | Entrada somente pela fatia que a exige |
| Outbox abstrata demais | Primeira operação concreta em SLICE-002 |
| Administração fraca temporária | SLICE-000 precede APIs de produto |
| Testes verdes incompletos | Contagem, prova negativa e CI |
| Ferramentas excessivas | Uma ferramenta por finalidade |
| Schema antigo contaminando o novo | Schema vazio e histórico próprio |
| Perda de dado inesperado | Inspeção, backup e aprovação destrutiva |
| Starter atrasar feedback | Checkpoint após `IH-MVP-020` |
| Documentos divergirem | Hierarquia normativa e ADR |
| Branch longa | Uma capacidade revisável por PR |
| Rollback impossível | Compatibilidade de schema e projeções idempotentes |

## 21. Critérios de aprovação desta estratégia

Antes de marcar este documento como aprovado, devem ser confirmados:

1. substituição controlada em vez de coexistência;
2. ausência de necessidade de migrar dados reais;
3. novo schema Flyway independente;
4. mesmo repositório e subprojetos incrementais;
5. `communication` como nome canônico;
6. MIG-001 como primeiro incremento;
7. SLICE-000 antes de `ClientApplication`;
8. `ClientApplication` antes de identidade de usuário;
9. outbox entrando com a primeira projeção externa;
10. checkpoint de desenvolvimento antes da conclusão integral do MVP;
11. revisão humana ou entrega autônoma controlada com gates equivalentes por
    fatia.

## 22. Primeiro passo histórico após aprovação

O passo abaixo foi concluído por `MIG-001` e permanece registrado como histórico
da estratégia inicial.

Criar branch exclusiva para `MIG-001` e investigar a descoberta dos testes.

O contrato inicial será:

- resultado: o gate padrão executa todos os testes esperados;
- mudança máxima: build e testes estritamente necessários;
- proibido: refatorar produção, atualizar stack inteira ou remover legado;
- evidência: relatório antes/depois e prova de falha controlada;
- encerramento: revisão humana antes de commit e push, conforme o modo vigente
  naquele incremento.
