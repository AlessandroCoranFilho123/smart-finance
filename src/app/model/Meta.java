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
        this.nome = nome;
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

    public void setAtualCentavos(long valor) {
        this.atualCentavos = Math.max(0, valor);
    }

    public boolean possuiAlvo() {
        return alvoCentavos == null;
    }

    public long restanteParaAlvo() {
        if (possuiAlvo()) return Long.MAX_VALUE;
        return Math.max(alvoCentavos - atualCentavos, 0);
    }

    public void adicionar(long centavos) {
        atualCentavos += centavos;
    }

    public long retirar(long centavos) {
        long permitido = Math.min(centavos, atualCentavos);
        atualCentavos -= permitido;
        return permitido;
    }

    public double progresso() {
        if (possuiAlvo() || alvoCentavos == 0) return 0;
        return Math.min((double) atualCentavos / alvoCentavos, 1.0);
    }
}
