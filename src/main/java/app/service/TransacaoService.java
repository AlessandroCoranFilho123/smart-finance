package app.service;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TransacaoService {

    private final List<Transacao> transacoes;
    private final List<Meta> metas;

    public TransacaoService(List<Transacao> transacoes, List<Meta> metas) {
        this.transacoes = Objects.requireNonNull(transacoes);
        this.metas = Objects.requireNonNull(metas);
    }

    public void registrar(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            Set<String> tags
    ) {
        Objects.requireNonNull(tipo, "tipo obrigatório");
        Objects.requireNonNull(categoria, "categoria obrigatória");

        if (valorCentavos <= 0)
            throw new IllegalArgumentException("Valor inválido");

        Set<String> safeTags =
                tags == null
                        ? Set.of()
                        : tags.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet());

        boolean exigeMeta =
                categoria == Categoria.AdicionarMeta || categoria == Categoria.RetirarMeta;

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
                    safeTags,
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
                    safeTags,
                    "Resgate de meta"
            ));
            return;
        }

        transacoes.add(new Transacao(
                tipo,
                categoria,
                valorCentavos,
                null,
                comentario,
                safeTags,
                categoria.name()
        ));
    }

    public void excluirTransacao(int index) {
        if (index < 0 || index >= transacoes.size()) return;

        Transacao t = transacoes.get(index);

        if (t.temMeta()) {
            Meta m = encontrarMeta(t.getMetaNome());
            if (m != null) {
                if (t.getCategoria() == Categoria.AdicionarMeta)
                    m.retirar(t.getValorCentavos());
                else if (t.getCategoria() == Categoria.RetirarMeta)
                    m.adicionar(t.getValorCentavos());
            }
        }
        transacoes.remove(index);
    }

    public void excluirMeta(Meta meta) {
        if (meta == null)
            throw new IllegalArgumentException("Meta inválida");

        long devolvido = meta.getAtualCentavos();

        if (devolvido > 0) {
            transacoes.add(new Transacao(
                    TipoTransacao.Entrada,
                    Categoria.Outros,
                    devolvido,
                    null,
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
                .filter(m -> Objects.equals(m.getNome(), nome))
                .findFirst()
                .orElse(null);
    }
}
