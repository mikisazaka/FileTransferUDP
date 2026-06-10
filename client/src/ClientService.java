package src;

import java.net.*;
import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    private void enviarArquivo(
            DatagramSocket socket,
            InetAddress servidor,
            Scanner scanner) throws Exception {

        System.out.print("Caminho do arquivo: ");
        String caminho = scanner.nextLine();

        File arquivo = new File(caminho);

        if (!arquivo.exists()) {
            System.out.println("Arquivo não encontrado.");
            return;
        }

        String comando =
                "UPLOAD:" + arquivo.getName();

        socket.send(
                new DatagramPacket(
                        comando.getBytes(),
                        comando.length(),
                        servidor,
                        PORTA));

        byte[] buffer = new byte[128];
        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
        socket.receive(resposta);

        int portaTransferencia = Integer.parseInt(
                new String(resposta.getData(),0,resposta.getLength()).replace("PORTA:","").trim());

        byte[] dados =
                Files.readAllBytes(
                        arquivo.toPath());

        Protocol.enviarBytes(
                socket,
                servidor,
                portaTransferencia,
                dados);

        System.out.println(
                "Upload concluído.");
    }

    private void baixarArquivo(
            DatagramSocket socket,
            InetAddress servidor,
            Scanner scanner) throws Exception {

        System.out.print(
                "Nome do arquivo: ");

        String nomeArquivo =
                scanner.nextLine();

        String comando =
                "DOWNLOAD:" + nomeArquivo;

        socket.send(
                new DatagramPacket(
                        comando.getBytes(),
                        comando.length(),
                        servidor,
                        PORTA));

        byte[] buffer = new byte[128];
        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
        socket.receive(resposta);

        int portaTransferencia = Integer.parseInt(
                new String(resposta.getData(),0,resposta.getLength()).replace("PORTA:","").trim());

        byte[] dados =
                Protocol.receberBytes(
                        socket,
                        servidor,
                        portaTransferencia);

        Files.write(
                Paths.get(
                        "download_" + nomeArquivo),
                dados);

        System.out.println(
                "Download concluído.");
    }
}