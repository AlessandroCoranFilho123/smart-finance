package app.model;

import java.time.LocalDate;
import java.util.UUID;

public record Transacao(
        UUID id,
        String descricao,
        long valorCentavos,
        TipoTransacao tipo,
        LocalDate data,
        UUID metaId,
        Categoria categoria
) {
    public Transacao {
        if (id == null) id = UUID.randomUUID();
    }

}
