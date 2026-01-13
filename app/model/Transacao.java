package app.model;

import java.io.Serial;
import java.util.Set;
import java.io.Serializable;

public class Transacao implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final TipoTransacao tipo;
    private final Categoria categoria;
    private final long valorCentavos;
    private final String metaNome;
    private String comentario;
    private final Set<String> tags;
    private final String nome;

    public Transacao(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            String metaNome,
            String comentario,
            Set<String> tags,
            String nome
    ) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.valorCentavos = valorCentavos;
        this.metaNome = metaNome == null ? "" : metaNome;
        this.comentario = comentario == null ? "" : comentario;
        this.tags = tags == null ? Set.of() : tags;
        this.nome = nome == null ? "" : nome;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public String getMetaNome() {
        return metaNome;
    }

    public String getComentario() {
        return comentario;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getNome() {
        return nome;
    }

    public boolean temMeta() {
        return !metaNome.isBlank();
    }

    public void setComentario(String comentario) {
        this.comentario = comentario == null ? "" : comentario;
    }
}



