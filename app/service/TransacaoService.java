package app.service;

import app.model.*;

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
            Set<String> tags
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
                    "Exclusão da meta: " + meta.getNome(),
                    Set.of("ajuste"),
                    "Devolução de meta"
            ));
        }

        metas.remove(meta);
    }

    public long calcularSaldoDisponivelCentavos() {
        return transacoes.stream()
                .mapToLong(t ->
                        t.getTipo() == TipoTransacao.Entrada
                                ? t.getValorCentavos()
                                : -t.getValorCentavos()
                ).sum();
    }

    private Meta encontrarMeta(String nome) {
        return metas.stream()
                .filter(m -> m.getNome().equals(nome))
                .findFirst()
                .orElse(null);
    }

    public void editarComentario(Transacao transacao, String novoComentario) {
        if (transacao == null)
            throw new IllegalArgumentException("Transação inválida");

        transacao.setComentario(novoComentario);
    }

}
