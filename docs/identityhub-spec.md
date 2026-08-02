# Especificação do MVP — IdentityHub

> **Status:** aprovado
>
> **Versão do documento:** 1.0
>
> **Última atualização:** 2026-07-28

## 1. Finalidade

Este documento define o comportamento verificável do MVP do IdentityHub. Ele descreve o que o produto deve oferecer e quais resultados devem ser observáveis por usuários, aplicações consumidoras e operadores autorizados.

Detalhes de implementação, componentes internos, tecnologias, implantação e decisões arquiteturais serão definidos em documentos próprios. Ideias aceitas que não pertencem ao MVP serão registradas em `roadmap.md`.

## 2. Convenções

Os termos **deve**, **não deve** e **somente** indicam requisitos obrigatórios.

Cada capacidade possui um identificador estável no formato `IH-MVP-NNN`. Os critérios de aceitação definem exemplos mínimos; eles não substituem testes adicionais de segurança, integração ou casos de borda.

## 3. Objetivo do MVP

O MVP deve permitir que mais de um SaaS utilize uma plataforma central de identidade sem:

- implementar seu próprio fluxo de autenticação;
- conhecer ou administrar diretamente o motor interno de identidade;
- compartilhar indevidamente usuários, papéis, sessões ou configurações com outro SaaS;
- transferir ao IdentityHub regras de autorização específicas do seu domínio de negócio.

O primeiro resultado esperado é integrar com segurança um SaaS real e tornar a integração de um segundo SaaS previsível e reutilizável.

## 4. Escopo

### 4.1 Incluído no MVP

- identidade global por ambiente;
- acesso explícito e isolado por aplicação;
- cadastro com e-mail e senha;
- verificação de e-mail;
- autenticação com e-mail e senha;
- autenticação social com Google, GitHub e Facebook;
- associação segura de identidades sociais;
- recuperação e alteração de senha;
- página de autenticação hospedada e personalizada por aplicação;
- suporte a tema claro, escuro e preferência do sistema;
- aplicações SPA com Authorization Code e PKCE;
- aplicações web confidenciais e BFFs;
- APIs protegidas;
- comunicação entre sistemas com Client Credentials;
- access tokens JWT assinados assimetricamente;
- refresh tokens com rotação e detecção de reutilização;
- logout e revogação de sessões;
- papéis isolados por aplicação;
- provisionamento de acesso solicitado pela própria aplicação;
- configuração da obrigatoriedade de telefone de contato por aplicação;
- notificações transacionais essenciais por e-mail;
- configuração declarativa e diagnóstico local para integrações Java;
- auditoria de eventos relevantes de identidade e segurança;
- administração global com privilégio mínimo e MFA obrigatório;
- ambientes independentes de desenvolvimento e produção.

### 4.2 Fora do MVP

- organizações, equipes e delegação administrativa B2B;
- catálogo unificado de SaaS e planos de acesso entre produtos;
- MFA para usuários finais, passkeys e autenticação sem senha;
- login genérico com Instagram;
- conexões com redes sociais para publicação, métricas, comentários ou outras funções de negócio;
- SMS e WhatsApp como canais gerais de notificação, exceto a capacidade mínima de prova de posse exigida para telefone obrigatório;
- credenciais próprias de provedores sociais por aplicação consumidora;
- aplicativo ou SDK móvel dedicado;
- console administrativo comercial em React;
- cobrança, quotas e planos do IdentityHub como SaaS;
- domínios de autenticação personalizados por cliente;
- personalização com HTML, JavaScript ou CSS arbitrários;
- modo de desenvolvimento local completo que substitua o serviço central;
- importação ou migração automatizada de usuários de sistemas externos;
- disponibilidade em alta escala ou múltiplas regiões.

## 5. Atores

### 5.1 Usuário final

Pessoa que cria uma conta ou se autentica para acessar uma aplicação consumidora.

### 5.2 Aplicação consumidora

SaaS que delega ao IdentityHub a autenticação e o controle geral de acesso à aplicação.

### 5.3 Administrador da plataforma

Pessoa com o papel `PLATFORM_ADMIN`, responsável pela administração cotidiana do ambiente: cadastrar aplicações, configurar políticas, gerenciar acessos, revogar sessões e investigar eventos.

### 5.4 Auditor da plataforma

Pessoa com o papel `PLATFORM_AUDITOR`, autorizada a consultar configurações, eventos e informações operacionais, sem realizar alterações.

### 5.5 Administrador emergencial

Identidade `BREAK_GLASS_ADMIN`, mantida fora do uso cotidiano e destinada exclusivamente à recuperação do ambiente ou resposta a incidente grave.

### 5.6 Cliente de máquina

Aplicação não humana autorizada a obter acesso para comunicação entre sistemas.

## 6. Linguagem do domínio

