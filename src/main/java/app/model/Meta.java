package app.model;

import java.util.UUID;

public class Meta {

    private final UUID id;
    private final String nome;
    private final Long alvoCentavos;
    private long atualCentavos;

    public Meta(String nome, Long alvoCentavos) {
        this(UUID.randomUUID(), nome, alvoCentavos, 0);
    }

    public Meta(UUID id, String nome, Long alvoCentavos, long atualCentavos) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.nome = nome == null ? "" : nome.trim();
        this.alvoCentavos = alvoCentavos;
        this.atualCentavos = atualCentavos;
    }

    public UUID getId() {
        return id;
    }

    public String getIdAsString() {
        return id.toString();
    }

    public String getNome() {
        return nome;
    }

    public Long getAlvoCentavos() {
        return alvoCentavos;
    }

    public long getAtualCentavos() {
        return atualCentavos;
    }

    public boolean semAlvo() {
        return alvoCentavos == null || alvoCentavos <= 0;
    }

    // Calcula quanto falta para alcançar meta
    public long restanteParaAlvo() {
        if (semAlvo()) return Long.MAX_VALUE; // Se não for definido valor alvo
        return Math.max(0, alvoCentavos - atualCentavos);
    }

    public void adicionar(long centavos) {
        if (centavos <= 0)
            throw new IllegalArgumentException("valor deve ser positivo");
        atualCentavos += centavos;
    }

    public long retirar(long centavos) {
        if (centavos <= 0) return 0;
        long permitido = Math.min(centavos, atualCentavos);
        atualCentavos -= permitido;
        return permitido;
    }

    public double progresso() {
        if (semAlvo()) return 0.0;
        return Math.min((double) atualCentavos / alvoCentavos, 1.0);
    }

    @Override
    public String toString() {
        return nome;
    }
}