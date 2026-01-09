package app.service;

import app.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

public class TransacaoService {

    private final List<Transacao> transacoes;
    private final List<Meta> metas;

    public TransacaoService(List<Transacao> transacoes, List<Meta> metas) {
        this.transacoes = transacoes;
        this.metas = metas;
    }

    public void registrar(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            Set<String> tags,
            LocalDateTime dataHora
    ) {

        if (valorCentavos <= 0)
            throw new IllegalArgumentException("Valor inválido");

        boolean exigeMeta = categoria == Categoria.AdicionarMeta
                || categoria == Categoria.RetirarMeta;

        if (exigeMeta && meta == null)
            throw new IllegalArgumentException("Selecione uma meta");

        if (categoria == Categoria.AdicionarMeta) {

            long saldo = calcularSaldoDisponivelCentavos();
            if (valorCentavos > saldo)
                throw new IllegalArgumentException("Saldo insuficiente");

            long permitido = Math.min(valorCentavos, meta.restanteParaAlvo());
            if (permitido <= 0)
                throw new IllegalArgumentException("Meta já concluída");

            meta.adicionar(permitido);

            transacoes.add(new Transacao(
                    TipoTransacao.Saida,
                    categoria,
                    permitido,
                    meta.getNome(),
                    comentario,
                    tags,
                    dataHora,
                    "Aplicação em meta"
            ));
            return;
        }

        if (categoria == Categoria.RetirarMeta) {

            long retirado = meta.retirar(valorCentavos);
            if (retirado <= 0)
                throw new IllegalArgumentException("Valor inválido para retirada");

            transacoes.add(new Transacao(
                    TipoTransacao.Entrada,
                    categoria,
                    retirado,
                    meta.getNome(),
                    comentario,
                    tags,
                    dataHora,
                    "Resgate de meta"
            ));
            return;
        }

        transacoes.add(new Transacao(
                tipo,
                categoria,
                valorCentavos,
                "",
                comentario,
                tags,
                dataHora,
                categoria.name()
        ));
    }

    public void excluirTransacao(int index) {

        Transacao t = transacoes.get(index);

        if (t.getCategoria() == Categoria.AdicionarMeta) {
            Meta m = encontrarMeta(t.getMetaNome());
            if (m != null) m.retirar(t.getValorCentavos());
        }

        if (t.getCategoria() == Categoria.RetirarMeta) {
            Meta m = encontrarMeta(t.getMetaNome());
            if (m != null) m.adicionar(t.getValorCentavos());
        }

        transacoes.remove(index);
    }

    public void excluirMeta(Meta meta) {

        long devolvido = meta.getAtualCentavos();

        if (devolvido > 0) {
            transacoes.add(new Transacao(
                    TipoTransacao.Entrada,
                    Categoria.Outros,
                    devolvido,
                    "",
                    "Exclusão de meta: " + meta.getNome(),
                    Set.of("ajuste"),
                    LocalDateTime.now(),
                    "Devolução de meta"
            ));
        }

        metas.remove(meta);
    }

    public long calcularSaldoDisponivelCentavos() {
        return transacoes.stream()
                .filter(t -> !t.temMeta())
                .mapToLong(t ->
                        t.getTipo() == TipoTransacao.Entrada
                                ? t.getValorCentavos()
                                : -t.getValorCentavos()
                ).sum();
    }

    public double gastoMedioMensal() {
        return mediaPorPeriodo(true);
    }

    public double gastoMedioSemanal() {
        return mediaPorPeriodo(false);
    }

    private double mediaPorPeriodo(boolean mensal) {

        Map<Object, Long> mapa = new HashMap<>();

        for (Transacao t : transacoes) {
            if (t.getTipo() != TipoTransacao.Saida) continue;
            if (t.temMeta()) continue;

            LocalDate d = t.getDataHora().toLocalDate();

            Object chave = mensal
                    ? d.getYear() + "-" + d.getMonthValue()
                    : d.get(WeekFields.ISO.weekOfWeekBasedYear());

            mapa.merge(chave, t.getValorCentavos(), Long::sum);
        }

        return mapa.values().stream()
                .mapToLong(v -> v)
                .average()
                .orElse(0) / 100.0;
    }

    private Meta encontrarMeta(String nome) {
        return metas.stream()
                .filter(m -> m.getNome().equals(nome))
                .findFirst()
                .orElse(null);
    }
}
