import java.io.*;
import java.nio.file.*;

// Responsável por todas as operações de entrada e saída pelo usuário no sistema de arquivos do servidor
public class FileManager {

    // Diretório onde os arquivos recebidos via upload serão salvos
    private static final String DIRETORIO = "server/arquivos_recebidos/";

    // Persiste um array de bytes no disco com o nome especificado
    public static void salvarArquivo(String nomeArquivo, byte[] dados) throws IOException {
        garantirDiretorio();

        File destino = new File(DIRETORIO + nomeArquivo);

        // Garante o fechamento do stream mesmo em caso de exceção
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            fos.write(dados);
        }

        System.out.println("Arquivo salvo com sucesso em: " + destino.getPath()
                + " (" + dados.length + " bytes)");
    }

    // Lê um arquivo do diretório do servidor e retorna seu conteúdo como bytes
    // Usado antes de enviar um arquivo ao cliente via download
    public static byte[] lerArquivo(String nomeArquivo) throws IOException {
        File origem = new File(DIRETORIO + nomeArquivo);

        if (!origem.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado no servidor: " + nomeArquivo);
        }

        // Files.readAllBytes: lê o arquivo inteiro de uma vez
        byte[] dados = Files.readAllBytes(origem.toPath());
        System.out.println("Arquivo lido para envio: " + nomeArquivo
                + " (" + dados.length + " bytes)");
        return dados;
    }

    // Lista todos os arquivos presentes no diretório do servidor
    public static String[] listarArquivos() {
        garantirDiretorio();

        File pasta = new File(DIRETORIO);

        // pasta.list() pode retornar null se o caminho não for um diretório válido
        String[] arquivos = pasta.list();

        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum arquivo disponível no servidor.");
            return new String[0];
        }

        System.out.println(arquivos.length + " arquivo(s) disponível(is).");
        return arquivos;
    }

    // Verifica se um arquivo existe no diretório do servidor
    // Usado pelo ClientHandler antes de tentar enviar um arquivo ao cliente
    public static boolean arquivoExiste(String nomeArquivo) {
        return new File(DIRETORIO + nomeArquivo).exists();
    }

    // Cria como prevenção o diretório de armazenamento caso ele ainda não exista
    private static void garantirDiretorio() {
        File pasta = new File(DIRETORIO);
        if (!pasta.exists()) {
            // Cria toda a cadeia de diretórios intermediários se necessário
            pasta.mkdirs();
            System.out.println("Diretório criado: " + pasta.getPath());
        }
    }
}