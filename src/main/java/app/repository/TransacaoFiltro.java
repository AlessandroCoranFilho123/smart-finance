package app.repository;

import app.model.Categoria;
import app.model.TipoTransacao;

import java.time.LocalDate;

public record TransacaoFiltro(
        String busca,
        TipoTransacao tipo,
        Categoria categoria,
        LocalDate dataInicio,
        LocalDate dataFim
) {

    public TransacaoFiltro {
        busca = busca == null ? "" : busca.trim();
    }

    public boolean possuiBusca() {
        return !busca.isBlank();
    }
}
