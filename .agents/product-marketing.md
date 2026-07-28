# Product Marketing Context — IdentityHub

> **Document version:** v1
>
> **Last updated:** 2026-07-28
>
> **Status:** draft

## Product Overview

### O que é

O IdentityHub é uma camada central de identidade para um portfólio de SaaS e
Micro-SaaS. Ele retira de cada produto a necessidade de construir e operar
repetidamente cadastro, autenticação, sessões, tokens e acesso geral à aplicação.

O produto combina:

- **Service Mode:** serviço central que orquestra identidade, aplicações,
  memberships, branding e operação;
- **Integration Mode:** integração leve para aplicações Java e Spring Boot,
  incluindo validação segura de tokens, cliente tipado opcional e console local de
  configuração;
- **experiência hospedada:** jornadas de cadastro e login que refletem a marca de
  cada aplicação dentro de limites seguros;
- **motor interno transparente:** Keycloak executa os protocolos e mantém
  credenciais e sessões, sem contrato direto com os produtos consumidores.

Uma identidade é global dentro de cada ambiente, mas o acesso é concedido
explicitamente por `Membership` e isolado por aplicação. Regras de pagamento,
planos, propriedade de recursos e demais autorizações de negócio permanecem no
SaaS consumidor.

### Estágio atual

O IdentityHub está em redefinição documental antes de uma refatoração profunda. O
primeiro objetivo é viabilizar com segurança um SaaS real do próprio portfólio.
Uma oferta comercial a terceiros é uma direção futura aceita, não um produto
disponível nem uma promessa de prazo.

### Promessa orientadora

> Integrar identidade a um novo SaaS deve ser previsível, seguro e
> significativamente mais simples do que construir e operar essa capacidade
> dentro do próprio produto.

## Target Audience

### Público inicial validável

O primeiro usuário operacional é o próprio mantenedor, que desenvolve um portfólio
de SaaS e Micro-SaaS com:

- APIs predominantemente em Java;
- frontends web ou mobile com tecnologias variadas;
- equipe e capacidade operacional pequenas;
- necessidade de lançar produtos com rapidez;
- preferência por baixo custo e pouca intervenção humana;
- necessidade de reutilizar identidade sem compartilhar indevidamente o acesso
  entre aplicações.

### Público comercial futuro — hipótese

Desenvolvedores independentes e pequenas equipes que:

- constroem um ou mais produtos SaaS;
- querem autenticação moderna sem criar um authorization server;
- valorizam segurança por padrão e integração previsível;
- preferem não operar nem expor diretamente os conceitos de um motor como
  Keycloak;
- precisam preservar no próprio produto as regras de autorização do negócio.

Essa definição ainda não constitui um ICP validado. Segmento prioritário,
disposição a pagar, processo de compra e requisitos mínimos de suporte exigem
pesquisa antes da comercialização.

### Usuário final indireto

A pessoa que acessa um SaaS integrado. Para ela, o fluxo de identidade deve parecer
parte natural do produto, com marca coerente, linguagem compreensível e proteção
adequada da conta.

### Fora do foco inicial

- grandes empresas com implantação dedicada, procurement ou governança avançada;
- B2B complexo com organizações, hierarquias e administração delegada;
- produtos que pretendam terceirizar suas regras contextuais de negócio;
- customização arbitrária de HTML, JavaScript ou CSS nas páginas de autenticação;
- substituição imediata de plataformas comerciais de identidade em todos os
  segmentos.

Esses limites protegem o foco do MVP e não representam exclusões permanentes.

## Personas

As personas abaixo são hipóteses de trabalho baseadas no contexto interno. Não
foram validadas por entrevistas.

### Desenvolvedor-mantenedor de múltiplos SaaS

- **Contexto:** cria e opera produtos com equipe muito pequena.
- **Objetivo:** lançar o próximo SaaS sem reconstruir autenticação.
- **Dores:** código duplicado, decisões sensíveis de segurança, operação dispersa e
  pouco tempo para suporte.
- **Critério de sucesso:** integrar uma nova aplicação com menos esforço que a
  anterior, sem depender de detalhes do motor interno.
