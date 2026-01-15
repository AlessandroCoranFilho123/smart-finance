package app.service;

import app.model.Meta;
import app.model.Transacao;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class PersistenciaService {

    private static final Path BASE_DIR = Paths.get(
            System.getProperty("user.home"),
            "AppData",
            "Local",
            "ProjetoFinancas"
    );

    private static final Path TRANSACOES_FILE = BASE_DIR.resolve("transacoes.dat");
    private static final Path METAS_FILE = BASE_DIR.resolve("metas.dat");

    static {
        try {
            Files.createDirectories(BASE_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao criar diretório de dados", e);
        }
    }

    public static List<Transacao> carregarTransacoes() {
        if (!Files.exists(TRANSACOES_FILE)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois =
                     new ObjectInputStream(Files.newInputStream(TRANSACOES_FILE))) {

            return (List<Transacao>) ois.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void salvarTransacoes(List<Transacao> transacoes) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(Files.newOutputStream(
                             TRANSACOES_FILE,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING
                     ))) {

            oos.writeObject(transacoes);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Meta> carregarMetas() {
        if (!Files.exists(METAS_FILE)) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois =
                     new ObjectInputStream(Files.newInputStream(METAS_FILE))) {

            return (List<Meta>) ois.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void salvarMetas(List<Meta> metas) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(Files.newOutputStream(
                             METAS_FILE,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING
                     ))) {

            oos.writeObject(metas);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Path getBaseDir() {
        return BASE_DIR;
    }
}
