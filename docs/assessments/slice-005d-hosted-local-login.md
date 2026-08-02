# SLICE-005D — Login local hospedado

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-02
>
> **Branch:** `feat/slice-005d-local-login`

## 1. Resultado observável

Uma identidade local habilitada e com e-mail verificado autentica-se na página
hospedada do IdentityHub por OpenID Connect Authorization Code com PKCE. A senha
permanece no motor interno, Direct Access Grants continuam desabilitados e o
resultado sem `Membership` não recebe audience ou papéis de API de negócio.

## 2. Rastreabilidade

- `identityhub-spec.md`: `IH-MVP-006` e parte de `IH-MVP-010`;
- `architecture.md`: seções 8.2 e 14.3;
- `security-model.md`: seções 8, 20, 25 e 27;
- ADR-0001, ADR-0004, ADR-0010, ADR-0012 e ADR-0014.

## 3. Critérios de aceitação

- login de conta verificada e habilitada conclui Authorization Code + PKCE S256;
- senha incorreta, conta inexistente e conta desabilitada exibem erro público
  genérico, sem entregar authorization code;
- cinco falhas por conta ativam espera temporária progressiva, sem lockout
  permanente automático;
- eventos nativos registram sucesso e falha sem senha, token ou código;
- Direct Access Grants, Implicit Flow e offline access permanecem desabilitados;
- o token obtido sem `Membership` não possui audience ou papéis de API de negócio;
- o harness aplica a baseline de login tanto a realm novo quanto preexistente;
- testes reais usam Keycloak e PostgreSQL descartáveis por Testcontainers.

## 4. Dentro do escopo

- baseline de brute force e eventos do realm;
- prova de contrato do login local hospedado;
- cenários negativos de autenticação;
- harness e documentação operacional correspondentes.

## 5. Fora do escopo

- endpoint próprio que receba senha;
- `Membership`, roles ou audience de negócio;
- callback implementado dentro de um SaaS consumidor;
- recuperação de senha, refresh, logout, login social, tema e branding;
- API de consulta agregada dos eventos do motor.

## 6. Riscos e rollback

O risco principal é bloquear temporariamente uma conta legítima por abuso. A
baseline usa espera progressiva, limita o máximo a quinze minutos e não habilita
lockout permanente. O rollback operacional restaura a configuração anterior do
realm; nenhum dado ou migration é alterado.

## 7. Recuperação de contexto

As fontes normativas, a sequência funcional, a última slice, o working tree e as
pendências foram revalidados antes da definição destes critérios.

## 8. Evidências

- o ciclo TDD começou com falha em `test_local_dev.py` por ausência da baseline
  de brute force, antes de sua implementação;
- um teste real expôs que o `Quick Login Check` padrão bloqueava a conta cedo e
  não contabilizava as tentativas seguintes; a configuração passou a desabilitar
  esse atalho para cumprir literalmente cinco falhas antes da espera de 30
  segundos, conforme a semântica documentada pelo Keycloak;
- `KeycloakAdminTokenIntegrationTest` demonstrou login hospedado completo com
  Authorization Code + PKCE, validação de issuer e assinatura, `sub` opaco,
  ausência de audience/papéis de negócio, falhas genéricas, bloqueio temporário e
  eventos `LOGIN`/`LOGIN_ERROR` sem credenciais;
- `python3 scripts/test_local_dev.py`: cinco testes verdes;
- `./gradlew.bat clean build --console=plain`: verde em 1m56s no Windows,
  incluindo `bootJar`, Checkstyle, JaCoCo e regressões;
- `./gradlew clean build --console=plain`: verde em 9m57s no Linux/WSL com
  PostgreSQL, Keycloak e Mailpit reais; 224 testes, zero falhas, erros ou
  ignorados;
- `local-env up` equivalente reaplicou a baseline com sucesso ao realm local já
  existente, sem recriação e sem alteração destrutiva;
- `git diff --check` ficou verde e o diff não contém migration, arquivo local,
  segredo real, log, IDE ou artefato gerado.

## 9. Operação e pendências

O harness cobre realms novos e preexistentes, mas não é configuração de produção.
O limite adicional de login por IP depende da topologia confiável de proxy/WAF e
permanece registrado em `PD-002`; a proteção por conta já está ativa e essa
pendência não autoriza exposição produtiva sem o gate de staging. `PD-001` ainda
impede a entrega produtiva de e-mail, sem bloquear a prova local de login.

Não houve mudança de contrato público, migration, nova dependência ou endpoint de
senha. O próximo passo da sequência aprovada é recuperação e lifecycle
(`IH-MVP-015` e `IH-MVP-017`).
