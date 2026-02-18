package app.model;

import java.time.LocalDate;
import java.util.UUID;

public class Transacao {

    private final UUID id;
    private final String descricao;
    private final long valorCentavos;
    private final TipoTransacao tipo;
    private final LocalDate data;
    private final UUID metaId;
    private final Categoria categoria;

    public Transacao(
            UUID id,
            String descricao,
            long valorCentavos,
            TipoTransacao tipo,
            LocalDate data,
            UUID metaId,
            Categoria categoria
    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.descricao = descricao;
        this.valorCentavos = valorCentavos;
        this.tipo = tipo;
        this.data = data;
        this.metaId = metaId;
        this.categoria = categoria;
    }

    public UUID getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public LocalDate getData() {
        return data;
    }

    public UUID getMetaId() {
        return metaId;
    }
    
    public Categoria getCategoria() {
        return categoria;
    }
}
