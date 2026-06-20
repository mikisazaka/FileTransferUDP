import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Ponto de entrada do servidor UDP
 *
 * Como UDP não é orientado a conexão, o mesmo socket é compartilhado
 * entre o loop principal e as threads. Isso exige sincronização em envios
 * concorrentes, tratada dentro do ClientHandler
 */
public class ServerMain {

    // Porta padrão que o servidor ficará escutando
    private static final int PORTA = 9876;

    // Tamanho do buffer para o pacote inicial de comando (somente o texto, não o arquivo em si)
    private static final int TAMANHO_BUFFER = 1024;

    public static void main(String[] args) {
        System.out.println("Inicializando servidor UDP na porta " + PORTA + "...");

        // Tenta executar esse bloco de código (fecha o socket automaticamente se o servidor encerrar)
        try (DatagramSocket serverSocket = new DatagramSocket(PORTA)) {
            System.out.println("Servidor pronto e aguardando requisições...");

            while (true) {
                // Criação de um novo buffer para cada iteração
                byte[] bufferRecebimento = new byte[TAMANHO_BUFFER];

                DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecebimento, bufferRecebimento.length);

                // A thread principal fica parada aqui até receber algo
                serverSocket.receive(pacoteRecebido);

                System.out.println("Nova requisição recebida de: "
                        + pacoteRecebido.getAddress() + ":" + pacoteRecebido.getPort());

                // Cada cliente é atendido em uma thread separada, permitindo que o
                // loop principal volte ao receber novos pacotes
                ClientHandler handler = new ClientHandler(pacoteRecebido, serverSocket);
                Thread threadCliente = new Thread(handler);
                threadCliente.start();
            }

        } catch (IOException e) {
            // Captura de erros quando o servidor não conseguiu abrir/usar o socket
            System.err.println("Erro crítico no servidor: " + e.getMessage());
        }
    }
}