- **Receio principal:** trocar desenvolvimento próprio por outra plataforma
  complexa de operar.

### Pequena equipe de produto — futura

- **Contexto:** possui aplicações Java e frontends diversos, mas não uma equipe
  especializada em identidade.
- **Objetivo:** obter fluxos modernos e proteção de APIs sem desviar o time do
  domínio do produto.
- **Dores:** curva de OAuth/OIDC, configuração insegura, manutenção e dependência
  de fornecedor.
- **Critério de sucesso:** contratos estáveis, diagnóstico claro e autonomia das
  regras de negócio.
- **Receio principal:** maturidade, confiabilidade, suporte e custo de migração.

### Usuário de um SaaS integrado

- **Contexto:** quer acessar o produto adquirido, não aprender sobre o IdentityHub.
- **Objetivo:** cadastrar-se, autenticar-se e recuperar a conta com segurança.
- **Dores:** fluxos confusos, aparência desconectada, solicitações excessivas de
  dados e falhas sem explicação.
- **Critério de sucesso:** experiência familiar, rápida e coerente com o SaaS.
- **Receio principal:** não reconhecer ou não confiar na página de autenticação.

## Problems & Pain Points

### Problema funcional

Cada novo SaaS volta a enfrentar cadastro, verificação, login social, recuperação,
sessões, emissão e validação de tokens, logout, auditoria e controle inicial de
acesso.

### Problema econômico e operacional

- repetir a solução atrasa lançamentos;
- cada implementação amplia manutenção e superfície de incidentes;
- equipes pequenas não conseguem operar muitas variações com segurança;
- uma correção ou melhoria precisa ser replicada entre produtos;
- integração direta com um motor de identidade transfere sua complexidade a cada
  consumidor.

### Problema de experiência

Centralizar identidade pode produzir uma experiência genérica ou desconectada da
marca. Customização irrestrita, por outro lado, cria riscos de segurança e
operação. O produto precisa equilibrar coerência visual e limites seguros.

### Tensão principal

Os compradores futuros podem querer simultaneamente simplicidade, controle,
segurança, personalização e baixo custo. O IdentityHub não deve esconder os
trade-offs: ele oferece autonomia nas regras do SaaS e contratos estáveis, mas
delimita a customização e centraliza responsabilidades de identidade.

## Competitive Landscape

Este panorama é preliminar. Não substitui pesquisa competitiva atualizada nem
validação com clientes.

### Plataformas de identidade gerenciadas

Exemplos a avaliar incluem Auth0, Clerk, Supabase Auth e ofertas gerenciadas
baseadas em Keycloak ou ZITADEL. São alternativas que um potencial cliente pode
considerar ao buscar menor esforço operacional.

Questões ainda a pesquisar:

- experiência real de integração para pequenas equipes Java;
- custos nos padrões de uso relevantes;
- limites de branding e portabilidade;
- suporte a portfólios de múltiplos SaaS;
- custo operacional total e dependência da plataforma.

### Motores operados diretamente

Keycloak e ZITADEL podem ser adotados diretamente. Essa escolha oferece controle,
mas exige que cada equipe compreenda, configure, atualize e opere o motor e seus
conceitos.

O IdentityHub não compete por ter inventado um novo motor. Sua tese é que existe
valor em transformar um motor consolidado numa experiência de produto mais
previsível para o ecossistema consumidor.

### Construção própria em cada SaaS

É a alternativa indireta mais próxima do problema original. Pode parecer rápida no
primeiro fluxo, mas replica decisões de segurança, código e operação entre
produtos.

### Status da diferenciação

Não há, neste estágio, evidência suficiente para afirmar superioridade de preço,
segurança, velocidade ou funcionalidade sobre essas alternativas. Comparações
públicas dependerão de pesquisa e medições reproduzíveis.

## Differentiation

As características abaixo são diferenciais pretendidos e hipóteses de
posicionamento, ainda não vantagens de mercado comprovadas:

- **orientação a um portfólio de SaaS:** identidade compartilhável quando desejado,
  com membership e papéis isolados por aplicação;
