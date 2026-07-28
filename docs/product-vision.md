# Visão do Produto — IdentityHub

> **Status:** aprovado
>
> **Versão do documento:** 1.0
>
> **Última atualização:** 2026-07-28

## 1. Propósito

O IdentityHub existe para retirar dos produtos SaaS a responsabilidade de construir e operar repetidamente os mesmos mecanismos críticos de identidade, autenticação e controle de acesso.

Seu propósito é oferecer um ponto único, seguro e simples de integrar para gerenciar identidades de usuários e o acesso inicial a diferentes aplicações, permitindo que cada produto consumidor concentre seu código e suas decisões no próprio domínio de negócio.

## 2. Contexto

Cada novo SaaS precisa resolver problemas semelhantes: cadastro, login, recuperação de conta, login social, sessões, tokens, proteção de APIs e gerenciamento básico de acesso. Implementar essas capacidades separadamente em cada produto:

- aumenta o tempo necessário para lançar novos produtos;
- replica código e decisões sensíveis de segurança;
- produz experiências inconsistentes para usuários;
- amplia o custo de manutenção e atualização;
- dificulta uma futura experiência integrada entre produtos.

O IdentityHub nasce para atender inicialmente um portfólio próprio de SaaS e Micro-SaaS. Essa utilização real será a base para amadurecer o produto antes de uma eventual oferta comercial a terceiros.

## 3. Visão

Ser a plataforma de identidade comum para um ecossistema de SaaS, capaz de acelerar o lançamento de novos produtos sem comprometer segurança, isolamento ou autonomia.

No futuro, o IdentityHub poderá ser oferecido como SaaS para desenvolvedores e pequenas equipes que desejem integrar autenticação e autorização sem operar diretamente a complexidade de um provedor de identidade.

## 4. Proposta de valor

### Declaração principal

O IdentityHub oferece autenticação e gerenciamento de acesso seguros e prontos para integração, com uma experiência consistente e personalizável para cada SaaS, ocultando a complexidade operacional do motor de identidade.

### Valor entregue

- **Mais velocidade:** reduz o trabalho necessário para adicionar identidade e proteção de APIs a um novo produto.
- **Segurança por padrão:** concentra práticas, atualizações e controles de segurança em uma solução especializada.
- **Integração previsível:** oferece contratos estáveis para aplicações Java, frontends web e, progressivamente, outros tipos de cliente.
- **Autonomia dos produtos:** mantém no SaaS consumidor as regras de autorização próprias do seu domínio.
- **Experiência personalizável:** permite que cada aplicação apresente sua marca sem precisar construir todo o fluxo de identidade.
- **Evolução compartilhada:** uma melhoria no IdentityHub pode beneficiar todos os produtos integrados.

## 5. Público

### Público inicial

O primeiro usuário operacional é o próprio mantenedor do IdentityHub, integrando-o a um portfólio de SaaS e Micro-SaaS com:

- APIs desenvolvidas em Java;
- frontends que podem utilizar tecnologias diferentes;
- aplicações web, backends e integrações entre sistemas;
- necessidade de lançamento rápido e baixa dependência de suporte humano.

### Público futuro

Desenvolvedores independentes e pequenas equipes de software que:

- constroem um ou mais produtos SaaS;
- precisam de autenticação moderna sem desenvolver um servidor de autorização próprio;
- valorizam configuração simples, segurança e baixo custo operacional;
- desejam manter as regras de negócio e a autorização contextual dentro de seus produtos;
- preferem não conhecer nem integrar diretamente o motor de identidade subjacente.

### Usuário final indireto

A pessoa que utiliza um SaaS integrado ao IdentityHub. Para ela, a autenticação deve parecer parte natural do produto acessado, com identidade visual coerente, fluxos compreensíveis e proteção adequada de sua conta.

### Fora do foco inicial

- grandes empresas que exijam implantação corporativa dedicada, suporte especializado ou governança avançada;
- cenários B2B complexos com organizações, hierarquias e delegação administrativa;
- produtos que queiram transferir ao IdentityHub suas regras de autorização específicas de negócio;
- clientes que precisem customizar livremente o código executado nas páginas de autenticação.

Esses limites definem o foco do MVP, não impedimentos permanentes do produto.

## 6. Problema central

O problema central não é apenas implementar uma tela de login. É integrar e operar corretamente um conjunto de responsabilidades sensíveis — identidade, credenciais, provedores externos, sessões, tokens, recuperação e acesso às aplicações — sem repetir essa complexidade em cada SaaS.

Alternativas existentes podem ser robustas, mas frequentemente expõem diretamente sua configuração, seus conceitos e sua operação ao projeto consumidor. O IdentityHub busca oferecer uma camada de produto mais simples e orientada ao ecossistema de SaaS, utilizando um motor de identidade consolidado sem transferir seu acoplamento aos consumidores.