- **UserAccount:** identidade global de uma pessoa dentro de um ambiente.
- **LoginIdentifier:** identificador usado para autenticação. No MVP, o identificador local é um endereço de e-mail.
- **ContactPoint:** meio de contato verificado, mantido separadamente da finalidade de autenticação.
- **ExternalIdentity:** vínculo entre um `UserAccount` e a identidade estável fornecida por um provedor externo.
- **ClientApplication:** representação lógica de um SaaS no IdentityHub.
- **ApplicationClient:** credencial ou configuração de protocolo para um canal de uma `ClientApplication`, como SPA, BFF, API ou máquina.
- **Membership:** relação explícita que permite a um `UserAccount` acessar uma `ClientApplication`.
- **Role:** papel geral atribuído dentro dos limites de uma `ClientApplication`.
- **AuthSession:** sessão autenticada de uma pessoa, com início, expiração e revogação.
- **TokenFamily:** sequência de refresh tokens originada por uma autenticação e invalidada em conjunto quando houver logout ou reutilização indevida.
- **Branding:** configuração visual segura associada a uma `ClientApplication`.

Os nomes em inglês estabelecem a linguagem ubíqua do produto e poderão ser usados nos contratos técnicos. Os textos explicativos e a experiência do usuário podem ser localizados.

## 7. Invariantes do MVP

1. Um `UserAccount` é global somente dentro de um ambiente.
2. A existência de um `UserAccount` não concede acesso automático a nenhuma `ClientApplication`.
3. Todo acesso humano a recursos protegidos de uma aplicação exige uma `Membership` ativa para essa aplicação.
4. Papéis e concessões de uma aplicação não podem ser usados em outra.
5. Credenciais, sessões e tokens nunca podem ser transferidos entre ambientes.
6. Uma identidade externa é reconhecida pela combinação estável de emissor e identificador do sujeito, nunca apenas pelo e-mail informado pelo provedor.
7. E-mails coincidentes não autorizam associação automática entre contas.
8. Um usuário pendente de verificações obrigatórias não pode receber token que autorize recursos protegidos da aplicação que exige essas verificações.
9. Telefone de contato não é identificador de login no MVP.
10. Exigir telefone não representa consentimento para mensagens de marketing, SMS ou WhatsApp.
11. Tokens emitidos para uma aplicação não devem carregar papéis de outra aplicação.
12. Regras sobre propriedade de recursos, assinatura, plano ou limites de uso permanecem na aplicação consumidora.
13. Uma aplicação consumidora nunca deve receber, armazenar ou reenviar a senha ou outra credencial humana do IdentityHub.
14. Autenticar uma identidade não cria `Membership`; a concessão de acesso deve partir da aplicação consumidora ou de um `PLATFORM_ADMIN`.
15. Uma aplicação consumidora não pode atribuir privilégios de plataforma.
16. Privilégios administrativos pertencem a um único ambiente e não se propagam entre desenvolvimento e produção.
17. Nenhum perfil administrativo pode visualizar senhas, códigos utilizáveis, tokens completos ou valores de segredos protegidos.

## 8. Capacidades funcionais

### IH-MVP-001 — Cadastrar uma aplicação consumidora

Um `PLATFORM_ADMIN` deve poder cadastrar uma `ClientApplication` com identificador único, nome de exibição, estado e configurações iniciais.

A aplicação deve começar isolada de todas as demais. Seus clientes, URIs, origens, papéis, políticas e branding devem pertencer somente a ela.

#### Critérios de aceitação

- Dado um identificador ainda não utilizado, quando o operador cadastrar a aplicação, então ela ficará disponível para configuração.
- Dado um identificador já utilizado no mesmo ambiente, quando houver nova tentativa de cadastro, então a operação será rejeitada sem alterar a aplicação existente.
- Dado um operador sem autorização, quando ele tentar cadastrar ou alterar uma aplicação, então o acesso será negado e auditado.
- Nenhuma credencial secreta deve ser retornada novamente depois do momento seguro definido para sua emissão.

### IH-MVP-002 — Configurar canais de uma aplicação

Um `PLATFORM_ADMIN` deve poder associar a uma `ClientApplication` um ou mais `ApplicationClients` dos tipos:

- SPA pública;
- aplicação web confidencial ou BFF;
- API protegida;
- cliente de máquina.

Cada cliente deve declarar apenas os dados compatíveis com seu tipo.

A aplicação também deve declarar quais métodos de autenticação disponibiliza dentre senha, Google, GitHub e Facebook.

#### Critérios de aceitação

- Uma SPA não deve depender de segredo armazenado no navegador.
- Uma aplicação confidencial ou cliente de máquina deve ser autenticado antes de receber tokens.
- URIs de redirecionamento devem ser comparadas exatamente com valores previamente autorizados.
- Origens web devem ser permitidas explicitamente; curingas inseguros devem ser rejeitados.
- Um cliente desabilitado não deve iniciar novos fluxos nem obter novos tokens.
- Um método de autenticação desabilitado não deve aparecer nem poder ser iniciado para a aplicação.