- **motor encapsulado:** consumidores dependem dos contratos do IdentityHub, não
  de claims ou APIs privadas do Keycloak;
- **integração Java/Spring focada:** starter, configuração declarativa, diagnóstico
  e console local no fluxo do desenvolvedor;
- **autonomia do produto consumidor:** autenticação e acesso geral são
  centralizados, enquanto autorização contextual continua no SaaS;
- **experiência hospedada com marca:** personalização segura por aplicação sem
  execução de código arbitrário;
- **evolução compartilhada:** uma melhoria central pode beneficiar todas as
  aplicações integradas.

### Posicionamento provisório

Para desenvolvedores independentes e pequenas equipes que constroem SaaS, o
IdentityHub pretende ser a camada de identidade que oferece integração previsível,
segurança por padrão e isolamento entre aplicações, sem exigir operação direta do
motor de identidade nem apropriar-se das regras de negócio do produto.

## Objections

### “Por que não usar Keycloak diretamente?”

Essa pode ser a escolha correta para equipes capazes de assumir sua modelagem,
configuração, upgrades e operação. O valor pretendido do IdentityHub está em
encapsular esse acoplamento, oferecer contratos voltados aos SaaS e reutilizar uma
operação comum entre aplicações.

### “Por que não usar uma plataforma gerenciada já madura?”

Essa objeção é legítima. A resposta futura dependerá de evidências sobre aderência
ao portfólio, experiência Java, controle operacional, custo total e portabilidade.
O produto ainda não deve alegar vantagem sem essa validação.

### “Uma solução nova de identidade é segura o bastante?”

O IdentityHub usa protocolos e um motor consolidados, mas isso não elimina riscos.
A resposta precisa ser demonstrada por testes automatizados, threat model,
scanning, avaliação ASVS, teste dinâmico e pentest independente antes de oferta
externa.

### “Ficarei preso ao IdentityHub?”

Os contratos são projetados para não expor detalhes privados do Keycloak e usam
padrões como OAuth 2.0, OpenID Connect e JWT. Ainda assim, migração nunca é custo
zero; documentação de exportação, versionamento e saída será necessária antes da
comercialização.

### “Conseguirei reproduzir exatamente o design do meu produto?”

Não no MVP. A experiência permite branding declarativo seguro, temas e assets
aprovados, mas não aceita código arbitrário. A limitação é deliberada e deve ser
comunicada antes da integração.

### “Isso também resolve permissões e planos do meu SaaS?”

Não. O IdentityHub autentica, protege o acesso geral e fornece papéis isolados por
aplicação. Propriedade de recursos, assinatura, limites de plano e decisões
contextuais pertencem ao SaaS.

## Switching Dynamics

Hipóteses baseadas no framework de forças de mudança; exigem validação em pesquisa.

### Push — o que afasta da situação atual

- repetição de autenticação em cada produto;
- risco de configurações inconsistentes;
- manutenção e atualização distribuídas;
- demora para lançar novos SaaS.

### Pull — o que atrai para o IdentityHub

- integração reutilizável;
- segurança e operação concentradas;
- identidade global com acesso explícito;
- experiência de login coerente com a aplicação;
- menor exposição dos conceitos do motor.

### Habit — o que mantém a solução atual

- código de autenticação já conhecido;
- templates internos aparentemente suficientes;
- controle direto sobre cada implementação;
- receio de adicionar dependência central.

### Anxiety — o que dificulta a mudança

- maturidade e disponibilidade do IdentityHub;
- migração de identidades existentes;
- impacto de uma indisponibilidade central;
- segurança, privacidade e conformidade;
- preço, suporte e continuidade ainda indefinidos.

## Customer Language

Ainda não existe pesquisa de voz do cliente. As expressões abaixo são linguagem
interna do fundador e devem orientar clareza, não ser apresentadas como depoimentos:

- “fácil de configurar e plugar”;
- “o mais simples, mas o mais completo possível para o MVP”;
- “complicar o que não precisa ser complicado”;
- “hub único de gerenciamento de autenticação e autorização”.

