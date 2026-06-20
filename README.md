# File Transfer UDP

Aplicação cliente/servidor de transferência de arquivos via **UDP**, escrita em Java puro (sem dependências externas). 
O servidor aceita múltiplos clientes simultâneos e suporta três operações: listar arquivos, enviar (upload) e baixar (download).

## Como executar

### 1. Iniciar o servidor

Execute a partir da **raiz do projeto** (importante: o servidor salva os arquivos em `server/arquivos_recebidos/`, um caminho relativo):

```bash
java -cp out/server ServerMain
```

Saída esperada:
```
Inicializando servidor UDP na porta 9876...
Servidor pronto e aguardando requisições...
```

Deixe esse terminal aberto — o servidor ficará escutando indefinidamente.

### 2. Iniciar o cliente

Em **outro terminal** (pode ser na mesma máquina ou em outra, desde que a rede permita UDP na porta 9876), também a partir da raiz do projeto:

```bash
java -cp out/client ClientMain
```

Será exibido um menu interativo:

```
===== CLIENTE UDP =====
1 - Listar arquivos
2 - Enviar arquivo
3 - Download
4 - Sair
Escolha:
```

- **1 – Listar arquivos**: mostra os arquivos disponíveis no servidor.
- **2 – Enviar arquivo**: solicita o caminho de um arquivo local e faz o upload para o servidor (salvo em `server/arquivos_recebidos/`).
- **3 – Download**: solicita o nome de um arquivo presente no servidor e o baixa, salvando localmente como `download_<nome_do_arquivo>` no diretório onde o cliente foi executado.
- **4 – Sair**: encerra o cliente.

### Conectando a um servidor remoto

Por padrão o cliente se conecta a `localhost:9876` (constantes `HOST` e `PORTA` em `ClientService.java`). Para conectar a um servidor em outra máquina, edite o valor de `HOST` nesse arquivo antes de compilar, ou ajuste conforme necessário.

### Autores
- [Graciele Miki Sazaka](https://github.com/mikisazaka)
- [Giovana Nunes Constancio](https://github.com/giconstancio)
- [João Paulo Simplicio](https://github.com/simplicioJoao)
- [Felipy Ogido](https://github.com/FelipyOgido)