### IH-MVP-003 — Cadastrar usuário com e-mail e senha

Quando o cadastro autônomo estiver habilitado para uma aplicação, um visitante deve poder iniciar o cadastro informando e-mail, senha e os dados obrigatórios definidos pela política da aplicação.

O e-mail deve ser tratado de forma normalizada para comparação, sem alterar o valor apropriado para comunicação. A senha deve ser validada segundo a política de segurança vigente e nunca deve ser exposta após o recebimento.

O cadastro deve criar ou reutilizar com segurança a identidade global. Ele não deve conceder `Membership` automaticamente.

#### Critérios de aceitação

- Um cadastro válido deve produzir resposta genérica de sucesso e iniciar a verificação de e-mail.
- Um cadastro com senha fora da política deve ser rejeitado com orientação suficiente para correção, sem registrar a senha.
- Uma solicitação para e-mail já existente deve produzir resposta que não revele se a conta existe.
- O cadastro não deve conceder acesso automático à aplicação de origem nem a outra aplicação.
- Se o cadastro autônomo estiver desabilitado, a tentativa deve ser recusada.
- Nenhum token capaz de autorizar recursos protegidos deve ser emitido sem `Membership` ativa.

### IH-MVP-004 — Verificar e-mail

O usuário deve poder comprovar a posse do e-mail por meio de um código ou link temporário, de uso único e associado à finalidade de verificação.

#### Critérios de aceitação

- Uma verificação válida e dentro do prazo deve marcar o e-mail como verificado.
- Um código expirado, já utilizado, inválido ou destinado a outra finalidade deve ser rejeitado.
- A resposta de falha não deve revelar dados adicionais da conta.
- O reenvio deve ser limitado e deve invalidar ou tornar ineficaz o código anterior conforme a política de segurança.
- A conclusão da verificação deve ativar a `Membership` somente se nenhuma outra exigência da aplicação estiver pendente.

### IH-MVP-005 — Aplicar política de telefone de contato

Cada `ClientApplication` deve configurar a exigência de telefone como `DISABLED`, `OPTIONAL` ou `REQUIRED`.

Quando uma pessoa já possuir telefone verificado em seu `UserAccount`, a aplicação poderá considerar o requisito satisfeito sem solicitar novamente o mesmo dado. O compartilhamento do telefone com a aplicação consumidora deve depender de finalidade e permissão apropriadas.

Quando `REQUIRED` estiver habilitado, deve existir um meio transacional mínimo, fornecido por provedor oficial, para comprovar a posse do telefone.

#### Critérios de aceitação

- Com `DISABLED`, o fluxo de cadastro não deve solicitar telefone.
- Com `OPTIONAL`, o usuário deve poder concluir o acesso sem informar telefone.
- Com `REQUIRED`, a `Membership` não deve se tornar ativa até que um telefone válido seja verificado.
- Exigir telefone não deve registrar consentimento para marketing nem habilitar automaticamente SMS ou WhatsApp.
- A prova de posse do telefone não deve autorizar comunicações posteriores por esse canal.
- Uma aplicação sem permissão para receber o contato não deve obter o número em tokens ou respostas de perfil.

### IH-MVP-006 — Autenticar com e-mail e senha

Um usuário deve poder autenticar-se com e-mail e senha. Se possuir `Membership` ativa, poderá continuar o fluxo de acesso da aplicação. Sem `Membership`, a identidade autenticada poderá ser usada pelo backend ou BFF da aplicação somente para correlacionar uma aquisição mantida server-side; o resultado não autoriza APIs de negócio.

#### Critérios de aceitação

- Credenciais válidas, conta habilitada e `Membership` ativa devem permitir a continuidade do fluxo de acesso.
- Credenciais válidas sem `Membership` ativa não devem produzir autorização para APIs de negócio.
- Credenciais inválidas, conta inexistente e conta desabilitada devem produzir resposta pública genérica.
- Tentativas repetidas devem sofrer limitação e proteção contra força bruta.
- Uma autenticação bem-sucedida ou falha deve gerar registro de auditoria sem armazenar a senha.
- Autenticar-se em uma aplicação não deve conceder acesso a outra.
- Autenticar-se sem acesso não deve criar `Membership`.
- O backend ou BFF deve obter o `sub` opaco somente de um resultado OIDC validado.

### IH-MVP-007 — Autenticar com provedor social

Uma aplicação deve poder habilitar individualmente Google, GitHub e Facebook dentre os provedores disponibilizados no ambiente.

O usuário deve ser redirecionado ao provedor escolhido e retornar ao fluxo da aplicação de origem. O IdentityHub deve identificar a conta externa pela combinação estável de emissor e sujeito.

