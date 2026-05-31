import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientHandler implements Runnable {
    private DatagramPacket pacoteInicial;
    private InetAddress clienteIp;
    private int clientePorta;
    private static final String DIRETORIO_ARQUIVOS = "server/arquivos_recebidos/";

    public ClientHandler(DatagramPacket pacoteInicial) {
        this.pacoteInicial = pacoteInicial;
        this.clienteIp = pacoteInicial.getAddress();
        this.clientePorta = pacoteInicial.getPort();
    }

    @Override
    public void run() {
        try (DatagramSocket socketDedicado = new DatagramSocket()) {

            String requisicao = new String(pacoteInicial.getData(), 0, pacoteInicial.getLength()).trim();
            System.out.println("[THREAD " + Thread.currentThread().getId() + "] Comando recebido: " + requisicao);

            if (requisicao.equalsIgnoreCase("LISTAR")) {
                enviarListaArquivos(socketDedicado);
            } else if (requisicao.startsWith("UPLOAD")) {
                String nomeArquivo = requisicao.split(":", 2)[1].trim();
                receberArquivo(socketDedicado, nomeArquivo);
            } else if (requisicao.startsWith("DOWNLOAD")) {
                String nomeArquivo = requisicao.split(":", 2)[1].trim();
                enviarArquivo(socketDedicado, nomeArquivo);
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

    private void receberArquivo(DatagramSocket socket, String nomeArquivo) {
        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Iniciando recebimento do arquivo: " + nomeArquivo);
        try {
            byte[] dadosCompletos = Protocol.receberBytes(socket, clienteIp, clientePorta);
            FileManager.salvarArquivo(nomeArquivo, dadosCompletos);

            System.out.println("[THREAD " + Thread.currentThread().getId()
                    + "] Upload de '" + nomeArquivo + "' concluído com sucesso.");

        } catch (IOException e) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Erro ao receber/salvar arquivo '" + nomeArquivo + "': " + e.getMessage());
        }
    }

    private void enviarArquivo(DatagramSocket socket, String nomeArquivo) {
        System.out.println("[THREAD " + Thread.currentThread().getId()
                + "] Iniciando envio do arquivo: " + nomeArquivo);

        if (!FileManager.arquivoExiste(nomeArquivo)) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Arquivo não encontrado: " + nomeArquivo);
            return;
        }

        try {
            byte[] dados = FileManager.lerArquivo(nomeArquivo);
            Protocol.enviarBytes(socket, clienteIp, clientePorta, dados);

            System.out.println("[THREAD " + Thread.currentThread().getId()
                    + "] Download de '" + nomeArquivo + "' finalizado.");

        } catch (IOException e) {
            System.err.println("[THREAD " + Thread.currentThread().getId()
                    + "] Erro ao ler/enviar arquivo '" + nomeArquivo + "': " + e.getMessage());
        }
    }
}