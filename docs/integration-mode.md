# Integration Mode — IdentityHub

> **Status:** aprovado
>
> **Versão do documento:** 1.0
>
> **Última atualização:** 2026-07-28

## 1. Finalidade

Este documento define o contrato técnico e a experiência de desenvolvimento do
Integration Mode no MVP do IdentityHub.

O Integration Mode é uma biblioteca incorporada a aplicações Java/Spring Boot
consumidoras. Ele reduz a configuração necessária para proteger APIs e integrar
operações autorizadas com o Service Mode, sem incorporar o motor de identidade no
processo consumidor.

Este documento detalha:

- o starter Spring Boot;
- a configuração de runtime;
- o manifesto declarativo da aplicação;
- validação de JWT e mapeamento de authorities;
- o cliente tipado opcional;
- o console local em `/identity-hub-config`;
- diagnóstico, diff e aplicação explícita de configuração;
- extensibilidade, compatibilidade e testes.

## 2. Objetivos

O Integration Mode deve:

1. tornar segura a configuração padrão de uma API Java;
2. esconder contratos internos do Keycloak;
3. validar issuer, audience, assinatura e validade de todo access token;
4. mapear apenas claims públicos e documentados;
5. permitir que o consumidor substitua componentes sem copiar a implementação;
6. separar configuração de runtime de configuração remota desejada;
7. diagnosticar erros antes de uma alteração remota;
8. nunca alterar o Service Mode silenciosamente no startup;
9. manter segredos fora de arquivos versionados;
10. permanecer pequeno o suficiente para não transformar o consumidor em parte do
    IdentityHub.

## 3. Não objetivos

O Integration Mode não:

- armazena usuários, senhas, sessões ou memberships;
- emite access tokens, ID tokens ou refresh tokens;
- executa login ou cadastro dentro da aplicação consumidora;
- incorpora Keycloak;
- acessa Admin REST API, banco ou classes do Keycloak;
- decide regras contextuais do domínio consumidor;
- substitui o frontend hospedado de autenticação;
- sincroniza configuração remota a cada startup;
- oferece Local Development Mode completo;
- suporta WebFlux no primeiro release do MVP;
- fornece SDK para frontend ou aplicativo móvel no primeiro release.

## 4. Unidade de distribuição

O MVP publica um único artefato:

```text
br.dev.andrestamatto.identityhub:identityhub-spring-boot-starter
```

Esse artefato contém:

- auto-configuração Spring Boot;
- suporte Servlet para OAuth 2.0 Resource Server;
- validadores de JWT;
- conversor de authorities;
- propriedades e metadata para IDE;
- cliente HTTP tipado opcional;
- parser e validador do manifesto;
- endpoints locais do console, condicionais;
- bundle estático do console.

Uma separação futura entre `autoconfigure`, `starter`, `client` ou `console` somente
será feita se compatibilidade, tamanho ou consumidores reais demonstrarem
necessidade. A distribuição pública continua sendo um único starter no MVP.

## 5. Compatibilidade

### 5.1 Baseline

- Java 21;
- Spring Boot com suporte oficial ativo;
- aplicação Servlet;
- configuração por YAML ou properties;
- Gradle e Maven como sistemas consumidores;
- JWT assinado assimetricamente.

A primeira implementação deve escolher e registrar em ADR uma única linha de Spring
Boot. Não haverá compromisso de compatibilidade simultânea entre majors no MVP. A
linha escolhida deve ser compatível com o primeiro SaaS consumidor e não pode estar
fora de suporte na data da release.

### 5.2 Matriz publicada

Cada release do starter deve publicar:

| Informação | Exemplo ilustrativo |
|---|---|
| Versão do starter | `1.x` |
| Java mínimo | `21` |
| Spring Boot suportado | linha definida no ADR |
| Contrato IdentityHub | `v1` |
| Formato do manifesto | `identityhub.dev/v1alpha1` |

Uma combinação fora da matriz não é considerada suportada, mesmo que compile.

## 6. Instalação

### 6.1 Gradle