## 7. Princípios do produto

### Segurança sem configuração heroica

Os caminhos recomendados devem ser seguros por padrão. Uma integração simples não deve depender de o consumidor conhecer todos os detalhes de OAuth, OpenID Connect ou do motor interno.

### Simplicidade proporcional

O produto deve resolver integralmente o necessário para o MVP sem antecipar estruturas que ainda não possuem uso real. Extensibilidade não justifica abstrações, classes ou infraestrutura sem necessidade presente.

### Motor interno transparente

O motor de identidade é um detalhe interno do IdentityHub. Projetos consumidores integram-se exclusivamente por contratos, configurações e ferramentas do IdentityHub.

### Identidade global, acesso isolado

Uma pessoa pode possuir uma identidade global no ambiente, mas seu acesso a cada aplicação é explícito e isolado. Existir no IdentityHub não concede automaticamente acesso a todos os SaaS.

### Autonomia do domínio consumidor

O IdentityHub responde por autenticação e controles gerais de acesso à aplicação. Cada SaaS continua responsável por decisões como propriedade de recursos, limites de plano e permissões ligadas ao seu negócio.

### Experiência coerente com cada produto

Os fluxos de identidade devem ser centralizados, mas capazes de refletir marca, tema e políticas de cadastro da aplicação consumidora dentro de limites seguros.

### Evolução baseada em uso real

O IdentityHub será validado primeiro pelos produtos do próprio ecossistema. Recursos futuros devem responder a necessidades comprovadas, riscos concretos ou requisitos para sua comercialização.

## 8. Direção do MVP

O MVP deverá:

- operar principalmente como um serviço central de identidade;
- oferecer uma integração leve para aplicações Java;
- suportar aplicações web, APIs e comunicação entre sistemas;
- fornecer cadastro e autenticação por credenciais e provedores sociais priorizados;
- emitir e validar tokens usando padrões modernos;
- isolar aplicações, papéis e concessões de acesso;
- oferecer fluxos de autenticação hospedados e personalizáveis;
- permitir configuração declarativa e diagnóstico local da integração;
- manter rastreabilidade e fundamentos operacionais compatíveis com uso em produção;
- ocultar dos consumidores a administração e os contratos específicos do motor interno.

Os comportamentos, critérios de aceitação e limites exatos do MVP serão definidos em `identityhub-spec.md`.

## 9. Objetivos de produto

### Curto prazo

- viabilizar com segurança o lançamento do primeiro SaaS consumidor;
- reduzir o esforço de integração dos SaaS seguintes;
- substituir a baseline `v0.3.0` por uma fundação menor, coesa e orientada aos novos limites;
- comprovar os fluxos essenciais em uma infraestrutura de custo compatível com o estágio do produto.

### Médio e longo prazo

- tornar o IdentityHub a camada comum de identidade do catálogo de SaaS;
- permitir uma experiência integrada entre produtos quando houver necessidade real;
- amadurecer operação, administração e isolamento para atender clientes externos;
- avaliar sua oferta comercial como SaaS sem exigir mudanças nos contratos dos consumidores existentes.

## 10. Indicadores de sucesso

No estágio inicial, o sucesso será evidenciado quando:

- o primeiro SaaS utilizar o IdentityHub em produção sem integração direta com o motor interno;
- um novo SaaS puder ser integrado reutilizando o mesmo modelo e ferramentas, sem duplicar a implementação de autenticação;
- cada aplicação mantiver identidade visual e políticas próprias dentro dos limites suportados;
- falhas e eventos relevantes de identidade puderem ser diagnosticados sem acesso direto ao banco de dados;
- a evolução do IdentityHub não exigir que consumidores conheçam detalhes específicos do motor interno;
- os controles de segurança definidos para o MVP forem cobertos por testes e por uma avaliação de segurança antes do lançamento.

Metas quantitativas de disponibilidade, desempenho, tempo de integração e adoção serão definidas após a primeira implantação real produzir uma linha de base confiável.

## 11. Restrições e premissas atuais

- O produto deve iniciar com baixo custo operacional e aproveitar a infraestrutura já disponível.
- O MVP será mantido por uma equipe muito pequena e deve exigir pouca operação manual.
- A primeira integração prioriza o ecossistema Java, sem tornar o protocolo dependente de Java.
- O IdentityHub poderá se tornar um SaaS comercial, mas modelo de negócio, preços e níveis de serviço ainda não foram definidos.
- Suporte B2B avançado, catálogo unificado e integrações sociais de negócio pertencem à evolução futura, não ao núcleo inicial.

## 12. Promessa orientadora

> Integrar identidade a um novo SaaS deve ser previsível, seguro e significativamente mais simples do que construir e operar essa capacidade dentro do próprio produto.
