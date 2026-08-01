# Entrega autônoma controlada

> **Status:** approved
>
> **Mandato concedido em:** 2026-07-31
>
> **Escopo:** implementação incremental do MVP do IdentityHub

## 1. Finalidade

Este documento autoriza o agente a implementar, documentar, publicar e mesclar
fatias do IdentityHub sem aprovação humana individual, desde que preserve as
fontes de verdade, os limites de segurança e todos os gates deste documento.

O mandato é revogável a qualquer momento pelo mantenedor. Autonomia substitui a
espera por aprovação, não substitui especificação, teste, revisão ou evidência.

## 2. Ciclo autorizado

Para cada fatia, o agente pode:

1. sincronizar `develop` e criar uma branch exclusiva;
2. definir resultado observável, critérios, limites, riscos e rollback;
3. implementar com TDD e atualizar a documentação aplicável;
4. executar testes focados e os gates canônicos;
5. revisar segurança, arquitetura, credenciais e higiene do diff;
6. criar commits Conventional Commits por contexto;
7. fazer push e abrir PR para `develop`;
8. acompanhar checks e corrigir falhas reproduzíveis;
9. mesclar o PR quando todos os gates estiverem satisfeitos;
10. sincronizar `develop` e iniciar a próxima fatia do roadmap.

Um PR contém uma única fatia ou ajuste preparatório coerente. Dependências entre
fatias permanecem sequenciais.

## 3. Gates obrigatórios de merge

Um PR autônomo só pode ser mesclado quando:

- critérios de aceitação estiverem rastreados e demonstrados;
- testes tenham apresentado vermelho pela razão esperada antes do verde;
- build Windows e build Linux/WSL estiverem verdes quando aplicáveis;
- integrações reais necessárias tiverem sido executadas;
- Checkstyle, JaCoCo, ArchUnit e `bootJar` aplicáveis estiverem verdes;
- checks obrigatórios do GitHub estiverem concluídos com sucesso;
- não houver thread de revisão ou decisão bloqueante aberta;
- migration, segurança, observabilidade e rollback tiverem sido avaliados;
- diff não contiver secret, ambiente local, log, IDE ou artefato gerado;
- documentação e changelog estiverem coerentes;
- pendências adiáveis estiverem registradas.

Check cancelado, ausente ou em andamento não é verde. O agente não reduz um gate
para obter merge.

## 4. Decisões adiáveis

Uma decisão pode ser adiada somente quando houver alternativa temporária segura,
reversível e interna, sem alterar contrato público ou conceder acesso adicional.

Ela deve ser registrada em `pending-decisions.md` com contexto, impacto, escolha
temporária e condição de resolução. O desenvolvimento pode prosseguir nas partes
independentes.

O registro é sanitizado: não contém credenciais, dados pessoais nem instruções
exploráveis de vulnerabilidade.

## 5. Condições de parada

O agente interrompe a implementação e solicita o mantenedor diante de:

- divergência entre fontes normativas;
- ambiguidade que altere contrato público, regra de negócio ou postura de
  segurança;
- decisão difícil de reverter sem alternativa aprovada;
- migration destrutiva ou risco de perda de dados;
- necessidade de contratar serviço, aceitar custo ou criar conta externa;
- necessidade de credencial real indisponível;
- vulnerabilidade crítica sem correção segura e verificável no escopo;
- falha obrigatória de CI que não possa ser reproduzida ou explicada;
- proteção do repositório que exija ação humana;
- operação fora dos limites da seção 6.

Uma condição bloqueante não é escondida como pendência adiável.

## 6. Operações não autorizadas

O mandato não autoriza:

- deploy ou alteração de produção;
- compra, assinatura ou aumento de custo;
- exclusão ou limpeza de banco de dados;
- rotação, revogação ou exposição de credencial real;
- redução de autenticação, autorização, auditoria ou isolamento;
- bypass de check, proteção de branch ou hook;
- force push ou reescrita destrutiva de histórico;
- edição de outro repositório, inclusive Auto Radar;
- comunicação externa em nome do mantenedor além dos PRs deste repositório.

## 7. Acompanhamento

Cada PR registra resumo, critérios, testes, riscos, rollback e pendências. O
agente fornece atualizações periódicas durante uma execução longa. O mantenedor
pode acompanhar sem que isso suspenda o mandato e pode intervir ou revogá-lo a
qualquer momento.