```groovy
dependencies {
    implementation "br.dev.andrestamatto.identityhub:identityhub-spring-boot-starter:<version>"
}
```

### 6.2 Maven

```xml
<dependency>
    <groupId>br.dev.andrestamatto.identityhub</groupId>
    <artifactId>identityhub-spring-boot-starter</artifactId>
    <version><!-- versão fixada --></version>
</dependency>
```

O consumidor deve fixar a versão. Intervalos abertos e versões dinâmicas não são
suportados.

## 7. Dois tipos de configuração

O Integration Mode separa deliberadamente:

| Configuração | Local padrão | Finalidade | Pode conter segredo? |
|---|---|---|---|
| Runtime | `application.yml` ou ambiente | Proteger a instância em execução | Somente por referência externa |
| Estado desejado | `identityhub/identityhub.yaml` | Descrever a aplicação no Service Mode | Não |

Essa separação impede que a inicialização normal da API seja confundida com uma
operação administrativa.

## 8. Configuração de runtime

### 8.1 Propriedades mínimas

```yaml
identityhub:
  security:
    enabled: true
    issuer-uri: https://auth.dev.andrestamatto.dev.br
    audience: catalog-api
```

### 8.2 Propriedades suportadas

```yaml
identityhub:
  security:
    enabled: true
    issuer-uri: https://auth.dev.andrestamatto.dev.br
    audience: catalog-api
    clock-skew: 60s
    authorities:
      role-prefix: ROLE_
      scope-prefix: SCOPE_
  client:
    enabled: false
    base-url: https://auth.dev.andrestamatto.dev.br
    client-id: ${IDENTITYHUB_MACHINE_CLIENT_ID:}
    client-secret: ${IDENTITYHUB_MACHINE_CLIENT_SECRET}
    connect-timeout: 2s
    read-timeout: 5s
  console:
    enabled: false
    manifest: classpath:identityhub/identityhub.yaml
    path: /identity-hub-config
```

### 8.3 Regras

- o namespace é exclusivamente `identityhub`;
- `issuer-uri` e `audience` são obrigatórios quando segurança estiver habilitada;
- issuer deve ser HTTPS, exceto ambiente local explicitamente permitido;
- audience deve conter um identificador exato, nunca padrão ou expressão;
- duração inválida ou negativa impede o startup;
- segredo literal no YAML produz falha ou alerta bloqueante conforme a origem;
- propriedades possuem Javadoc e metadata para autocomplete;
- valores podem ser substituídos pelos mecanismos normais do Spring;
- nenhuma propriedade habilita implicitamente console ou cliente de máquina.

## 9. Auto-configuração de segurança

### 9.1 Componentes fornecidos

O starter deve fornecer, quando as condições forem atendidas:

- `JwtDecoder` com validação de issuer, audience, assinatura e tempo;
- `OAuth2TokenValidator<Jwt>` composto;
- `Converter<Jwt, AbstractAuthenticationToken>` para claims públicos;
- `AuthenticationEntryPoint` e `AccessDeniedHandler` sem dados sensíveis;
- `SecurityFilterChain` segura somente quando o consumidor não declarar uma própria;
- health contributor sanitizado;
- propriedades validadas no startup.

As classes de auto-configuração usam mecanismos oficiais do Spring Boot, recuam
diante de beans definidos pelo consumidor e não usam component scanning.

### 9.2 Política padrão

Na ausência de `SecurityFilterChain` do consumidor, o starter:

- configura Resource Server JWT;
- exige autenticação para toda requisição;
- não libera endpoint anônimo;
- usa sessão `STATELESS`;
- desabilita CSRF somente nessa cadeia Bearer stateless;
- não habilita CORS globalmente;
- não ativa method security implicitamente.

O consumidor pode fornecer sua própria `SecurityFilterChain`. Nesse caso, o starter
mantém decoder, validadores e conversor disponíveis, mas não deve competir pela
ordem dos filtros. Aplicações que também utilizam cookie devem preservar proteção
CSRF nas cadeias correspondentes.

