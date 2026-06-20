import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Processa a requisição de um único cliente em uma thread dedicada
 *
 * Cada instância recebe o pacote inicial com o comando do cliente e o socket
 * do servidor, e decide qual operação executar: LISTAR, UPLOAD ou DOWNLOAD
 */
public class ClientHandler implements Runnable {
    private DatagramPacket pacoteInicial; // Pacote com o comando recebido do cliente
    private InetAddress clienteIp; // IP do cliente
    private int clientePorta; // Porta do cliente

    // Diretório de armazenamento
    private static final String DIRETORIO_ARQUIVOS = "server/arquivos_recebidos/";

    // Socket principal do servidor, usado apenas para o envio de porta
    private DatagramSocket socketServidor;

    public ClientHandler(DatagramPacket pacoteInicial, DatagramSocket socketServidor) {
        this.pacoteInicial = pacoteInicial;
        this.socketServidor = socketServidor;
        // Armazena o endereço do cliente para envios futuros nesta thread
        this.clienteIp = pacoteInicial.getAddress();
        this.clientePorta = pacoteInicial.getPort();
    }

    // Ponto de execução da thread
    @Override
    public void run() {
        try {
            // Converte os bytes recebidos em String e remove espaços ou caracteres nulos das extremidades
            String requisicao = new String(pacoteInicial.getData(), 0, pacoteInicial.getLength()).trim();
            System.out.println("[THREAD " + Thread.currentThread().getId() + "] Comando recebido: " + requisicao);

            // Roteamento baseado no comando recebido
            if (requisicao.equalsIgnoreCase("LISTAR")) {
                enviarListaArquivos(socketServidor);
            } else if (requisicao.startsWith("UPLOAD")) {
                // Formato esperado: "UPLOAD:<nomeArquivo>"
                String nomeArquivo = requisicao.split(":", 2)[1].trim();
                receberArquivo(nomeArquivo);
            } else if (requisicao.startsWith("DOWNLOAD")) {
                // Formato esperado: "DOWNLOAD:<nomeArquivo>"
                String nomeArquivo = requisicao.split(":", 2)[1].trim();
                enviarArquivo(nomeArquivo);
            } else {
                System.out.println("Comando desconhecido.");
            }

        } catch (IOException e) {
            System.err.println("Erro no atendimento do cliente: " + e.getMessage());
        }
    }

    // Envia ao cliente a lista de arquivos disponíveis no servidor
    private void enviarListaArquivos(DatagramSocket socket) throws IOException {
        String[] arquivos = FileManager.listarArquivos();

        // Monta uma string com um nome de arquivo por linha
        StringBuilder listaFormatada = new StringBuilder();
        if (arquivos.length == 0) {
            listaFormatada.append("Nenhum arquivo disponível no servidor.");
        } else {
            for (String arquivo : arquivos) {
                listaFormatada.append(arquivo).append("\n");
            }
        }

        byte[] dadosEnviar = listaFormatada.toString().getBytes();
        DatagramPacket pacoteResposta = new DatagramPacket(
                dadosEnviar, dadosEnviar.length, clienteIp, clientePorta
        );
        socket.send(pacoteResposta);

        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Lista de arquivos enviada ao cliente.");
    }

    // Recebe um arquivo enviado pelo cliente (operação de upload)
    private void receberArquivo(String nomeArquivo) {
        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Iniciando recebimento do arquivo: " + nomeArquivo);
        try {
            // Construtor vazio faz o SO escolher uma porta livre automaticamente
            DatagramSocket socketTransferencia = new DatagramSocket();

            // Informa ao cliente em qual porta ele deve enviar os dados
            enviarPortaTransferencia(socketTransferencia.getLocalPort());
            // Aguarda e recebe os dados
            byte[] dadosCompletos = Protocol.receberBytes(socketTransferencia, clienteIp, clientePorta);
            socketTransferencia.close();
            // Persiste o arquivo no disco
            FileManager.salvarArquivo(nomeArquivo, dadosCompletos);

            System.out.println("[THREAD " + Thread.currentThread().getId()
                    + "] Upload de '" + nomeArquivo + "' concluído com sucesso.");

        } catch (IOException e) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Erro ao receber/salvar arquivo '" + nomeArquivo + "': " + e.getMessage());
        }
    }

    // Envia um arquivo ao cliente (operação de download)
    private void enviarArquivo(String nomeArquivo) {
        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Iniciando envio do arquivo: " + nomeArquivo);

        // Validação para evitar abrir o socket de transferência à toa
        if (!FileManager.arquivoExiste(nomeArquivo)) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Arquivo não encontrado: " + nomeArquivo);
            return;
        }

        try {
            DatagramSocket socketTransferencia = new DatagramSocket();

            // Cliente precisa saber em qual porta enviar/receber
            enviarPortaTransferencia(socketTransferencia.getLocalPort());
            // Lê o arquivo do disco e envia
            byte[] dados = FileManager.lerArquivo(nomeArquivo);
            Protocol.enviarBytes(socketTransferencia, clienteIp, clientePorta, dados);
            socketTransferencia.close();

            System.out.println("[THREAD " + Thread.currentThread().getId()
                    + "] Download de '" + nomeArquivo + "' finalizado.");

        } catch (IOException e) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Erro ao ler/enviar arquivo '" + nomeArquivo + "': " + e.getMessage());
        }
    }

    // Envia ao cliente a porta do socket de transferência que acabou de ser criado
    // É necessário já que UDP não tem conexão: o cliente precisa saber para qual porta direcionar os pacotes de dados
    private void enviarPortaTransferencia(int porta) throws IOException {
        String msg = "PORTA:" + porta;
        byte[] dados = msg.getBytes();

        // Reutiliza o socket principal para esta mensagem curta de controle
        socketServidor.send(new DatagramPacket(dados, dados.length, clienteIp, clientePorta));
    }
}