#### Critérios de aceitação

- Um provedor desabilitado para a aplicação não deve aparecer nem iniciar autenticação.
- Uma identidade externa já vinculada deve resolver sempre para o mesmo `UserAccount`.
- Uma identidade externa nova deve poder criar conta quando o cadastro estiver habilitado e as informações obrigatórias forem concluídas, sem criar `Membership` automaticamente.
- Se o provedor não fornecer um e-mail confiável necessário ao MVP, o usuário deve concluir e verificar um e-mail antes da ativação.
- Um e-mail igual ao de uma conta existente não deve provocar vínculo automático.
- Para vincular uma nova identidade externa a uma conta existente, o usuário deve comprovar controle da conta existente em um fluxo autenticado.
- Cancelamento, erro ou recusa do provedor deve retornar uma mensagem segura e permitir nova tentativa.
- Segredos dos provedores não devem ser expostos à aplicação consumidora nem ao navegador.
- Sem `Membership` ativa, a autenticação social não deve autorizar APIs de negócio.

### IH-MVP-008 — Hospedar a experiência de autenticação

O IdentityHub deve hospedar as páginas de cadastro, login, verificação, recuperação e demais etapas essenciais de identidade.

A aplicação consumidora deve direcionar o usuário ao fluxo hospedado e não deve precisar implementar formulários que capturem a senha local do IdentityHub.

#### Critérios de aceitação

- O fluxo deve identificar a aplicação de origem de maneira não manipulável.
- Links, formulários e retornos devem permanecer limitados a destinos previamente autorizados.
- As páginas devem funcionar com teclado, possuir rótulos compreensíveis, foco visível e contraste adequado.
- Informações sensíveis não devem aparecer em URLs, logs do navegador ou mensagens públicas.
- A experiência deve oferecer tema claro, escuro e preferência do sistema.

### IH-MVP-009 — Personalizar a experiência por aplicação

Um `PLATFORM_ADMIN` deve poder configurar nome de exibição, logotipo e opções visuais suportadas para cada `ClientApplication`.

O conjunto de personalizações deve ser restrito a valores e artefatos seguros. Código executável e estilos arbitrários não fazem parte do contrato.

#### Critérios de aceitação

- O fluxo iniciado por uma aplicação deve usar somente o branding efetivo daquela aplicação.
- Na ausência de branding próprio, deve ser utilizada a identidade visual padrão do IdentityHub.
- Um artefato inválido, acima dos limites ou de tipo não permitido deve ser rejeitado.
- Conteúdo fornecido pela aplicação não deve permitir execução de script, injeção de marcação ou acesso a recursos internos.
- A falha ao carregar um artefato personalizado deve preservar um fluxo utilizável com fallback seguro.

### IH-MVP-010 — Autorizar aplicações de usuário

SPAs, aplicações web e BFFs devem autenticar usuários por Authorization Code. Clientes públicos devem utilizar PKCE.

#### Critérios de aceitação

- O código de autorização deve ser temporário, de uso único e vinculado ao cliente e à URI de redirecionamento.
- Uma SPA deve utilizar PKCE e não deve obter tokens com segredo de cliente.
- Um verificador PKCE ausente ou incorreto deve impedir a troca do código.
- Um código reutilizado, expirado ou emitido para outro cliente deve ser rejeitado.
- O fluxo Implicit e o fluxo baseado no envio direto de usuário e senha pela aplicação consumidora não devem estar disponíveis.
- Sem `Membership` ativa, o resultado do fluxo não deve autorizar recursos protegidos da aplicação.

### IH-MVP-011 — Autorizar comunicação entre sistemas

Um cliente de máquina autorizado deve poder obter token por Client Credentials para os escopos permitidos ao próprio cliente.

#### Critérios de aceitação

- Credenciais válidas devem produzir token limitado ao cliente, público e escopos autorizados.
- Credenciais inválidas ou cliente desabilitado devem ser rejeitados sem indicar qual parte da credencial falhou.
- Tokens de máquina não devem representar um `UserAccount` nem herdar papéis de usuário.
- Client Credentials não deve produzir refresh token.
- Um cliente autorizado a provisionar acesso deve atuar somente sobre sua própria `ClientApplication`.

### IH-MVP-012 — Emitir access token

O IdentityHub deve emitir access tokens JWT de curta duração, assinados assimetricamente e verificáveis sem compartilhamento da chave privada.

O contrato mínimo para tokens de usuário deve permitir validar:

- emissor;
- sujeito estável e opaco;
- público pretendido;
- cliente autorizado;
- instante de emissão;
- expiração;
- identificador do token;
- escopos concedidos;
- papéis pertencentes à aplicação de destino.

Dados pessoais não devem ser incluídos por padrão.

#### Critérios de aceitação