### 9.3 Falha segura

A aplicação não deve iniciar com proteção parcialmente configurada.

Devem impedir o startup:

- issuer ausente ou inválido;
- audience ausente;
- algoritmo não permitido;
- configuração contraditória;
- console habilitado em ambiente proibido;
- cliente tipado habilitado sem credenciais resolvíveis;
- manifesto obrigatório ilegível quando uma operação do console for solicitada.

Se discovery ou JWKS forem indispensáveis e estiverem indisponíveis no primeiro
startup, a inicialização falha. Durante a execução, chaves já obtidas podem ser
usadas enquanto permanecerem válidas e confiáveis; sem material confiável,
requisições autenticadas falham fechadas. Cache persistente entre processos fica
fora do MVP.

## 10. Contrato público de access token

O starter conhece apenas o contrato IdentityHub:

| Claim | Uso |
|---|---|
| `iss` | Emissor exato |
| `sub` | Identificador opaco do usuário ou cliente |
| `aud` | API destinatária |
| `exp` | Expiração |
| `iat` | Instante de emissão |
| `jti` | Identificador do token |
| `azp` | Cliente autorizado, quando aplicável |
| `scope` | Escopos concedidos |
| `roles` | Papéis da aplicação destinatária |
| `sid` | Sessão, quando aplicável |

O starter não lê `realm_access`, `resource_access` ou outro claim privado do
Keycloak. A projeção do Service Mode deve entregar o formato público acima.

Na fatia inicial de audience pública, `roles` é emitido como lista vazia. A
presença da audience esperada demonstra somente a Membership ativa; papéis de
negócio serão introduzidos por uma capacidade posterior e não devem ser inferidos
de claims privados do motor.

### 10.1 Authorities

- cada scope válido vira `SCOPE_<scope>`;
- cada role válida vira `ROLE_<role>`;
- nomes vazios, duplicados, excessivos ou fora do formato são rejeitados;
- prefixos são configuráveis, mas não podem ser vazios;
- roles de plataforma não são aceitas em audience de SaaS;
- e-mail, telefone e nome não viram authority;
- o domínio consumidor pode combinar authorities com regras próprias.

### 10.2 Audience

O token deve conter a audience configurada para a API. A presença de outra audience
não substitui a esperada. Token sem audience, com audience errada ou destinado
somente a outro serviço é rejeitado.

## 11. Manifesto declarativo

### 11.1 Local e formato

O local padrão é:

```text
src/main/resources/identityhub/identityhub.yaml
```

Exemplo:

```yaml
apiVersion: identityhub.dev/v1alpha1
kind: ClientApplication

metadata:
  key: social-catalog
  displayName: Social Catalog

spec:
  environment: development
  issuer: https://auth.dev.andrestamatto.dev.br

  authenticationMethods:
    - PASSWORD
    - GOOGLE
    - GITHUB

  registration:
    enabled: true
    phoneContact: OPTIONAL

  clients:
    - key: social-catalog-web
      type: SPA
      redirectUris:
        - http://127.0.0.1:5173/auth/callback
      webOrigins:
        - http://127.0.0.1:5173

    - key: social-catalog-api
      type: API
      audience: social-catalog-api

    - key: social-catalog-provisioning
      type: MACHINE
      scopes:
        - membership:write

  roles:
    - USER
    - PREMIUM

  branding:
    displayName: Social Catalog
    logo: classpath:identityhub/branding/logo.webp
    colorMode: SYSTEM
    primaryColor: "#0F766E"
```

### 11.2 Conteúdo permitido

O manifesto pode declarar:

- identificação lógica da aplicação;
- ambiente e issuer;
- clientes SPA, BFF, API e máquina;
- URIs e origens;
- métodos de autenticação;
- política de cadastro e telefone;
- papéis gerais;
- branding seguro;
- referências locais de artefatos;
- versão do contrato.

O manifesto nunca contém:

