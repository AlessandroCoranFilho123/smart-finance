package app.service;

import app.model.Meta;
import app.model.Transacao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PersistenciaService {

    private static final String DIR = "data";
    private static final String METAS_FILE = DIR + "/metas.dat";
    private static final String TRANSACOES_FILE = DIR + "/transacoes.dat";

    public static void salvarMetas(List<Meta> metas) {
        salvarObjeto(metas, METAS_FILE);
    }

    public static void salvarTransacoes(List<Transacao> transacoes) {
        salvarObjeto(transacoes, TRANSACOES_FILE);
    }

    @SuppressWarnings("unchecked")
    public static List<Meta> carregarMetas() {
        return (List<Meta>) carregarObjeto(METAS_FILE, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public static List<Transacao> carregarTransacoes() {
        return (List<Transacao>) carregarObjeto(TRANSACOES_FILE, new ArrayList<>());
    }

    private static void salvarObjeto(Object obj, String caminho) {
        try {
            new File(DIR).mkdirs();
            ObjectOutputStream oos =
                    new ObjectOutputStream(new FileOutputStream(caminho));
            oos.writeObject(obj);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Object carregarObjeto(String caminho, Object padrao) {
        try {
            ObjectInputStream ois =
                    new ObjectInputStream(new FileInputStream(caminho));
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } catch (Exception e) {
            return padrao;
        }
    }
}