- Uma API deve poder obter metadados públicos e chaves de verificação pelo emissor configurado.
- Token expirado, malformado, alterado, com assinatura inválida, emissor incorreto ou público incompatível deve ser rejeitado.
- Um token destinado à aplicação A não deve autorizar a aplicação B.
- Papéis da aplicação A não devem aparecer no token destinado à aplicação B.
- A rotação de chave deve permitir transição segura durante a validade dos tokens já emitidos.

### IH-MVP-013 — Renovar sessão com refresh token

Clientes autorizados devem poder renovar uma sessão de usuário sem solicitar novamente suas credenciais, respeitando os limites da sessão e da aplicação.

Cada uso bem-sucedido deve rotacionar o refresh token. Um token substituído não deve voltar a ser aceito.

#### Critérios de aceitação

- Um refresh token válido deve produzir novos tokens e invalidar sua própria reutilização.
- Refresh token expirado, revogado, pertencente a outro cliente ou sessão deve ser rejeitado.
- A reutilização de um refresh token substituído deve revogar sua `TokenFamily` e exigir nova autenticação.
- Uma conta, `Membership` ou cliente desabilitado não deve renovar tokens.
- A aplicação consumidora não deve armazenar refresh token em local deliberadamente acessível a scripts quando existir alternativa segura para seu tipo.

### IH-MVP-014 — Encerrar sessão e revogar renovação

O usuário deve poder encerrar sua sessão. Um `PLATFORM_ADMIN` deve poder revogar sessões quando necessário para responder a risco ou mudança de acesso.

#### Critérios de aceitação

- O logout deve invalidar a sessão e impedir novas renovações da `TokenFamily` afetada.
- Revogar uma sessão deve impedir o uso de seus refresh tokens.
- Remover acesso ou desabilitar uma conta deve impedir novas autenticações e renovações.
- O usuário deve ser redirecionado após o logout somente para destino previamente autorizado.
- A documentação e a integração devem deixar claro que um access token JWT já emitido pode continuar válido até sua expiração.

### IH-MVP-015 — Recuperar e alterar senha

O usuário deve poder solicitar recuperação de senha e definir nova senha por prova temporária enviada ao e-mail verificado. Um usuário autenticado deve poder alterar a própria senha após comprovação adequada.

#### Critérios de aceitação

- A solicitação de recuperação deve retornar resposta indistinguível para contas existentes e inexistentes.
- A prova de recuperação deve ser temporária, de uso único e limitada à finalidade.
- Prova inválida, expirada ou reutilizada deve ser rejeitada.
- A nova senha deve cumprir a política vigente e não deve ser registrada em logs ou auditoria.
- Após a redefinição, as sessões e famílias de refresh token abrangidas pela política de segurança devem ser revogadas.
- A alteração deve gerar evento de auditoria e notificação de segurança ao e-mail verificado.

### IH-MVP-016 — Gerenciar acesso e papéis por aplicação

Uma aplicação consumidora deve poder conceder, suspender e remover `Memberships` da própria aplicação por meio de um cliente de máquina autorizado. Um `PLATFORM_ADMIN` também deve poder executar essas operações, além de atribuir e remover papéis definidos para a aplicação.

O backend ou BFF da aplicação deve manter a referência de aquisição em sessão server-side e correlacioná-la ao `sub` opaco obtido por Authorization Code com PKCE. Após confirmar pagamento ou outra regra própria de aquisição, deverá solicitar a concessão usando esse identificador e suas próprias credenciais de máquina.

O IdentityHub deve fornecer apenas autorização geral de acesso e papéis da aplicação. Pagamento, plano, assinatura, preço e decisões sobre recursos do negócio permanecem fora do IdentityHub.

#### Critérios de aceitação

- Uma solicitação válida da aplicação deve conceder acesso somente à própria aplicação.
- A solicitação deve usar o identificador opaco do usuário; credenciais humanas não devem ser aceitas.
- O identificador do usuário não deve ser aceito do navegador como autoridade; deve ser derivado de um resultado OIDC validado pelo backend ou BFF.
- A mesma solicitação de provisionamento repetida com a mesma chave de idempotência não deve criar `Membership` nem atribuições duplicadas.
- Falha temporária após a confirmação comercial deve permitir retentativa e reconciliação pela aplicação.
- Suspender ou remover uma `Membership` deve impedir novos tokens e renovações para aquela aplicação.
- Atribuir um papel deve exigir que o papel pertença à mesma aplicação da `Membership`.
- Uma tentativa de atribuição entre aplicações deve ser rejeitada e auditada.
- A remoção de um papel deve refletir nos tokens emitidos posteriormente.
- Alterações de acesso não devem criar nem modificar pagamento, plano, assinatura ou outros dados de domínio mantidos pelo SaaS consumidor.
- Após a concessão, o usuário deve obter um novo token ou renovar o fluxo para que o acesso atualizado seja refletido.