- client secret;
- senha;
- token;
- chave privada;
- credencial de provedor social;
- credencial SMTP, SMS ou WhatsApp;
- TOTP seed ou recovery code;
- HTML, JavaScript ou CSS arbitrário.

### 11.3 Semântica

- listas sem ordem de negócio são comparadas semanticamente;
- chaves lógicas são estáveis e únicas dentro da aplicação;
- campos desconhecidos são rejeitados;
- default aplicado pelo parser aparece no diff;
- caminho `classpath:` é resolvido apenas pelo processo local;
- URL remota para logotipo não é aceita;
- o arquivo é validável offline por schema versionado;
- migração de formato não acontece silenciosamente.

## 12. Ciclo validate, diff e apply

```mermaid
flowchart LR
    Manifest[Manifesto local] --> Validate[Validate]
    Validate -->|válido| Effective[Consultar estado efetivo]
    Effective --> Diff[Diff semântico]
    Diff -->|alterações| Confirm[Confirmação explícita]
    Confirm --> Auth[MFA e autorização]
    Auth --> Upload[Upload seguro de artefatos]
    Upload --> Apply[Apply idempotente]
    Apply --> Verify[Reler e verificar]
```

### 12.1 Validate

Validação local verifica:

- schema e tipos;
- chaves duplicadas;
- compatibilidade entre tipo de cliente e campos;
- HTTPS e formato de URI;
- wildcard inseguro;
- origem incompatível;
- método não suportado;
- telefone fora dos valores permitidos;
- role reservada ou inválida;
- artefato ausente, excedente ou de tipo proibido;
- segredo aparente;
- ambiente e issuer incompatíveis.

Nenhuma chamada remota é necessária para essa etapa.

### 12.2 Diff

Após autenticação adequada, a ferramenta obtém o estado efetivo pelo contrato do
IdentityHub e apresenta:

- criação;
- alteração;
- remoção;
- valor mascarado;
- diferença de default;
- drift remoto;
- operação potencialmente destrutiva;
- ausência de mudança.

O diff não exibe segredos existentes nem depende de representação do Keycloak.

### 12.3 Apply

Apply:

- nunca ocorre no startup;
- exige ação explícita;
- exige `PLATFORM_ADMIN`;
- exige autenticação recente e MFA;
- usa versão ou ETag para impedir lost update;
- usa idempotency key;
- envia arquivos locais, não caminhos;
- valida novamente no servidor;
- apresenta resultado por operação;
- relê o estado efetivo ao final;
- produz auditoria e correlação.

Se o estado mudar depois do diff, o apply é rejeitado e um novo diff é exigido.
Falha parcial deve ser apresentada com estado observável e capacidade de retry
seguro, sem afirmar sucesso total.

### 12.4 Credenciais geradas

Quando a criação de cliente confidencial produzir uma credencial:

- o valor aparece uma única vez em canal protegido;
- não é gravado no manifesto;
- não é persistido pelo console;
- deve ser copiado diretamente para o secret manager do consumidor;
- fechar ou atualizar a página elimina a visualização;
- uma nova exibição exige rotação, não recuperação.

## 13. Console local

### 13.1 Experiência

Quando habilitado, o console fica disponível em:

```text
http://127.0.0.1:<porta>/identity-hub-config
```

Ele possui cinco áreas:

1. **Overview:** aplicação, ambiente, issuer e status da integração;
2. **Validate:** problemas do manifesto com localização e orientação;
3. **Diff:** comparação semântica antes de qualquer mutação;
4. **Apply:** confirmação, progresso e resultado auditável;
5. **Diagnostics:** conectividade, JWKS, audience, versão e correlação.

Branding inclui preview seguro. O console não renderiza HTML ou CSS fornecido pelo
manifesto.

### 13.2 Tecnologia

- React e TypeScript;
- Vite somente no build do IdentityHub;
- bundle estático empacotado no JAR;
- nenhum Node.js no runtime consumidor;
- nenhuma dependência de CDN;
- fontes e assets locais;
- API local sob `/identity-hub-config/api`;
- estado de autenticação fora de Web Storage;
- suporte a tema claro, escuro e preferência do sistema;
- navegação por teclado e contraste acessível.

