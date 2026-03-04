package app.service;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public class TransacaoService {

    private final TransacaoDAO transacaoDAO;
    private final MetaDAO metaDAO;

    public TransacaoService(TransacaoDAO transacaoDAO, MetaDAO metaDAO) {
        this.transacaoDAO = Objects.requireNonNull(transacaoDAO);
        this.metaDAO = Objects.requireNonNull(metaDAO);
    }

    public void registrar(
            TipoTransacao tipo,
            Categoria categoria,
            long valorCentavos,
            Meta meta,
            String comentario,
            LocalDate data) {

        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(categoria, "categoria é obrigatória");

        if (valorCentavos <= 0)
            throw new IllegalArgumentException("Valores devem ser positivos");

        boolean exigeMeta =
                categoria == Categoria.AdicionarMeta ||
                        categoria == Categoria.RetirarMeta;

        if (exigeMeta && meta == null)
            throw new IllegalArgumentException("Selecione uma meta");

        if (categoria == Categoria.AdicionarMeta) {

            long saldo = calcularSaldoDisponivelCentavos();
            if (valorCentavos > saldo)
                throw new IllegalArgumentException("Saldo insuficiente");

            long permitido = Math.min(valorCentavos, meta.restanteParaAlvo());
            if (permitido <= 0)
                throw new IllegalArgumentException("Meta já alcançada");

            meta.adicionar(permitido);
            metaDAO.atualizar(meta);

            Transacao t = new Transacao(
                    UUID.randomUUID(),
                    "Aplicação em meta: " + meta.getNome(),
                    permitido,
                    TipoTransacao.Saida,
                    LocalDate.now(),
                    meta.getId(),
                    Categoria.AdicionarMeta
            );


            transacaoDAO.inserir(t);
            return;
        }

        if (categoria == Categoria.RetirarMeta) {

            long retirado = meta.retirar(valorCentavos);
            if (retirado <= 0)
                throw new IllegalArgumentException("Valor deve ser maior que 0");

            metaDAO.atualizar(meta);

            Transacao t = new Transacao(
                    UUID.randomUUID(),
                    "Resgate de meta: " + meta.getNome(),
                    retirado,
                    TipoTransacao.Entrada,
                    LocalDate.now(),
                    meta.getId(),
                    Categoria.RetirarMeta
            );

            transacaoDAO.inserir(t);
            return;
        }

        Transacao t = new Transacao(
                UUID.randomUUID(),
                comentario,
                valorCentavos,
                tipo,
                LocalDate.now(),
                null,
                categoria
        );

        transacaoDAO.inserir(t);
    }

    public long calcularSaldoDisponivelCentavos() {
        return transacaoDAO.calcularSaldo();
    }

    public long calcularReceitasMes(YearMonth mes) {
        return transacaoDAO.calcularTotalPorTipoEMes(
                TipoTransacao.Entrada,
                mes.toString()
        );
    }

    public long calcularDespesasMes(YearMonth mes) {
        return transacaoDAO.calcularTotalPorTipoEMes(
                TipoTransacao.Saida,
                mes.toString()
        );
    }

    public void excluirTransacao(UUID transacaoId, Categoria categoria) {

        Transacao t = transacaoDAO.buscarPorId(transacaoId);
        if (t == null) return;

        if (categoria == Categoria.AdicionarMeta && t.metaId() != null) {
            Meta meta = metaDAO.buscarPorId(t.metaId());
            if (meta != null) {
                meta.retirar(t.valorCentavos());
                metaDAO.atualizar(meta);
            }
        }

        if (categoria == Categoria.RetirarMeta && t.metaId() != null) {
            Meta meta = metaDAO.buscarPorId(t.metaId());
            if (meta != null) {
                meta.adicionar(t.valorCentavos());
                metaDAO.atualizar(meta);
            }
        }

        transacaoDAO.excluir(transacaoId);
    }
}
