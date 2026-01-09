package app.model;

import java.time.LocalDateTime;
import java.util.Set;

public class Transacao {

    private final TipoTransacao tipo;
    private final Categoria categoria;
    private final long valorCentavos;
    private final String metaNome;
    private final String comentario;
    private final Set<String> tags;
    private LocalDateTime dataHora;
    private final String nome;

    public Transacao(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            String metaNome,
            String comentario,
            Set<String> tags,
            LocalDateTime dataHora,
            String nome
    ) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.valorCentavos = valorCentavos;
        this.metaNome = metaNome == null ? "" : metaNome;
        this.comentario = comentario == null ? "" : comentario;
        this.tags = tags == null ? Set.of() : tags;
        this.dataHora = dataHora == null ? LocalDateTime.now() : dataHora;
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

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getNome() {
        return nome;
    }

    public boolean temMeta() {
        return !metaNome.isBlank();
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}