O console é parte do mesmo artefato do starter e não constitui serviço ou frontend
implantável separado.

### 13.3 Direção visual

A interface deve ser uma ferramenta técnica sóbria e reconhecível:

- alta densidade informacional sem poluição;
- diff como elemento visual principal;
- estado de ambiente sempre visível;
- produção identificada por tratamento visual inequívoco;
- ações destrutivas separadas e descritas;
- ausência de animações que atrasem diagnóstico;
- linguagem objetiva e erros acionáveis;
- identidade visual do IdentityHub, não o branding do SaaS configurado.

### 13.4 Condições de ativação

O console somente inicia quando:

- `identityhub.console.enabled=true`;
- o perfil ou ambiente local permitido está ativo;
- `server.address` está explicitamente restrito a `127.0.0.1` ou `::1`;
- a aplicação não está executando em ambiente de produção;
- o manifesto pode ser localizado;
- a cadeia de segurança específica foi criada com sucesso.

Ter o perfil `dev` não é controle de segurança suficiente isoladamente.

### 13.5 Autenticação administrativa

- Authorization Code com PKCE;
- cliente público exclusivo para ferramenta administrativa local;
- callback exato em endereço IP de loopback;
- token mantido somente no backend local e em memória volátil;
- navegador recebe cookie de sessão `HttpOnly`, `SameSite=Lax`;
- CSRF obrigatório nas mutações;
- `PLATFORM_AUDITOR` pode validar e consultar diff;
- somente `PLATFORM_ADMIN` pode aplicar;
- apply exige autenticação recente;
- logout apaga sessão e tokens locais.

O console não solicita nem recebe senha diretamente. O login acontece no domínio
oficial do IdentityHub.

### 13.6 Proteção contra exposição

- requisição não originada de loopback é rejeitada;
- forwarded headers não são confiados sem proxy explicitamente configurado;
- bind ausente, ambíguo ou em interface pública impede o startup do console;
- respostas não contêm segredos ou tokens;
- CSP restritiva e `frame-ancestors 'none'`;
- `Cache-Control: no-store` nas respostas autenticadas;
- limites de tamanho e frequência;
- erros não retornam stack trace;
- endpoints do console não são incluídos na cadeia comum da API consumidora.

### 13.7 Alterações em produção

O console nunca é servido pela instância produtiva do SaaS consumidor.

Uma execução local pode consultar produção somente quando o target estiver fixado
no manifesto e a autenticação ocorrer no issuer de produção. Apply em produção
permanece desabilitado por padrão e exige:

- opt-in local por variável de ambiente não versionada;
- confirmação textual da aplicação e do ambiente;
- `PLATFORM_ADMIN` de produção com MFA e autenticação recente;
- novo diff sem drift;
- auditoria destacada.

Não existe seletor livre de URL do servidor na interface.

## 14. Cliente tipado opcional

O cliente de máquina é habilitado separadamente do Resource Server.

O contrato inicial pode oferecer:

- conceder membership;
- suspender membership;
- remover membership;
- atribuir ou remover role permitida;
- consultar o resultado de operação própria;
- provisionar acesso usando o `sub` obtido pelo backend em um fluxo OIDC validado.

Regras:

- usa somente APIs públicas do IdentityHub;
- obtém token por Client Credentials;
- solicita a audience `identityhub-integration-api` e exige o scope
  `membership:write` para mutações de acesso;
- não representa usuário;
- não envia identificador de aplicação: o IdentityHub deriva o escopo da
  aplicação pelo `azp` validado do cliente de máquina;
- nunca aceita do navegador um identificador humano como autoridade;
- não recebe refresh token;
- propaga `traceparent` e correlation ID;
- exige idempotency key nas mutações;
- possui timeout;
- não repete automaticamente mutação não idempotente;
- não registra payload sensível;
- permite substituição por bean do consumidor;
- falha de chamada não altera a decisão de autenticação local da API.