### Vocabulário preferido

- identidade;
- integração previsível;
- segurança por padrão;
- contratos estáveis;
- isolamento por aplicação;
- motor interno transparente;
- autonomia do produto;
- experiência de autenticação hospedada;
- configuração declarativa.

### Termos e promessas a evitar

- “segurança absoluta”;
- “zero configuração”;
- “infalível”;
- “enterprise-ready”;
- “totalmente customizável”;
- “integra-se a qualquer coisa instantaneamente”;
- “SSO universal”;
- qualquer redução de custo ou tempo sem medição;
- comparação de superioridade sem fonte verificável.

## Brand Voice

### Personalidade

- direta e calma;
- tecnicamente confiável, mas compreensível;
- pragmática e sem entusiasmo artificial;
- transparente sobre limites e trade-offs;
- cuidadosa com afirmações de segurança;
- orientada a ajudar o desenvolvedor a avançar.

### Como escrever

- começar pelo resultado que o usuário alcança;
- explicar protocolos apenas quando ajudam uma decisão;
- diferenciar claramente recurso atual, compromisso e possibilidade futura;
- usar exemplos concretos de integração;
- preferir precisão a superlativos;
- reconhecer quando uma alternativa concorrente pode ser adequada.

### Como não escrever

- usar medo como argumento de venda;
- sugerir que segurança pode ser delegada sem responsabilidade;
- esconder dependências ou limitações;
- transformar detalhes internos de arquitetura na proposta principal;
- apresentar roadmap como compromisso de prazo.

## Proof Points

### Evidência disponível

- visão, especificação comportamental, arquitetura, modelo de segurança,
  Integration Mode, roadmap e ADRs foram documentados e aprovados;
- limites entre IdentityHub, Keycloak e produtos consumidores estão definidos;
- critérios de aceitação e gates de segurança estão especificados.

Esses itens são evidência de preparo interno, não prova de adoção, confiabilidade em
produção ou valor de mercado.

### Evidência ainda necessária

- primeiro SaaS operando em produção sem integração direta com Keycloak;
- segundo SaaS integrado pelos mesmos contratos com menor esforço;
- linha de base mensurável de tempo e intervenções na integração;
- isolamento entre aplicações demonstrado;
- resultados de testes de segurança e pentest independente;
- restauração, atualização e resposta a falhas exercitadas;
- entrevistas com potenciais clientes externos;
- disposição a pagar e modelo de suporte validados;
- depoimentos, estudos de caso e métricas somente após uso real autorizado.

### Alegações não autorizadas no estágio atual

Não existem ainda clientes externos, depoimentos, métricas públicas, SLA, preço
validado, certificações ou resultados de pentest. Nenhum material deve insinuar que
esses elementos já existem.

## Goals

### Objetivo atual

Lançar com segurança o primeiro SaaS consumidor e demonstrar que ele utiliza o
IdentityHub sem acoplamento direto ao motor interno.

### Próximos resultados

1. comprovar que uma segunda integração reutiliza os mesmos contratos;
2. medir esforço, falhas e intervenções manuais;
3. consolidar o IdentityHub como camada de identidade do portfólio próprio;
4. pesquisar demanda externa antes de definir oferta comercial;
5. avaliar comercialização somente após operação, segurança, custo por tenant,
   suporte e requisitos legais estarem compreendidos.

### Ação desejada por estágio

- **agora:** adoção e validação interna pelo primeiro SaaS;
- **após prova de reutilização:** entrevistas e descoberta com desenvolvedores e
  pequenas equipes;
- **antes de oferta comercial:** validação de ICP, concorrência, preço, suporte e
  riscos legais;
- **futuro, se aprovado:** demonstração, lista de interesse ou onboarding
  self-service, conforme evidência de demanda.

### Métricas

Ainda não há baseline quantitativa confiável. Tempo de integração, intervenções
manuais, disponibilidade, incidentes, custo por aplicação e adoção deverão ser
medidos no uso real antes de estabelecer metas ou claims públicos.

## Changelog

- v1 (2026-07-28) — Initial context.