### IH-MVP-017 — Desabilitar conta global

Um `PLATFORM_ADMIN` deve poder desabilitar um `UserAccount` quando houver motivo operacional ou de segurança.

#### Critérios de aceitação

- Uma conta desabilitada não deve iniciar novas sessões nem renovar sessões existentes em qualquer aplicação do ambiente.
- A desabilitação deve revogar as sessões abrangidas pela política de segurança.
- A operação deve ser auditada com ator, motivo, instante e identificador da conta.
- A conta não deve ser excluída silenciosamente como efeito da desabilitação.

### IH-MVP-018 — Entregar notificações essenciais por e-mail

O MVP deve enviar por e-mail:

- verificação de endereço;
- recuperação de senha;
- confirmação de alteração de senha.

A indisponibilidade temporária do serviço de e-mail não deve apagar a solicitação válida nem exigir que o usuário repita imediatamente toda a operação.

#### Critérios de aceitação

- Uma notificação aceita deve possuir identificador rastreável e finalidade explícita.
- Falha temporária deve permitir novas tentativas controladas.
- O reprocessamento da mesma solicitação não deve gerar efeitos de segurança duplicados.
- Falha permanente deve ficar disponível para diagnóstico por `PLATFORM_ADMIN` e `PLATFORM_AUDITOR`.
- Senhas, tokens completos e códigos utilizáveis não devem aparecer em logs.
- O destinatário e o conteúdo devem corresponder ao ambiente e à aplicação de origem.

### IH-MVP-019 — Auditar eventos de identidade e segurança

O IdentityHub deve manter trilha de auditoria para, no mínimo:

- cadastro iniciado e concluído;
- verificação concluída ou rejeitada;
- autenticação bem-sucedida e falha;
- vínculo de identidade externa;
- recuperação e alteração de senha;
- emissão, renovação e revogação de sessão;
- reutilização de refresh token;
- mudanças de `Membership` e papéis;
- solicitações de provisionamento aceitas, rejeitadas e repetidas;
- mudanças de configuração de aplicação;
- operações administrativas negadas.

#### Critérios de aceitação

- Cada registro deve conter tipo do evento, instante, resultado, ator quando conhecido, aplicação e identificador de correlação.
- Dados sensíveis e segredos não devem ser armazenados no evento.
- `PLATFORM_ADMIN` e `PLATFORM_AUDITOR` devem conseguir correlacionar eventos de uma mesma operação.
- Uma aplicação ou perfil de plataforma não deve consultar eventos fora do seu escopo autorizado.
- Instantes devem ser registrados de forma não ambígua e comparável entre ambientes.

### IH-MVP-020 — Integrar uma API Java

Uma API Java deve poder adotar o Integration Mode para validar tokens emitidos pelo IdentityHub e aplicar regras de acesso gerais sem depender de contratos específicos do motor interno.

#### Critérios de aceitação

- A integração deve validar assinatura, emissor, público e expiração.
- Requisição sem autenticação válida a recurso protegido deve ser rejeitada.
- Token válido sem papel ou escopo exigido deve resultar em acesso negado.
- Falha ao obter configuração ou chaves sem cache válido deve causar rejeição segura, não liberação de acesso.
- Atualizações compatíveis do motor interno não devem exigir alteração no código consumidor.
- A aplicação deve continuar responsável por suas autorizações contextuais de domínio.

### IH-MVP-021 — Configurar e diagnosticar o Integration Mode

O projeto consumidor deve poder declarar configuração não secreta e versionável para sua integração. Uma ferramenta local deve validar essa configuração, comparar o estado desejado com o estado efetivo e permitir aplicação explícita por `PLATFORM_ADMIN`.

#### Critérios de aceitação

- A configuração deve identificar aplicação, emissor, tipo de cliente, URIs, origens, métodos de autenticação, política de telefone e branding.
- Segredos não devem ser armazenados no arquivo declarativo.
- A ferramenta deve apontar campos inválidos e configurações inseguras antes da aplicação.
- A comparação deve indicar ausência de alteração ou diferenças entre estado desejado e efetivo.
- A aplicação da configuração deve ser explícita, autenticada e repetível sem criar recursos duplicados.
- O início da aplicação consumidora não deve reconfigurar silenciosamente o ambiente de produção.
- A interface local deve permanecer desabilitada em produção por padrão.
- Um logotipo presente nos recursos do consumidor deve ser enviado ao serviço; o serviço não deve tentar acessar diretamente o sistema de arquivos ou classpath remoto.

### IH-MVP-022 — Separar ambientes

Desenvolvimento e produção devem operar como ambientes independentes, com emissores, identidades, aplicações, credenciais, chaves, sessões e dados próprios.

#### Critérios de aceitação

