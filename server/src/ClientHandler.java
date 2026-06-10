package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientHandler implements Runnable {
    private DatagramPacket pacoteInicial;
    private InetAddress clienteIp;
    private int clientePorta;
    private static final String DIRETORIO_ARQUIVOS = "server/arquivos_recebidos/";
    private DatagramSocket socketServidor;

    public ClientHandler(DatagramPacket pacoteInicial, DatagramSocket socketServidor) {
        this.pacoteInicial = pacoteInicial;
        this.socketServidor = socketServidor;
        this.clienteIp = pacoteInicial.getAddress();
        this.clientePorta = pacoteInicial.getPort();
    }

    @Override
    public void run() {
        try {

            String requisicao = new String(pacoteInicial.getData(), 0, pacoteInicial.getLength()).trim();
            System.out.println("[THREAD " + Thread.currentThread().getId() + "] Comando recebido: " + requisicao);

            if (requisicao.equalsIgnoreCase("LISTAR")) {
                enviarListaArquivos(socketServidor);
            } else if (requisicao.startsWith("UPLOAD")) {
                String nomeArquivo = requisicao.split(":", 2)[1].trim();
                receberArquivo(nomeArquivo);
            } else if (requisicao.startsWith("DOWNLOAD")) {
                String nomeArquivo = requisicao.split(":", 2)[1].trim();
                enviarArquivo(nomeArquivo);
            } else {
                System.out.println("Comando desconhecido.");
            }

        } catch (IOException e) {
            System.err.println("Erro no atendimento do cliente: " + e.getMessage());
        }
    }

    private void enviarListaArquivos(DatagramSocket socket) throws IOException {
        String[] arquivos = FileManager.listarArquivos();

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

    private void receberArquivo(String nomeArquivo) {
        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Iniciando recebimento do arquivo: " + nomeArquivo);
        try {
            DatagramSocket socketTransferencia = new DatagramSocket();
            enviarPortaTransferencia(socketTransferencia.getLocalPort());
            byte[] dadosCompletos = Protocol.receberBytes(socketTransferencia, clienteIp, clientePorta);
            socketTransferencia.close();
            FileManager.salvarArquivo(nomeArquivo, dadosCompletos);

            System.out.println("[THREAD " + Thread.currentThread().getId()
                    + "] Upload de '" + nomeArquivo + "' concluído com sucesso.");

        } catch (IOException e) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Erro ao receber/salvar arquivo '" + nomeArquivo + "': " + e.getMessage());
        }
    }

    private void enviarArquivo(String nomeArquivo) {
        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Iniciando envio do arquivo: " + nomeArquivo);

        if (!FileManager.arquivoExiste(nomeArquivo)) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Arquivo não encontrado: " + nomeArquivo);
            return;
        }

        try {
            DatagramSocket socketTransferencia = new DatagramSocket();
            enviarPortaTransferencia(socketTransferencia.getLocalPort());
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
    private void enviarPortaTransferencia(int porta) throws IOException {
        String msg = "PORTA:" + porta;
        byte[] dados = msg.getBytes();
        socketServidor.send(new DatagramPacket(dados, dados.length, clienteIp, clientePorta));
    }
}
