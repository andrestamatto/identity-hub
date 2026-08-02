# SLICE-005C — Aquisição correlacionada por OIDC padrão

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-02
>
> **Branch:** `refactor/slice-005c-standard-oidc-acquisition`

## 1. Resultado observável

O contrato público do IdentityHub usa apenas Authorization Code com PKCE para
identificar uma pessoa durante a aquisição de um SaaS. O backend ou BFF consumidor
mantém o contexto comercial e provisiona `Membership` depois de validar o resultado
OIDC e concluir sua decisão de negócio.

## 2. Critérios de aceitação

- não existem endpoint, scope ou tipos de runtime para `OnboardingSession` ou
  `OnboardingIdentityProof`;
- configuração de cliente de máquina volta ao contrato mínimo, sem scope de
  onboarding;
- documentação exige callback backend/BFF, OIDC validado e correlação server-side;
- autenticação sem `Membership` não autoriza APIs de negócio;
- migrations Flyway V11–V13 permanecem imutáveis e suas estruturas ficam inativas;
- harness local e testes não dependem do protocolo removido;
- builds canônicos Windows e Linux/WSL ficam verdes.

## 3. Limites

Esta fatia não implementa a API de provisionamento de `Membership`, o login
hosted nem o callback de um SaaS consumidor. Ela reduz a fronteira antes dessas
capacidades e não altera dados existentes.

## 4. Segurança e rollback

A remoção diminui a superfície de ataque e não relaxa controles OIDC. O rollback
de código é o reaproveitamento dos commits das SLICE-005A/005B; migrations não
precisam ser reaplicadas porque foram preservadas. A retirada física das estruturas
obsoletas está registrada como PD-003 e não bloqueia o runtime.

## 5. Evidências

- build Windows `./gradlew.bat clean build --console=plain`: verde, incluindo
  compilação, testes, Checkstyle, JaCoCo e `bootJar`;
- build Linux/WSL `./gradlew clean build --console=plain`: verde com 221 testes,
  zero falhas e zero ignorados; confirmação incremental `./gradlew build`
  retornou `BUILD SUCCESSFUL`;
- busca na árvore ativa: nenhum tipo, endpoint ou scope proprietário de onboarding;
- migrations V11–V13 comparadas com `develop`: sem alteração;
- ADR-0013 preservada como histórico e marcada `Superseded` pela ADR-0014;
- documentos normativos, ADRs, roadmap, estratégia, guias e assessments vigentes
  relidos integralmente antes da publicação.

## 6. Alinhamento com a sequência aprovada

Esta correção não autoriza iniciar `Membership`. A sequência da seção 10 de
`migration-strategy.md` ainda possui trabalho anterior:

1. concluir identidade local com `IH-MVP-006` (login por e-mail e senha);
2. implementar recuperação e lifecycle (`IH-MVP-015` e `IH-MVP-017`);
3. somente depois iniciar aquisição e acesso (`IH-MVP-010` e `IH-MVP-016`).

Qualquer antecipação futura exige uma das justificativas expressas na seção
10.2 e atualização documental material; interesse técnico isolado não é motivo.
