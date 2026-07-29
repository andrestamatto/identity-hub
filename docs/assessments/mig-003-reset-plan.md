# MIG-003 — Plano de reset controlado

> **Status:** approved
>
> **Data:** 2026-07-29
>
> **Branch:** `refactor/mig-003-foundation-reset`

## 1. Objetivo

Substituir a implementação abandonada por uma fundação Spring Boot mínima,
executável e protegida por testes, sem antecipar capacidades de produto.

O reset não migra comportamento antigo. A tag `v0.3.0` e o archive documental
permanecem como referências históricas.

## 2. Pré-condição comprovada

Antes de qualquer remoção:

- a branch foi criada a partir da `develop` no commit `378c5f4`;
- o comando `.\gradlew.bat clean test` passou;
- 30 classes de teste e 91 testes legados foram executados;
- nenhum arquivo versionado estava modificado.

O primeiro `clean` encontrou dois daemons do cache descartável do MIG-002 ainda
mantendo arquivos sob `build/` abertos. Somente esses processos foram encerrados;
o cache global do usuário não foi apagado. A repetição concluiu com sucesso.

## 3. Premissas

1. Não existem identidades ou dados de produção a migrar.
2. Nenhuma operação destrutiva será executada em banco de dados.
3. O repositório continua único.
4. `identityhub-service` será o único subprojeto neste incremento.
5. Starter e theme somente nascerão em suas próprias fatias.
6. Packages de negócio somente nascerão com comportamento verificável.
7. O Auto Radar não será alterado por esta branch.

## 4. Remoção executada após aprovação

Remover da árvore ativa:

- todo o `src/main` legado;
- todo o `src/test` legado e o gate fixo de 91 testes;
- domínio `User`, cadastro e confirmação antigos;
- hashing e configuração de pepper;
- persistence JPA e migration `users`;
- e-mail, SMS, WhatsApp, Twilio, OpenFeign e templates;
- profiles e configurações do comportamento abandonado;
- dependências exclusivas dessas capacidades.

Não remover:

- `docs/`;
- `docs/archive/v0.3.0/`;
- `.agents/product-marketing.md`;
- `.gitignore`;
- Gradle Wrapper;
- changelog e histórico Git.

## 5. Estrutura alvo

```text
identity-hub/
├── AGENTS.md
├── build.gradle
├── settings.gradle
├── gradle/wrapper/
├── docs/
└── identityhub-service/
    ├── build.gradle
    └── src/
        ├── main/
        │   ├── java/br/dev/andrestamatto/identityhub/bootstrap/
        │   │   ├── IdentityHubApplication.java
        │   │   ├── config/
        │   │   ├── observability/
        │   │   └── security/
        │   └── resources/application.yml
        └── test/java/br/dev/andrestamatto/identityhub/
            ├── bootstrap/
            └── architecture/
```

Nenhum package `identity`, `access`, `clientapplication`, `communication`,
`administration` ou `audit` será criado neste incremento.

## 6. Build mínimo

### 6.1 Baseline

- Java 21;
- Spring Boot 4.1.0;
- Spring Security 7.1.0 gerenciado;
- Gradle Wrapper 9.6.1;
- versão de desenvolvimento `0.4.0-SNAPSHOT`;
- ArchUnit 1.4.2 somente em testes;
- Checkstyle 13.9.0 como análise Java principal;
- JaCoCo 0.8.15 para visibilidade, sem limiar arbitrário.

O root project agrega o build. A aplicação e suas dependências pertencem ao
subprojeto `identityhub-service`.

### 6.2 Dependências do service

Adicionar somente:

- Spring Boot Web;
- Spring Boot Actuator;
- Spring Boot Security;
- configuration processor;
- Spring Boot Test;
- ArchUnit.

Não adicionar ainda:

- JPA, Flyway ou driver PostgreSQL;
- Keycloak ou cliente administrativo;
- Testcontainers;
- e-mail ou mensageria;
- frontend, Thymeleaf ou theme;
- starter de integração.