- Uma credencial ou token de desenvolvimento não deve ser aceito em produção.
- Uma conta criada em desenvolvimento não deve existir automaticamente em produção.
- Configurações aplicadas em desenvolvimento não devem alterar produção.
- URLs e metadados de cada ambiente devem identificar inequivocamente seu emissor.

### IH-MVP-023 — Administrar a plataforma

O IdentityHub deve oferecer administração global do ambiente sem depender do console administrativo do motor interno.

O papel `PLATFORM_ADMIN` deve permitir as operações cotidianas previstas nesta especificação. O papel `PLATFORM_AUDITOR` deve oferecer consulta sem mutação. O `BREAK_GLASS_ADMIN` deve permanecer separado dos papéis cotidianos e ser utilizado somente em recuperação ou incidente grave.

#### Critérios de aceitação

- Toda conta administrativa cotidiana deve utilizar MFA por TOTP compatível com aplicativos padronizados, incluindo Google Authenticator.
- O primeiro acesso administrativo não deve prosseguir sem a configuração do segundo fator e dos mecanismos de recuperação definidos.
- Códigos de recuperação devem ser de uso único, protegidos contra consulta posterior e regeneráveis somente mediante autenticação reforçada.
- Autenticação administrativa deve utilizar cliente e público distintos dos tokens destinados às aplicações consumidoras.
- Operações especialmente sensíveis devem exigir nova autenticação ou elevação recente de confiança.
- `PLATFORM_ADMIN` deve poder gerenciar aplicações, configurações, contas, `Memberships`, papéis, sessões, falhas operacionais e auditoria pelos contratos do IdentityHub.
- `PLATFORM_AUDITOR` deve poder consultar somente os dados autorizados e não deve executar mutações.
- Nenhuma aplicação consumidora ou cliente de máquina deve conceder, remover ou herdar papéis de plataforma.
- O sistema deve impedir a remoção ou desativação que deixe o ambiente sem administrador cotidiano recuperável.
- Toda autenticação, consulta sensível e mutação administrativa deve ser auditada.
- Uma credencial administrativa de desenvolvimento não deve conceder acesso administrativo em produção.
- A interface administrativa e o acesso emergencial não devem estar publicamente expostos.
- O `BREAK_GLASS_ADMIN` não deve ser utilizado para operações rotineiras e todo uso deve gerar evidência destacada de segurança.
- A administração normal não deve exigir acesso direto ao banco de dados nem ao console do motor interno.

## 9. Requisitos transversais verificáveis

### 9.1 Segurança

- Senhas devem ser armazenadas somente por derivação resistente e configurada conforme a política vigente.
- Chaves privadas, segredos de cliente, credenciais de provedores e provas temporárias devem permanecer protegidos e nunca ser retornados em APIs comuns.
- Operações administrativas devem exigir autenticação forte, autorização de privilégio mínimo e auditoria.
- MFA administrativo deve ser obrigatório mesmo enquanto MFA de usuários finais permanecer fora do MVP.
- Interfaces administrativas internas não devem estar publicamente acessíveis.
- Endpoints sensíveis devem possuir limitação de uso e proteção contra automação abusiva.
- Redirecionamentos, origens e públicos devem usar listas explícitas.
- Respostas devem minimizar enumeração de contas, clientes e credenciais.
- A validação deve falhar de forma segura quando dependências críticas ou configurações confiáveis estiverem indisponíveis.

### 9.2 Privacidade

- Tokens e respostas devem expor somente dados necessários ao escopo autorizado.
- Finalidades de autenticação, segurança, contato operacional e marketing devem permanecer distintas.
- Nenhuma aplicação deve obter dados de outra aplicação por inferência, consulta ou token.
- Dados de contato compartilhados com uma aplicação devem depender de escopo e finalidade apropriados.

### 9.3 Confiabilidade

- Operações repetidas por timeout ou nova tentativa não devem criar identidades, vínculos, aplicações ou concessões duplicadas.
- Falhas parciais entre capacidades internas devem ser detectáveis e passíveis de reconciliação por `PLATFORM_ADMIN`.
- Horários, identificadores de correlação e resultados devem permitir diagnóstico de fluxos distribuídos.
- Retentativas devem ser limitadas e não devem produzir ciclos infinitos.

### 9.4 Compatibilidade

- Contratos públicos do IdentityHub devem possuir estratégia explícita de evolução compatível.
- Aplicações consumidoras não devem depender de nomes de classes, endpoints administrativos ou formatos privados do motor interno.
- Claims públicos devem ser documentados e alterados de forma controlada.

### 9.5 Testabilidade