Credenciais são fornecidas pelo ambiente ou secret manager. O manifesto contém
somente a identidade lógica do cliente.

### 14.1 Contrato HTTP inicial de concessão

Enquanto o cliente tipado não for entregue, o contrato público inicial é:

```http
POST /api/v1/memberships
Authorization: Bearer <client-credentials-access-token>
Idempotency-Key: <opaque-key>
Content-Type: application/json

{
  "userAccountRef": "680ac2e4-bfb0-4375-a75e-453b6e7b600c"
}
```

Uma solicitação aceita retorna `202 Accepted` com `operationId`, `membershipId`,
`state=PENDING` e `acceptedAt`. A resposta não devolve application id nem dados
comerciais. O estado `PENDING` prova a intenção durável, mas ainda não concede
acesso; a ativação depende da projeção confirmada no motor interno.

A mesma chave com o mesmo comando devolve a mesma operação. A reutilização da
chave com outro comando retorna conflito. Campos desconhecidos, inclusive
qualquer application id informado pelo solicitante, são rejeitados.

### 14.2 Consulta e reconciliação da operação

O cliente acompanha somente operações da própria aplicação:

```http
GET /api/v1/membership-operations/{operationId}
Authorization: Bearer <client-credentials-access-token>
```

A resposta contém `operationId`, `membershipId`, `membershipState`,
`projectionState`, `attempts`, `lastFailureCode`, `acceptedAt` e `updatedAt`.
Não contém application id, usuário ou mensagem interna do provedor. Operação
desconhecida e operação de outra aplicação retornam a mesma resposta `404`.

Uma projeção `FAILED` conhecida pode ser recolocada em processamento:

```http
POST /api/v1/membership-operations/{operationId}/projection/reconcile
Authorization: Bearer <client-credentials-access-token>
```

O comando exige a mesma audience e o scope `membership:write`, retorna
`202 Accepted` e é idempotente enquanto a operação já estiver `PENDING`. A
reconciliação não amplia acesso: a Membership volta ou permanece `PENDING` até
nova confirmação remota.

## 15. Diagnóstico e observabilidade

### 15.1 Startup

O starter registra, sem valores sensíveis:

- versão;
- issuer normalizado;
- audience;
- recursos habilitados;
- resultado da validação;
- substituições feitas pelo consumidor.

### 15.2 Health

O health contributor distingue:

- configuração local válida;
- material de assinatura disponível;
- último refresh de JWKS;
- cliente tipado habilitado;
- Service Mode alcançável para operações administrativas.

Health público retorna somente estado agregado. Detalhes ficam restritos ao
Actuator autorizado e ao console local.

### 15.3 Métricas

- tokens aceitos e rejeitados por motivo não sensível;
- falha de audience;
- falha de issuer;
- `kid` desconhecido;
- atualização de JWKS;
- latência e resultado do cliente tipado;
- validate, diff e apply;
- drift e conflito de versão.

Nenhuma métrica usa `sub`, e-mail, token ou client secret como label.

## 16. Erros públicos

Para recursos protegidos:

- ausência ou invalidade de autenticação resulta em `401`;
- autenticação válida sem authority resulta em `403`;
- `WWW-Authenticate` segue o padrão Bearer;
- detalhes internos de validação permanecem em observabilidade protegida;
- resposta não diferencia informação útil para enumeração.

O cliente tipado usa exceções estáveis do IdentityHub e preserva status, correlation
ID e categoria de erro. Ele não expõe DTO ou exceção do Keycloak.

## 17. Extensibilidade

O consumidor pode substituir:

- `JwtDecoder`;
- validadores adicionais;
- conversor de authorities;
- `SecurityFilterChain`;
- transporte do cliente tipado;
- correlation ID provider;
- clock para testes.

O starter deve recuar diante de beans equivalentes e documentar a consequência de
cada substituição. Extensões não podem desabilitar silenciosamente issuer ou
audience; fazê-lo exige configuração explícita fora do caminho suportado.

## 18. Testes

### 18.1 Starter

