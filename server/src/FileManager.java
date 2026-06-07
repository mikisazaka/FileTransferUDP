package src;

import java.io.*;
import java.nio.file.*;

public class FileManager {

    private static final String DIRETORIO = "server/arquivos_recebidos/";

    public static void salvarArquivo(String nomeArquivo, byte[] dados) throws IOException {
        garantirDiretorio();

        File destino = new File(DIRETORIO + nomeArquivo);
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            fos.write(dados);
        }

        System.out.println("Arquivo salvo com sucesso em: " + destino.getPath()
                + " (" + dados.length + " bytes)");
    }

    public static byte[] lerArquivo(String nomeArquivo) throws IOException {
        File origem = new File(DIRETORIO + nomeArquivo);

        if (!origem.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado no servidor: " + nomeArquivo);
        }

        byte[] dados = Files.readAllBytes(origem.toPath());
        System.out.println("Arquivo lido para envio: " + nomeArquivo
                + " (" + dados.length + " bytes)");
        return dados;
    }

    public static String[] listarArquivos() {
        garantirDiretorio();

        File pasta = new File(DIRETORIO);
        String[] arquivos = pasta.list();

        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum arquivo disponível no servidor.");
            return new String[0];
        }

        System.out.println(arquivos.length + " arquivo(s) disponível(is).");
        return arquivos;
    }

    public static boolean arquivoExiste(String nomeArquivo) {
        return new File(DIRETORIO + nomeArquivo).exists();
    }

    private static void garantirDiretorio() {
        File pasta = new File(DIRETORIO);
        if (!pasta.exists()) {
            pasta.mkdirs();
            System.out.println("Diretório criado: " + pasta.getPath());
        }
    }
}