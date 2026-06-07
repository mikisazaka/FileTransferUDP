package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerMain {
    private static final int PORTA = 9876;
    private static final int TAMANHO_BUFFER = 1024;

    public static void main(String[] args) {
        System.out.println("Inicializando servidor UDP na porta " + PORTA + "...");

        try (DatagramSocket serverSocket = new DatagramSocket(PORTA)) {
            System.out.println("Servidor pronto e aguardando requisições...");

            while (true) {
                byte[] bufferRecebimento = new byte[TAMANHO_BUFFER];

                DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecebimento, bufferRecebimento.length);
                serverSocket.receive(pacoteRecebido);

                System.out.println("Nova requisição recebida de: "
                        + pacoteRecebido.getAddress() + ":" + pacoteRecebido.getPort());

                ClientHandler handler = new ClientHandler(pacoteRecebido);
                Thread threadCliente = new Thread(handler);
                threadCliente.start();
            }

        } catch (IOException e) {
            System.err.println("Erro crítico no servidor: " + e.getMessage());
        }
    }
}