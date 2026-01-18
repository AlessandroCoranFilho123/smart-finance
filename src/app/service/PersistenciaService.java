package app.service;

import app.model.Meta;
import app.model.Transacao;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class PersistenciaService {


    private static final Path BASE_DIR = Paths.get(
            System.getProperty("user.home"), ".projetofinancas");

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

            Object obj = ois.readObject();

            if (obj instanceof List<?> list &&
                    list.stream().allMatch(Transacao.class::isInstance)) {
                return list.stream()
                        .map(Transacao.class::cast)
                        .toList();
            }
            throw new IllegalStateException("Arquivo de transações inválido");
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Erro ao carregar transações", e);
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
            oos.flush();

        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar transações", e);
        }
    }

    public static List<Meta> carregarMetas() {
        if (!Files.exists(METAS_FILE)) {
            return List.of();
        }

        try (ObjectInputStream ois =
                     new ObjectInputStream(Files.newInputStream(METAS_FILE))) {

            Object obj = ois.readObject();

            if (obj instanceof List<?> list &&
                    list.stream().allMatch(Meta.class::isInstance)) {

                return list.stream()
                        .map(Meta.class::cast)
                        .toList();
            }
            throw new IllegalStateException("Arquivo de metas inválido");
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Erro ao carregar meta", e);
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
            oos.flush();

        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar meta", e);
        }
    }
}