- Relógio, aleatoriedade e integrações externas devem permitir testes determinísticos nos limites apropriados.
- Os fluxos críticos devem possuir testes unitários, de integração, de contrato e ponta a ponta proporcionais ao risco.
- Integrações reais com banco de dados, motor de identidade e e-mail devem ser exercitadas em ambiente descartável.
- Provedores sociais devem ser simulados nos testes automatizados; testes reais devem ocorrer somente em ambientes autorizados.
- Uma versão candidata não deve ser liberada com vulnerabilidades conhecidas classificadas como críticas ou altas.

### 9.6 Observabilidade operacional

- Fluxos críticos de autenticação, emissão, renovação, revogação e entrega de e-mail devem expor métricas de sucesso, falha e duração sem dados pessoais.
- Requisições e processamentos decorrentes devem propagar identificador de correlação ou contexto de rastreamento.
- O serviço deve informar separadamente se está em execução e se possui condições de atender tráfego.
- Sinais operacionais não devem expor segredos, tokens, códigos ou dados pessoais.
- Uma dependência externa indisponível deve produzir sinal diagnosticável e comportamento limitado, sem espera indefinida.

## 10. Cenários de aceitação do MVP

O MVP estará funcionalmente concluído quando os seguintes percursos forem demonstrados em ambiente equivalente ao de produção:

1. Um operador cadastra duas aplicações isoladas, cada uma com branding e políticas próprias.
2. Um usuário se cadastra com e-mail e senha durante a aquisição da primeira aplicação, verifica os dados exigidos e autentica-se por Authorization Code com PKCE sem obter acesso de negócio.
3. O backend ou BFF da primeira aplicação valida o resultado OIDC, correlaciona o `sub` opaco à aquisição server-side e, após confirmar sua regra comercial, provisiona a `Membership` usando suas próprias credenciais e uma chave de idempotência.
4. Antes do provisionamento, o usuário não acessa recursos protegidos; depois dele, um novo token concede acesso somente à primeira aplicação.
5. O mesmo usuário adquire a segunda aplicação e recebe acesso somente após o provisionamento correspondente feito por ela.
6. Um usuário autentica-se por Google, GitHub e Facebook nos provedores habilitados.
7. Uma tentativa de associação social baseada apenas em e-mail coincidente é impedida.
8. Uma SPA conclui Authorization Code com PKCE e chama uma API com token destinado ao público correto.
9. Um BFF confidencial conclui Authorization Code e mantém tokens fora do alcance direto do navegador.
10. Um cliente de máquina obtém token próprio sem representar usuário.
11. Uma API rejeita tokens expirados, alterados, de emissor incorreto, público incorreto e com papel insuficiente.
12. Um refresh token é rotacionado; sua reutilização revoga a família correspondente.
13. Logout impede renovação, respeitando a validade residual documentada do access token.
14. Recuperação de senha não revela se uma conta existe e revoga as sessões definidas pela política.
15. A indisponibilidade temporária de e-mail produz retentativa rastreável sem duplicar a operação de identidade.
16. A ferramenta local detecta configuração inválida, mostra diferenças e aplica configuração autorizada de forma idempotente.
17. Tokens, identidades e configurações de desenvolvimento são rejeitados em produção.
18. Os testes de segurança e o pentest planejado não deixam vulnerabilidades críticas ou altas conhecidas abertas para a liberação.
19. Um `PLATFORM_ADMIN` autenticado com TOTP administra o ambiente sem acessar o motor interno; um `PLATFORM_AUDITOR` consulta a auditoria sem conseguir alterá-la.

## 11. Matriz de responsabilidade

| Responsabilidade | IdentityHub | Aplicação consumidora |
|---|---:|---:|
| Identidade global e credenciais | Sim | Não |
| Login local e social | Sim | Não |
| Sessões, tokens e logout | Sim | Valida e inicia fluxos |
| Acesso geral à aplicação | Sim | Consome a decisão |
| Papéis gerais da aplicação | Sim | Interpreta e aplica |
| Decisão de conceder ou remover acesso após aquisição | Materializa | Decide e solicita |
| Administração global do ambiente | Sim | Não |
| Propriedade de recursos de negócio | Não | Sim |
| Limites de plano, assinatura e cobrança | Não | Sim |
| Branding do fluxo de identidade | Armazena e renderiza | Declara |
| Consentimento e uso de dados no negócio | Não | Sim |
| Segurança da própria API e frontend | Parcialmente auxilia | Sim |

## 12. Rastreabilidade documental

- `product-vision.md`: propósito, público e proposta de valor.
- `architecture.md`: componentes, módulos, limites e fluxos internos.
- `security-model.md`: ameaças, protocolos, parâmetros de tokens e políticas de segurança.
- `integration-mode.md`: starter Java, configuração e console local.
- `roadmap.md`: capacidades futuras aceitas.
- `adr/`: decisões arquiteturais e suas consequências.

Esta especificação define o comportamento aprovado do MVP, mas não autoriza inferir decisões técnicas que ainda não tenham sido registradas nos documentos correspondentes.
