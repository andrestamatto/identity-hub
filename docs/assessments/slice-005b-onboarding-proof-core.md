# SLICE-005B — Núcleo da prova de identidade de onboarding

> **Superseded:** o runtime e o contrato desta fatia foram removidos pela
> SLICE-005C e pela ADR-0014. Este documento permanece como evidência histórica.

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-02
>
> **Branch:** `feat/slice-005b-onboarding-proof-core`
>
> **Base:** `develop` em `2345bc1`

## 1. Resultado observável

Uma identidade humana já autenticada e com e-mail verificado pode concluir uma
`OnboardingSession` pendente exatamente uma vez. A conclusão devolve a prova opaca
somente nessa chamada, persiste apenas seu digest e não cria `Membership`, token ou
efeito externo.

## 2. Requisitos relacionados

- `IH-MVP-006`: autenticação sem acesso produz no máximo prova restrita;
- `IH-MVP-010`: ausência de `Membership` não autoriza recurso protegido;
- `IH-MVP-016`: prova vinculada à aplicação e à aquisição;
- ADR-0013 e `security-model.md` seção 9.

## 3. Critérios de aceitação

- somente sessão `PENDING`, existente e não expirada pode ser concluída;
- identidade e e-mail verificado são entradas obrigatórias do limite interno;
- a sessão passa atomicamente para `PROOF_ISSUED` e não pode emitir outra prova;
- a prova possui 256 bits aleatórios, validade de 30 minutos e é devolvida uma vez;
- o PostgreSQL armazena somente SHA-256 da prova, nunca seu valor original;
- a prova fica vinculada à sessão, usuário opaco, aplicação, aquisição, verificação
  de e-mail e correlação;
- concorrência sobre a mesma sessão produz uma única prova persistida;
- falha, expiração ou repetição não altera o estado persistido;
- nenhum log, métrica ou `toString` contém prova, digest, usuário ou aquisição;
- a operação não cria `Membership`, não emite token e não chama o Keycloak.

## 4. Dentro do escopo

- lifecycle de domínio da sessão e da prova;
- geração criptograficamente segura e hashing da prova;
- transação e persistência PostgreSQL;
- testes unitários, de aplicação e de integração concorrente.

## 5. Fora do escopo

- endpoint público ou interno;
- redirecionamento do navegador, `state`, `nonce` e callback OIDC;
- escolha entre fachada OIDC e extensão suportada do Keycloak;
- autenticação de senha propriamente dita;
- entrega, troca ou consumo da prova pelo SaaS;
- criação ou projeção de `Membership`.

## 6. Riscos e rollback

O principal risco é emitir mais de uma prova em retry ou corrida. A sessão será
bloqueada dentro da transação e possuirá unicidade de prova no banco. O valor bruto
não poderá ser recuperado após a resposta inicial.

Antes de produção, rollback é o revert do PR. A migration será somente aditiva e
não removerá registros automaticamente. A borda pública permanece inexistente,
portanto esta fatia não amplia a superfície HTTP.

## 7. Implementação

- `OnboardingSession` controla a transição imutável de `PENDING` para
  `PROOF_ISSUED` e cria a prova com os mesmos vínculos de aplicação e aquisição;
- `OnboardingProofIssuance` impede que sessão e prova inconsistentes alcancem o
  adaptador de persistência;
- `VerifiedOnboardingIdentity` representa no limite interno uma identidade que já
  passou pela autenticação e pela verificação obrigatória de e-mail;
- `SecureRandomOnboardingProofTokenGenerator` produz 32 bytes com `SecureRandom` e
  codifica 43 caracteres Base64 URL-safe sem padding;
- `IssueOnboardingIdentityProof` devolve o valor bruto apenas no resultado imediato,
  redige seu `toString` e envia somente SHA-256 ao repositório;
- a V13 amplia o lifecycle da sessão e cria `onboarding_identity_proof` com digest,
  vínculo único por sessão, usuário opaco, verificação, estado e expiração;
- `select ... for update`, insert e transição de estado executam na mesma transação;
- nenhum controller, rota, evento, chamada ao Keycloak ou operação de `Membership`
  foi introduzido.

Não foi criado evento: a prova ainda não foi entregue nem consumida e não altera
acesso. A futura concessão de `Membership` será o primeiro fato relevante para
efeitos assíncronos e projeção.

## 8. Evidência TDD e gates

O primeiro teste falhou na compilação pela ausência do lifecycle, entidade e caso
de uso esperados. Após a implementação mínima, domínio e aplicação ficaram verdes.
O teste PostgreSQL concorrente apresentou dois falsos negativos no próprio harness:
`List.of` não aceitava o resultado `null` da vencedora e `getString` representava o
boolean PostgreSQL como `t`. As asserções foram tornadas nulas e tipadas sem alterar
o comportamento de produção; a repetição então comprovou uma única prova.

Gates executados em 2026-08-02:

- focado Windows: domínio, aplicação, geração, segurança HTTP e Checkstyle verdes;
- focado Linux/WSL: corrida transacional PostgreSQL e testes relacionados verdes
  em 2m27s;
- Windows: `.\gradlew.bat clean build` verde em 1m50s, incluindo `bootJar`,
  Checkstyle, ArchUnit e JaCoCo; integrações Docker ignoradas no host como esperado;
- Linux/WSL: `./gradlew clean build` verde em 10m53s com 248 testes, zero falhas,
  erros ou skips contra PostgreSQL 17.10, Keycloak 26.7.0 e Mailpit 1.30.6 reais;
- diff sem whitespace inválido, segredo, arquivo local, log, IDE ou artefato gerado.

## 9. Continuação segura

A próxima fatia que conectar navegador, autenticação no Keycloak e emissão desta
prova deverá primeiro fixar o contrato público de início/callback e o mecanismo
interno de delegação. Essa decisão fica fora desta entrega: nenhum endpoint ou
acoplamento provisório foi criado para antecipá-la.
