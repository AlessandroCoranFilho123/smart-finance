package app.service;

import app.model.Meta;
import app.repository.MetaDAO;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MetaService {

    private final MetaDAO metaDAO;

    public MetaService() {
        this.metaDAO = new MetaDAO();
    }

    public MetaService(MetaDAO metaDAO) {
        this.metaDAO = Objects.requireNonNull(metaDAO, "MetaDAO não pode ser null");
    }

    public void criarMeta(String nome, long alvoCentavos) {
        Objects.requireNonNull(nome, "Nome é obrigatório");

        if (nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome não pode estar vazio");
        }

        if (alvoCentavos <= 0) {
            throw new IllegalArgumentException("Valor alvo deve ser positivo");
        }
        Meta meta = new Meta(nome.trim(), alvoCentavos);
        metaDAO.inserir(meta);
    }

    public void atualizarMeta(Meta meta) {
        Objects.requireNonNull(meta, "Meta é obrigatória");

        if (meta.semAlvo()) {
            throw new IllegalArgumentException("Meta deve ter valor alvo");
        }

        metaDAO.atualizar(meta);
    }

    @SuppressWarnings("unused")
    public void deletarMeta(UUID id) {
        Objects.requireNonNull(id, "ID é obrigatório");

        metaDAO.deletar(id);
    }

    public List<Meta> listarTodasMetas() {
        return metaDAO.listarTodas();
    }

    @SuppressWarnings("unused")
    public Meta buscarPorId(UUID id) {
        Objects.requireNonNull(id, "ID é obrigatório");
        return metaDAO.buscarPorId(id);
    }

    public double calcularProgresso(Meta meta) {
        Objects.requireNonNull(meta, "Meta é obrigatória");
        return meta.progresso() * 100.0;
    }

    public boolean metaConcluida(Meta meta) {
        Objects.requireNonNull(meta, "Meta é obrigatória");
        return meta.getAtualCentavos() >= meta.getAlvoCentavos();
    }

    @SuppressWarnings("unused")
    public long calcularRestante(Meta meta) {
        Objects.requireNonNull(meta, "Meta é obrigatória");
        return meta.restanteParaAlvo();
    }

    public long contarMetasConcluidas() {
        return listarTodasMetas().stream()
                .filter(this::metaConcluida)
                .count();
    }
}