package src;

import java.io.*;
import java.net.*;
import java.util.*;

public class Protocol {
    private static final int TAM_PACOTE = 1024;
    private static final int TIMEOUT = 2000;

    public static void enviarBytes(DatagramSocket socket, InetAddress ip, int porta, byte[] dados) throws IOException {
        socket.setSoTimeout(TIMEOUT);

        int sequencia = 0;
        int offset = 0;

        while (offset < dados.length) {
            int tamanho = Math.min(TAM_PACOTE, dados.length - offset);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(sequencia);
            dos.writeInt(tamanho);
            dos.write(dados, offset, tamanho);

            byte[] pacote = baos.toByteArray();
            boolean ackRecebido = false;

            while (!ackRecebido) {
                DatagramPacket envio = new DatagramPacket(pacote, pacote.length, ip, porta);
                socket.send(envio);
                System.out.println("[ENVIO] Pacote " + sequencia);

                try {
                    byte[] ackBuffer = new byte[100];
                    DatagramPacket ack = new DatagramPacket(ackBuffer, ackBuffer.length);
                    socket.receive(ack);

                    String resposta = new String(ack.getData(), 0, ack.getLength());

                    if (resposta.equals("ACK:" + sequencia)) {
                        ackRecebido = true;
                        System.out.println("[ACK] Recebido ACK " + sequencia);
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("[TIMEOUT] Pacote " + sequencia + " perdido. Retransmitindo...");
                }
            }

            offset += tamanho;
            sequencia++;
        }

        ByteArrayOutputStream fim = new ByteArrayOutputStream();
        DataOutputStream dosFim = new DataOutputStream(fim);

        dosFim.writeInt(-1);

        socket.send(new DatagramPacket(fim.toByteArray(), fim.size(), ip, porta));
        System.out.println("[ENVIO] Fim do arquivo");
    }

    public static byte[] receberBytes(DatagramSocket socket, InetAddress ip, int porta) throws IOException {
        Map<Integer, byte[]> pacotes = new TreeMap<>();

        while (true) {
            byte[] buffer = new byte[TAM_PACOTE + 8];
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            socket.receive(pacote);

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pacote.getData(), 0, pacote.getLength()));
            int seq = dis.readInt();

            if (seq == -1) {
                System.out.println("[RECEPÇÃO] Fim do arquivo");
                break;
            }

            int tamanho = dis.readInt();
            byte[] dados = new byte[tamanho];
            dis.readFully(dados);

            if (Math.random() < 0.10) {
                System.out.println("[SIMULAÇÃO] Pacote " + seq + " descartado");
                continue;
            }

            pacotes.put(seq, dados);
            String ackMsg = "ACK:" + seq;
            byte[] ackDados = ackMsg.getBytes();

            socket.send(new DatagramPacket(ackDados, ackDados.length, pacote.getAddress(), pacote.getPort()));

            System.out.println("[ACK] Enviado ACK " + seq);
        }

        ByteArrayOutputStream arquivo = new ByteArrayOutputStream();

        for (byte[] parte : pacotes.values()) {
            arquivo.write(parte);
        }

        return arquivo.toByteArray();
    }
}