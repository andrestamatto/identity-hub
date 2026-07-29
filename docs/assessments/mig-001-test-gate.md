# MIG-001 — Evidência do gate de testes

> **Status:** approved
>
> **Data:** 2026-07-29
>
> **Branch:** `fix/mig-001-test-discovery`

## 1. Objetivo

Restaurar a confiança no comando canônico de testes antes da substituição da
fundação abandonada.

## 2. Diagnóstico

A baseline registrou 22 suites e 78 testes, além de oito classes aparentemente
omitidas. A investigação demonstrou que não havia omissão na descoberta do JUnit.

O Gradle executava as 30 classes e os 91 testes. No Windows, oito relatórios XML
receberam nomes encurtados iniciados por `__`. A medição original usava
`TEST-*.xml` e, por isso, não os contabilizou.

Não houve mudança no source set de produção ou de testes entre o commit avaliado
na baseline e a reprodução do MIG-001.

## 3. Correção

O comando canônico permanece:

```powershell
.\gradlew.bat clean test
```

A task `test` agora:

- informa no console a quantidade total executada;
- exige exatamente 91 testes para a baseline legada;
- falha se a contagem mudar sem atualização intencional do gate.

O número deve ser revisto quando o MIG-003 substituir a fundação e remover os
testes legados.

Para auditar os relatórios sem depender do nome físico:

```powershell
$reports = Get-ChildItem build/test-results/test -File -Filter *.xml
$tests = ($reports | ForEach-Object {
    [int]([xml](Get-Content $_.FullName)).testsuite.tests
} | Measure-Object -Sum).Sum
```

## 4. Evidências observadas

| Verificação | Resultado |
|---|---|
| `clean test` | verde após restauração das provas negativas |
| Contagem exibida pelo gate | 91 |
| Relatórios XML | 30 |
| Classe de prova | `DefaultWhatsappDeliveryTest`, 3 testes |
| Asserção controlada incorreta | comando vermelho com 91 executados e 1 falha |
| Baseline controlada de 92 | comando vermelho com 91 executados |
| Execução direcionada com `--tests` | verde com 2 testes, sem falso negativo do gate |
| Código de produção alterado | nenhum |

## 5. Limites

- o gate protege a quantidade da baseline, não mede cobertura;
- a correção não atualiza Gradle, JUnit, Spring Boot ou outras dependências;
- nenhuma qualidade funcional dos testes legados é inferida apenas pelo resultado
  verde;
- o gate não substitui os controles de CI e análise previstos nos incrementos
  seguintes.