- `ApplicationContextRunner` para condições e override de beans;
- binding e validação de propriedades;
- startup inválido;
- cadeia segura padrão;
- cadeia fornecida pelo consumidor;
- issuer, audience, assinatura e expiração;
- mapping de scopes e roles;
- ausência de dependência em claims privados;
- respostas `401` e `403`;
- cache e rotação de JWKS;
- falha fechada sem chave confiável.

### 18.2 Manifesto e console

- schema e compatibilidade de versão;
- campos desconhecidos;
- detecção de segredo;
- URIs, origens e tipos de cliente;
- diff determinístico;
- lost update;
- apply idempotente;
- upload e limites;
- CSRF;
- bloqueio fora de loopback;
- bloqueio em produção;
- roles administrativas;
- acessibilidade e navegação por teclado;
- browser E2E.

### 18.3 Integração

Testcontainers deve iniciar a versão fixada do Keycloak para validar:

- discovery e JWKS;
- token real aceito;
- issuer ou audience errados;
- rotação de chave;
- Client Credentials;
- fluxo administrativo do console;
- ausência de acoplamento a claim privado.

Contratos HTTP do plano de controle podem usar servidor simulado em testes rápidos,
mas a suíte de compatibilidade deve executar contra o Service Mode real.

## 19. Compatibilidade e evolução

### 19.1 Versionamento

- Semantic Versioning para o artefato;
- contrato HTTP versionado;
- manifesto com `apiVersion`;
- remoção incompatível somente em major;
- depreciação documentada antes da remoção;
- metadata de configuração validada em IDE compatível.

### 19.2 Independência do motor

Uma atualização compatível do Keycloak não pode exigir mudança no consumidor.

Teste arquitetural deve impedir imports de pacotes `org.keycloak` no starter. O
starter depende de Spring Security, contratos IdentityHub e padrões OAuth/OIDC.

## 20. Critérios de aceitação

### 20.1 API Java

1. Adicionar o starter e informar issuer e audience protege a API por padrão.
2. Token válido para a audience correta é autenticado.
3. Token expirado, alterado, de outro issuer ou audience é rejeitado.
4. Scope e role documentados viram authorities previsíveis.
5. Token com claim privado do Keycloak não recebe privilégio adicional.
6. Configuração própria do consumidor substitui defaults sem duplicar filtros.
7. Falha sem chave confiável não libera acesso.

### 20.2 Configuração e console

1. Manifesto válido é analisado sem chamada remota.
2. Manifesto inseguro mostra campo e orientação antes do diff.
3. Diff sem alteração não oferece apply desnecessário.
4. Mudanças concorrentes impedem apply sobre estado antigo.
5. Apply idêntico não cria recursos duplicados.
6. Logotipo local é enviado como arquivo e o servidor não acessa o classpath remoto.
7. Segredo não é gravado no manifesto nem persistido pelo console.
8. Console permanece indisponível por padrão e fora de produção.
9. Auditor consulta diff sem conseguir aplicar.
10. Admin com MFA aplica e recebe resultado correlacionável.

## 21. Decisões que exigem ADR ou spike

Antes da implementação:

1. selecionar a linha inicial de Spring Boot compatível com o primeiro SaaS;
2. comprovar o fluxo de callback loopback com o cliente administrativo do Keycloak;
3. definir o formato HTTP versionado de desired state, diff e apply;
4. definir a estratégia de cache confiável de JWKS para startup offline;
5. validar o tamanho do bundle do console dentro do starter;
6. comprovar que a proteção de loopback funciona nos ambientes locais suportados,
   inclusive Docker quando fizer parte do fluxo.

## 22. Referências

- [Spring Boot — Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)
- [Spring Security — OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [RFC 9700 — Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- [RFC 8252 — OAuth 2.0 for Native Apps](https://www.rfc-editor.org/rfc/rfc8252.html)
- [IdentityHub — Especificação do MVP](identityhub-spec.md)
- [IdentityHub — Arquitetura](architecture.md)
- [IdentityHub — Modelo de segurança](security-model.md)
