import java.io.*;
import java.net.*;
import java.util.*;

public class Protocol {
    // Quantidade máxima de bytes transportados em cada pacote de dados
    private static final int TAM_PACOTE = 1024;

    // Tempo máximo de espera pelo ACK (em milissegundos - 2 segundos)
    private static final int TIMEOUT = 2000;

    public static void enviarBytes(DatagramSocket socket, InetAddress ip, int porta, byte[] dados) throws IOException {
        // Configura o tempo máximo de espera por um ACK
        socket.setSoTimeout(TIMEOUT);

        // Número de sequência do pacote atual
        int sequencia = 0;

        // Posição atual dentro do vetor de bytes do arquivo
        int offset = 0;

        // Continua enviando enquanto ainda houver dados não enviados
        while (offset < dados.length) {
            // Define o tamanho do próximo bloco de dados
            int tamanho = Math.min(TAM_PACOTE, dados.length - offset);

            // Fluxo para montar o pacote em memória
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Escreve o número de sequência no cabeçalho do pacote
            dos.writeInt(sequencia);

            // Escreve o tamanho do bloco de dados
            dos.writeInt(tamanho);

            // Escreve os dados do arquivo
            dos.write(dados, offset, tamanho);

            // Converte todo o conteúdo para um vetor de bytes
            byte[] pacote = baos.toByteArray();

            // Controla se o ACK correspondente foi recebido
            boolean ackRecebido = false;

            // Estratégia Stop-and-Wait:
            // só envia o próximo pacote após receber o ACK do atual
            while (!ackRecebido) {

                // Cria o datagrama UDP contendo o pacote
                DatagramPacket envio = new DatagramPacket(pacote, pacote.length, ip, porta);

                // Envia o pacote para o destino
                socket.send(envio);

                System.out.println("[ENVIO] Pacote " + sequencia);

                try {
                    // Buffer para receber o ACK
                    byte[] ackBuffer = new byte[100];

                    // Datagrama que receberá o ACK
                    DatagramPacket ack = new DatagramPacket(ackBuffer, ackBuffer.length);

                    // Aguarda a chegada do ACK
                    socket.receive(ack);

                    // Converte o ACK recebido para String
                    String resposta = new String(ack.getData(), 0, ack.getLength());

                    // Verifica se o ACK recebido corresponde ao pacote enviado
                    if (resposta.equals("ACK:" + sequencia)) {

                        // ACK correto recebido
                        ackRecebido = true;

                        System.out.println("[ACK] Recebido ACK " + sequencia);
                    }

                } catch (SocketTimeoutException e) {
                    // Timeout ocorreu: considera o pacote perdido e retransmite
                    System.out.println("[TIMEOUT] Pacote " + sequencia + " perdido. Retransmitindo...");
                }
            }

            // Avança para o próximo bloco do arquivo
            offset += tamanho;

            // Incrementa o número de sequência
            sequencia++;
        }

        // Cria um pacote especial indicando o fim da transmissão
        ByteArrayOutputStream fim = new ByteArrayOutputStream();
        DataOutputStream dosFim = new DataOutputStream(fim);

        // Sequência -1 significa "fim do arquivo"
        dosFim.writeInt(-1);

        // Envia o pacote de término
        socket.send(new DatagramPacket(fim.toByteArray(), fim.size(), ip, porta));
        System.out.println("[ENVIO] Fim do arquivo");
    }

    public static byte[] receberBytes(DatagramSocket socket, InetAddress ip, int porta) throws IOException {

        // Estrutura que armazena os pacotes ordenados pelo número de sequência
        Map<Integer, byte[]> pacotes = new TreeMap<>();

        while (true) {
            // Buffer para receber um pacote completo
            byte[] buffer = new byte[TAM_PACOTE + 8];

            // Datagrama de recepção
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

            // Aguarda um pacote UDP
            socket.receive(pacote);

            // Fluxo para leitura do conteúdo recebido
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pacote.getData(), 0, pacote.getLength()));

            // Lê o número de sequência
            int seq = dis.readInt();

            // Sequência -1 indica o término da transmissão
            if (seq == -1) {
                System.out.println("[RECEPÇÃO] Fim do arquivo");
                break;
            }

            // Lê o tamanho do bloco recebido
            int tamanho = dis.readInt();

            // Cria vetor para armazenar os dados
            byte[] dados = new byte[tamanho];

            // Lê os bytes do pacote
            dis.readFully(dados);

            /**
             * Simulação de perda de pacotes.
             * Aproximadamente 10% dos pacotes serão descartados.
             * Como não haverá ACK, o remetente realizará a retransmissão.
             */
            if (Math.random() < 0.10) {
                System.out.println("[SIMULAÇÃO] Pacote " + seq + " descartado");
                continue;
            }

            // Armazena o pacote utilizando a sequência como chave
            pacotes.put(seq, dados);

            // Monta a mensagem de ACK
            String ackMsg = "ACK:" + seq;

            // Converte o ACK para bytes
            byte[] ackDados = ackMsg.getBytes();

            // Envia o ACK para o remetente
            socket.send(new DatagramPacket(ackDados, ackDados.length, pacote.getAddress(), pacote.getPort()));

            System.out.println("[ACK] Enviado ACK " + seq);
        }

        // Estrutura utilizada para reconstruir o arquivo
        ByteArrayOutputStream arquivo = new ByteArrayOutputStream();

        // Percorre os pacotes já ordenados pelo TreeMap
        for (byte[] parte : pacotes.values()) {

            // Concatena cada parte na ordem correta
            arquivo.write(parte);
        }

        // Retorna o arquivo completo reconstruído
        return arquivo.toByteArray();
    }
}