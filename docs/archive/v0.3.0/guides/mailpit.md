# Guia: Mailpit para Notificações SMTP Locais

> Guia histórico da baseline `v0.3.0`. Não representa a configuração vigente.

Este guia descreve como usar o Mailpit para testar o envio real de e-mails do IdentityHub em ambiente local ou por túnel SSH.

## Objetivo

Mailpit captura e-mails enviados por SMTP e expõe uma interface web para inspeção. Ele é útil para validar o fluxo de verificação de registro sem depender de um provedor real de e-mail.

## Portas

- `1025`: porta SMTP usada pela aplicação para enviar e-mail.
- `8025`: porta HTTP usada pelo navegador para visualizar os e-mails capturados.

Essas portas têm protocolos diferentes. A interface em `8025` funcionar não garante, sozinha, que SMTP em `1025` esteja respondendo corretamente.

## Configuração IdentityHub

Para ambiente local com Mailpit:

```yaml
identity-hub:
  notification:
    email:
      enabled: true
      provider: smtp
      from: no-reply@identityhub.dev
      smtp:
        host: 127.0.0.1
        port: 1025
        username:
        password:
        auth: false
        starttls: false
        connection-timeout: 3000
        read-timeout: 3000
        write-timeout: 3000
```

Use `127.0.0.1` em vez de `localhost` quando estiver usando túnel SSH. `localhost` pode resolver para IPv6 (`::1`) ou IPv4 (`127.0.0.1`) dependendo do sistema/JVM, enquanto o túnel pode estar escutando somente em IPv4.

## Túnel SSH

Quando o Mailpit roda em outra máquina, crie um túnel expondo as duas portas localmente:

```bash
ssh -N \
  -L 127.0.0.1:1025:127.0.0.1:1025 \
  -L 127.0.0.1:8025:127.0.0.1:8025 \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  usuario@servidor
```

No Windows PowerShell, o comando pode ser escrito em uma linha:

```powershell
ssh -N -L 127.0.0.1:1025:127.0.0.1:1025 -L 127.0.0.1:8025:127.0.0.1:8025 -o ServerAliveInterval=30 -o ServerAliveCountMax=3 usuario@servidor
```

## Verificação de Conectividade

Teste a porta TCP:

```powershell
Test-NetConnection 127.0.0.1 -Port 1025
```

O resultado esperado é:

```text
TcpTestSucceeded : True
```

Teste também o protocolo SMTP:

```powershell
telnet 127.0.0.1 1025
```

O resultado esperado começa com:

```text
220 ... Mailpit ESMTP Service ready
```

Se a conexão TCP funciona, mas o banner `220` não aparece, o túnel ou o serviço SMTP está aceitando conexão sem responder corretamente como SMTP.

## Fluxo Manual Esperado

1. Inicie o IdentityHub com `identity-hub.notification.email.provider=smtp`.
2. Execute `POST /users/register`.
3. Abra `http://127.0.0.1:8025`.
4. Verifique o e-mail com o código de confirmação.
5. Execute `GET /users/confirm?username=<username>&code=<code>`.
6. Confirme que a resposta retorna usuário com status `ACTIVE`.
7. Verifique o e-mail de boas-vindas no Mailpit.

## Limitação Atual

Falhas no envio são tratadas de forma assíncrona e registradas em log. Retry/outbox não faz parte do IH-002 e deve ser implementado como feature separada.
