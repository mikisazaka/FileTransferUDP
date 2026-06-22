import java.net.*;
import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

ppublic class ClientService {

    // Define o endereço do servidor (neste caso, a própria máquina local)
    private static final String HOST = "localhost";
    
    // Define a porta padrão onde o servidor principal está escutando as requisições
    private static final int PORTA = 9876;

    /**
     * Método principal que inicia o cliente e gerencia o menu de interações.
     */
    public void iniciar() {

        // Uso do "Try-with-resources" para garantir que o socket e o scanner 
        // sejam fechados automaticamente ao encerrar o bloco, evitando vazamento de memória.
        try (
                DatagramSocket socket = new DatagramSocket(); // Cria o socket UDP do cliente
                Scanner scanner = new Scanner(System.in)      // Cria o leitor do teclado
        ) {

            // Converte o texto do HOST ("localhost") em um objeto de endereço IP válido
            InetAddress servidor = InetAddress.getByName(HOST);

            // Loop infinito para manter o menu do cliente rodando até que ele decida sair
            while (true) {

                // Exibição do menu textual no console
                System.out.println("\n===== CLIENTE UDP =====");
                System.out.println("1 - Listar arquivos");
                System.out.println("2 - Enviar arquivo");
                System.out.println("3 - Download");
                System.out.println("4 - Sair");
                System.out.print("Escolha: ");

                // Validação: Se o usuário não digitar um número inteiro...
                if (!scanner.hasNextInt()) {
                    scanner.nextLine(); // Descarta a entrada inválida (limpa o buffer)
                    continue;           // Volta para o início do loop (exibe o menu de novo)
                }
                
                // Captura a opção numérica digitada pelo usuário
                int opcao = scanner.nextInt();
                scanner.nextLine(); // Limpa a quebra de linha (\n) que sobra no buffer

                // Direciona o fluxo do programa com base na opção escolhida
                 switch (opcao) {

                    case 1:
                        // Chama o método para listar os arquivos do servidor
                        listarArquivos(socket, servidor);
                        break;

                    case 2:
                        // Chama o método para fazer upload de um arquivo
                        enviarArquivo(socket, servidor, scanner);
                        break;

                    case 3:
                        // Chama o método para fazer download de um arquivo
                        baixarArquivo(socket, servidor, scanner);
                        break;

                    case 4:
                        // Encerra o método iniciar(), fechando o programa cliente
                        return;
                }
            }

        } catch (Exception e) {
            // Captura qualquer exceção não tratada e exibe o rastro do erro
            System.out.println("Erro inesperado na execucao do cliente.");
            e.printStackTrace();
        }
    }

    /**
     * Solicita ao servidor a listagem de arquivos disponíveis.
     */
    private void listarArquivos(
            DatagramSocket socket,
            InetAddress servidor) throws Exception {

        // Converte o comando de texto em um array de bytes (padrão do protocolo UDP)
        byte[] dados = "LISTAR".getBytes();

        // Monta o pacote UDP com a mensagem, o tamanho, o destino e a porta do servidor
        DatagramPacket pacote = new DatagramPacket(
                dados,
                dados.length,
                servidor,
                PORTA
        );

        // Envia o pacote de requisição através do socket
        socket.send(pacote);

        // Prepara um buffer (espaço em memória) de 4KB para receber a resposta do servidor
        byte[] buffer = new byte[4096];

        // Cria o pacote vazio que será preenchido com os dados vindos do servidor
        DatagramPacket resposta =
                new DatagramPacket(buffer, buffer.length);

        // Bloqueia a execução do código até que a resposta do servidor chegue
        socket.receive(resposta);

        // Converte os bytes recebidos de volta para uma String textual
        String lista = new String(
                resposta.getData(),     // O array de bytes bruto
                0,                      // Índice inicial
                resposta.getLength()    // Quantidade real de bytes recebidos
        );

        // Exibe os arquivos listados enviados pelo servidor
        System.out.println("\nArquivos disponíveis:");
        System.out.println(lista);
    }

    /**
     * Envia um arquivo local para o servidor (Upload).
     */
    private void enviarArquivo(
            DatagramSocket socket,
            InetAddress servidor,
            Scanner scanner) throws Exception {

        // Pergunta ao usuário onde está o arquivo na máquina dele
        System.out.print("Caminho do arquivo: ");
        String caminho = scanner.nextLine();

        // Cria uma referência ao arquivo com base no caminho fornecido
        File arquivo = new File(caminho);

        // Validação local para garantir que o arquivo realmente existe antes de tentar enviar
        if (!arquivo.exists()) {
            System.out.println("Arquivo não encontrado, verifique o nome do arquivo.");
            return; // Aborta o método de upload se não existir
        }

        // Monta a string do protocolo indicando a intenção de UPLOAD e o nome do arquivo
        String comando = "UPLOAD:" + arquivo.getName();

        // Cria e envia o pacote de notificação de upload para o servidor principal
        socket.send(
                new DatagramPacket(
                        comando.getBytes(),
                        comando.length(),
                        servidor,
                        PORTA));

        // Cria um buffer pequeno para escutar qual será a porta dedicada ao upload
        byte[] buffer = new byte[128];
        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
        
        // Recebe a porta dedicada que o servidor abriu exclusivamente para este arquivo
        socket.receive(resposta);

        // Converte a resposta em String, remove o prefixo "PORTA:" e extrai o número da porta dedicada
        int portaTransferencia = Integer.parseInt(
                new String(resposta.getData(), 0, resposta.getLength())
                .replace("PORTA:", "")
                .trim()
        );

        // Lê todos os bytes do arquivo de forma síncrona para a memória RAM
        byte[] dados = Files.readAllBytes(arquivo.toPath());

        // Utiliza uma classe utilitária (Protocol) para transmitir os bytes em blocos
        // para a porta de transferência dedicada do servidor
        Protocol.enviarBytes(
                socket,
                servidor,
                portaTransferencia,
                dados);

        System.out.println("Upload concluído.");
    }

    /**
     * Solicita e recebe um arquivo do servidor (Download).
     */
    private void baixarArquivo(
            DatagramSocket socket,
            InetAddress servidor,
            Scanner scanner) throws Exception {

        // Solicita o nome do arquivo que o usuário deseja baixar
        System.out.print("Nome do arquivo: ");
        String nomeArquivo = scanner.nextLine();

        // Monta a string do protocolo de DOWNLOAD
        String comando = "DOWNLOAD:" + nomeArquivo;

        // Envia o pedido de download para o servidor principal (Porta 9876)
        socket.send(
                new DatagramPacket(
                        comando.getBytes(),
                        comando.length(),
                        servidor,
                        PORTA));

        // Buffer de 512 bytes para garantir a leitura de mensagens de erro textuais do servidor
        byte[] buffer = new byte[512]; 
        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
        
        // Aguarda a resposta do servidor (uma nova porta ou uma mensagem de erro)
        socket.receive(resposta);

        // Converte os dados da resposta em texto limpando espaços em branco nas pontas
        String conteudoResposta = new String(resposta.getData(), 0, resposta.getLength()).trim();

        // Trata arquivos inexistentes no servidor impedindo que a string de erro quebre o parseInt
        // Se a resposta não começar com "PORTA:", significa que o servidor enviou um erro em texto
        if (!conteudoResposta.startsWith("PORTA:")) {
            System.out.println("Arquivo não encontrado, verifique o nome do arquivo.");
            return; // Aborta a operação de download
        }

        // Se passou na validação, extrai o número da porta dedicada de transferência
        int portaTransferencia = Integer.parseInt(conteudoResposta.replace("PORTA:", "").trim());

        // Utiliza a classe utilitária (Protocol) para escutar e reconstruir o array de bytes 
        // enviado em blocos pelo servidor através da porta dedicada
        byte[] dados = Protocol.receberBytes(
                        socket,
                        servidor,
                        portaTransferencia);

        // Grava o array de bytes recebido no disco rígido do cliente.
        // O arquivo é salvo com o prefixo "download_" concatenado ao nome original
        Files.write(
                Paths.get("download_" + nomeArquivo),
                dados);

        System.out.println("Download concluído.");
    }
}