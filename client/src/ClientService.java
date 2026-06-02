import java.net.*;
import java.util.Scanner;

public class ClientService {

    private static final String HOST = "localhost";
    private static final int PORTA = 9876;

    public void iniciar() {

        try (
                DatagramSocket socket = new DatagramSocket();
                Scanner scanner = new Scanner(System.in)
        ) {

            InetAddress servidor = InetAddress.getByName(HOST);

            while (true) {

                System.out.println("\n===== CLIENTE UDP =====");
                System.out.println("1 - Listar arquivos");
                System.out.println("2 - Enviar arquivo");
                System.out.println("3 - Download");
                System.out.println("4 - Sair");
                System.out.print("Escolha: ");

                int opcao = scanner.nextInt();
                scanner.nextLine();

                 switch (opcao) {

                    case 1:
                        listarArquivos(socket, servidor);
                        break;

                    case 2:
                        enviarArquivo(socket, servidor, scanner);
                        break;

                    case 3:
                        baixarArquivo(socket, servidor, scanner);
                        break;

                    case 4:
                        return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listarArquivos(
            DatagramSocket socket,
            InetAddress servidor) throws Exception {

        byte[] dados = "LISTAR".getBytes();

        DatagramPacket pacote = new DatagramPacket(
                dados,
                dados.length,
                servidor,
                PORTA
        );

        socket.send(pacote);

        byte[] buffer = new byte[4096];

        DatagramPacket resposta =
                new DatagramPacket(buffer, buffer.length);

        socket.receive(resposta);

        String lista = new String(
                resposta.getData(),
                0,
                resposta.getLength()
        );

        System.out.println("\nArquivos disponíveis:");
        System.out.println(lista);
    }
}