ArchUnit 1.4.2 é a release atual publicada pelo projeto oficial:
[ArchUnit — releases](https://www.archunit.org/news).

Fontes adicionais:

- [Checkstyle — release atual](https://checkstyle.org/);
- [JaCoCo — releases](https://www.jacoco.org/jacoco/index.html);
- [Gitleaks — release 8.30.1](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.1);
- [GitHub Dependency Review](https://github.com/actions/dependency-review-action).

## 7. Comportamento da fundação

### 7.1 Composition root

`IdentityHubApplication`:

- inicia o Spring Boot;
- descobre configuração tipada;
- não contém regra de negócio;
- não habilita adapters que ainda não existem.

### 7.2 Configuração tipada

Uma propriedade `identityhub.runtime.environment` será vinculada a um tipo
fechado com os valores:

- `development`;
- `production`.

O default local será `development`. Valores desconhecidos impedem o startup.
Nenhum segredo terá valor default.

### 7.3 Tempo

Um único bean `Clock.systemUTC()` será fornecido pela composition root. Casos de
uso futuros deverão receber `Clock` por injeção.

### 7.4 Segurança inicial

Uma `SecurityFilterChain` mínima:

- permite somente os endpoints de liveness e readiness;
- nega qualquer outra requisição;
- usa política stateless e não persiste contexto de segurança;
- mantém headers seguros padrão;
- não cria login, sessão de produto, JWT, CORS ou regras de negócio.

Essa política fail-closed existe antes do primeiro endpoint administrativo.

### 7.5 Logging e correlação

Um filtro de infraestrutura:

- aceita `X-Correlation-ID` somente em formato e tamanho limitados;
- gera identificador quando ausente ou inválido;
- inclui o valor no MDC durante a requisição;
- devolve o identificador na resposta;
- sempre limpa o MDC;
- nunca registra headers, tokens, cookies ou corpos.

A geração será injetável para permitir teste determinístico.

### 7.6 Health

Actuator expõe somente:

- `/actuator/health/liveness`;
- `/actuator/health/readiness`.

Detalhes internos não serão exibidos. Não haverá dependência externa neste
incremento; readiness passará a refletir PostgreSQL e Keycloak quando eles forem
introduzidos.

## 8. Testes sentinela

O novo gate deve demonstrar:

1. contexto Spring inicia;
2. configuração `development` é vinculada;
3. valor de ambiente inválido impede binding;
4. `Clock` usa UTC;
5. liveness e readiness respondem sem detalhes internos;
6. rota funcional legada não é acessível;
7. correlação recebida válida é propagada;
8. correlação inválida é substituída;
9. MDC é limpo depois da requisição;
10. classes de domínio futuras não podem depender de Spring, JPA ou Keycloak;
11. packages de capacidade não podem formar ciclos;
12. classes `org.keycloak` não podem atravessar a fronteira de adapter futura.

Regras sem classes correspondentes serão explicitamente preparatórias e deverão
falhar assim que uma violação for introduzida.

## 9. CI e comandos canônicos

Comando local e de CI:

```powershell
.\gradlew.bat clean build
```

O workflow inicial:

- usar Java 21;
- validar e executar o Gradle Wrapper;
- executar `clean build`;
- não depender de credenciais;
- não publicar artefatos ou imagens.

As actions são fixadas por SHA, com a versão legível ao lado. O
`setup-gradle` usa cache básico, valida o wrapper e executa o mesmo comando local.
Gitleaks 8.30.1 examina secrets e o Dependency Review bloqueia vulnerabilidades
novas de severidade moderada ou superior. A primeira execução remota depende da
publicação da branch.

## 10. Evidências da execução

| Verificação | Resultado |
|---|---|
| Baseline antes do reset | 91 testes legados verdes |
| Primeiro ciclo vermelho | testes não compilaram sem as classes de fundação |
| Build final | `clean build` verde com Gradle 9.6.1 |
| Testes sentinela | 11 testes verdes |
| Runtime de teste | Tomcat real em porta aleatória |
| Spring Boot | 4.1.0 |
| Spring Security | 7.1.0 gerenciado |
| Artefato | `identityhub-service.jar` gerado |
| Health | liveness e readiness `UP`, sem detalhes |
| Rota legada | `GET /users/register` retorna `403` pela política global |
| Credencial default | nenhum `UserDetailsService` ou senha gerada |
| Resíduos ativos | nenhum resultado para providers, hashing ou migration antiga |
| Conteúdo do JAR | nenhuma classe ou migration legada |
| Prova negativa ArchUnit | package temporário fora de `bootstrap` tornou o teste vermelho |
| Checkstyle | código principal e testes verdes |
| Prova negativa Checkstyle | import não usado temporário tornou o gate vermelho |
| JaCoCo | relatórios HTML e XML gerados, sem limiar arbitrário |
| Gitleaks | 133 commits e árvore atual examinados; scans verdes |
| Banco ou container | nenhuma operação executada |

O package e o import usados nas provas negativas foram removidos antes do build
final.
O teste web inicia a aplicação empacotável com servidor real; não foi criado
servidor substituto para testes.

O primeiro scan Gitleaks identificou três fingerprints na implementação
abandonada: uma configuração de teste e duas constantes JWT. Os valores
permaneceram redigidos e não estão na árvore ativa. Como o histórico e a tag devem
ser preservados, `.gitleaksignore` contém somente esses fingerprints específicos;
qualquer ocorrência nova continua bloqueante. Antes de produção, deve ser
confirmado que os valores históricos não são válidos ou foram revogados. A
aprovação do reset não presume essa confirmação.

## 11. Critérios de aceitação

- [x] build limpo e reproduzível;
- [x] aplicação inicia em Java 21 e Spring Boot 4.1;
- [x] `bootJar` é gerado;
- [x] health possui liveness e readiness;
- [x] qualquer rota funcional legada é negada;
- [x] busca na árvore ativa de código, recursos e build não encontra Twilio,
      WhatsApp, hashing ou migration `users`;
- [x] somente o package `bootstrap` existe na aplicação;
- [x] configuração tipada e `Clock` são injetáveis;
- [x] correlação não vaza entre requisições;
- [x] regras ArchUnit passam;
- [x] análise estática é bloqueante;
- [x] cobertura fica visível em relatório;
- [x] scan completo de secrets passa com baseline histórico restrito;
- [x] CI está configurada para executar o mesmo build local;
- [x] documentação aponta para `v0.3.0` como recuperação histórica;
- [x] nenhum banco, schema ou dado externo foi alterado.

A execução remota da CI permanece como gate posterior ao push.

## 12. Sequência TDD executada

1. criar o subprojeto e um teste de contexto vermelho;
2. implementar a composition root mínima;
3. escrever testes de configuração e `Clock`;
4. implementar apenas o necessário para torná-los verdes;
5. escrever testes de health e política fail-closed;
6. implementar Actuator e segurança mínima;
7. escrever testes de correlação;
8. implementar o filtro mínimo;
9. adicionar regras ArchUnit e suas provas negativas;
10. remover a árvore legada;
11. executar busca de resíduos, `clean build` e `bootJar`;
12. adicionar CI, `AGENTS.md` e evidência final do MIG-003.

## 13. Estratégia de commits

Após aprovação e validação:

1. `build(platform): create Spring Boot 4 service module`
2. `refactor(foundation): remove abandoned implementation`
3. `test(architecture): add foundation guardrails`
4. `ci(build): add canonical Gradle verification`
5. `docs(migration): record MIG-003 reset evidence`

Cada commit deve representar um contexto revisável. Nenhum commit ou push será
feito antes da aprovação humana do resultado.

## 14. Rollback

O rollback é o revert do PR do MIG-003. O código removido também permanece:

- no histórico Git;
- na tag `v0.3.0`;
- na documentação arquivada em `docs/archive/v0.3.0/`.
