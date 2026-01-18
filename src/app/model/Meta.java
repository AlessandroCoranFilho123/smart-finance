package app.model;

import java.io.Serial;
import java.io.Serializable;

public class Meta implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String nome;
    private final Long alvoCentavos;
    private long atualCentavos;

    public Meta(String nome, Long alvoCentavos) {
        this.nome = nome == null ? "" : nome;
        this.alvoCentavos = alvoCentavos;
        this.atualCentavos = 0;
    }

    @Override
    public String toString() {
        return nome;
    }

    public String getNome() {
        return nome;
    }

    public long getAtualCentavos() {
        return atualCentavos;
    }

    public boolean possuiAlvo() {
        return alvoCentavos != null && alvoCentavos > 0;
    }

    public long restanteParaAlvo() {
        if (!possuiAlvo()) return Long.MAX_VALUE;
        return Math.max(0, alvoCentavos - atualCentavos);
    }

    public void adicionar(long centavos) {
        if (centavos > 0) {
            atualCentavos += centavos;
        }
    }

    public long retirar(long centavos) {
        if (centavos <= 0) return 0;
        long permitido = Math.min(centavos, atualCentavos);
        atualCentavos -= permitido;
        return permitido;
    }

    public double progresso() {
        if (!possuiAlvo()) return 0.0;
        return Math.min((double) atualCentavos / alvoCentavos, 1.0);
    }
}
