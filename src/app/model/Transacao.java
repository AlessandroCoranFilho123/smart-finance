package app.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        this.tipo = Objects.requireNonNull(tipo, "tipo obrigatório");
        this.categoria = Objects.requireNonNull(categoria, "categoria obrigatória");

        if (valorCentavos <= 0)
            throw new IllegalArgumentException("valor em centavos deve ser positivo");

        this.valorCentavos = valorCentavos;

        this.metaNome = metaNome == null ? "" : metaNome.trim();

        this.comentario = comentario == null ? "" : comentario.trim();
        this.tags = tags == null
                ? Set.of()
                : tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        this.nome = nome == null ? "" : nome.trim();
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
        this.comentario = comentario == null ? "" : comentario.trim();
    }
}



