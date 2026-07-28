# ADR-0005: Modular monolith no plano de controle

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

O plano de controle reúne aplicações, identidade orquestrada, memberships,
notificações, auditoria e integração com Keycloak. O MVP será mantido por equipe
pequena e implantado inicialmente em infraestrutura compartilhada.

Microsserviços aumentariam deploys, contratos distribuídos, latência, observabilidade
e falhas parciais antes de existir carga ou organização que os justifique.

## Decisão

`identityhub-service` será um modular monolith Spring Boot organizado por capacidade.

Módulos iniciais:

- `clientapplication`;
- `identity`;
- `access`;
- `notification`;
- `administration`;
- `audit`;
- `sharedkernel` mínimo, quando inevitável.

Regras:

- domínio não depende de Spring, HTTP, JPA ou Keycloak;
- módulos não acessam repositórios internos uns dos outros;
- comunicação síncrona ocorre por contratos de aplicação;
- fatos importantes podem usar eventos internos;
- transações pertencem ao caso de uso;
- controllers são finos;
- ciclos entre módulos são proibidos;
- cada aggregate protege suas invariantes.

## Consequências positivas

- uma unidade simples de build e deploy;
- transações locais quando apropriadas;
- menor custo de operação;
- limites de domínio explícitos;
- caminho de extração futura baseado em módulos coesos.

## Consequências negativas

- disciplina arquitetural precisa de testes automatizados;
- erro de processo pode afetar várias capacidades;
- escala independente não existe inicialmente;
- shared kernel mal utilizado pode recriar acoplamento.

## Alternativas consideradas

### Microsserviços desde o início

Rejeitados pela ausência de escala, equipes e ciclos independentes.

### Aplicação em camadas técnicas globais

Rejeitada porque espalha uma capacidade entre packages genéricos e favorece serviços
procedurais.

### Monólito sem módulos explícitos

Rejeitado porque dificulta linguagem, propriedade e futura extração.

## Validação

- ArchUnit ou mecanismo equivalente impede dependências proibidas;
- testes verificam ausência de ciclos;
- domínio compila sem Spring;
- cada fatia vertical identifica módulo proprietário;
- extração somente ocorre pelos gates do roadmap.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Roadmap](../roadmap.md)
