using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

/// <summary>
/// Regras de registro, atualização e exclusão de transações, incluindo o
/// fluxo especial das categorias Aplicar/Resgatar meta. Todas as operações que
/// combinam mais de um comando SQL são executadas atomicamente.
/// </summary>
public sealed class TransacaoService(
    Database db,
    TransacaoRepository transacaoRepository,
    MetaRepository metaRepository,
    CarteiraService carteiraService)
{
    public void Registrar(
        TipoTransacao tipo,
        Categoria categoria,
        long valorCentavos,
        Meta? meta,
        string comentario,
        DateOnly data,
        Guid? carteiraId = null)
    {
        if (valorCentavos <= 0)
        {
            throw new ArgumentException("Valores devem ser positivos");
        }

        Guid carteiraEfetiva = carteiraId ?? carteiraService.CarteiraPadraoId();

        if (categoria == Categoria.AdicionarMeta)
        {
            ArgumentNullException.ThrowIfNull(meta);
            db.EmTransacao(() => RegistrarAplicacaoMeta(meta, valorCentavos, comentario, data, carteiraEfetiva));
            return;
        }

        if (categoria == Categoria.RetirarMeta)
        {
            ArgumentNullException.ThrowIfNull(meta);
            db.EmTransacao(() => RegistrarResgateMeta(meta, valorCentavos, comentario, data, carteiraEfetiva));
            return;
        }

        ValidarSaldoSuficienteParaSaida(tipo, valorCentavos, carteiraEfetiva);

        transacaoRepository.Inserir(new Transacao(
            Guid.NewGuid(),
            categoria.NomeExibicao(),
            comentario?.Trim() ?? string.Empty,
            valorCentavos,
            tipo,
            data,
            null,
            categoria,
            carteiraEfetiva));
    }

    /// <summary>Atualiza preservando o ID original; falha de validação não altera nada (rollback).</summary>
    public void Atualizar(
        Guid idOriginal,
        TipoTransacao tipo,
        Categoria categoria,
        long valorCentavos,
        Meta? meta,
        string comentario,
        DateOnly data,
        Guid? carteiraId = null)
    {
        if (valorCentavos <= 0)
        {
            throw new ArgumentException("Valores devem ser positivos");
        }

        if (categoria is Categoria.AdicionarMeta or Categoria.RetirarMeta)
        {
            ArgumentNullException.ThrowIfNull(meta);
        }

        Transacao original = transacaoRepository.BuscarPorId(idOriginal)
            ?? throw new ArgumentException("Transação original não encontrada");

        Guid carteiraEfetiva = carteiraId ?? original.CarteiraId ?? Database.CarteiraBancoId;

        db.EmTransacao(() =>
        {
            // Remove a original (revertendo a meta quando aplicável) antes de
            // validar o saldo, para não contar duas vezes a mesma movimentação.
            ExcluirTransacaoInterna(original);

            if (categoria == Categoria.AdicionarMeta)
            {
                RegistrarAplicacaoMeta(meta!, valorCentavos, comentario, data, carteiraEfetiva, idOriginal);
                return;
            }

            if (categoria == Categoria.RetirarMeta)
            {
                RegistrarResgateMeta(meta!, valorCentavos, comentario, data, carteiraEfetiva, idOriginal);
                return;
            }

            ValidarSaldoSuficienteParaSaida(tipo, valorCentavos, carteiraEfetiva);

            transacaoRepository.Inserir(new Transacao(
                idOriginal,
                categoria.NomeExibicao(),
                comentario?.Trim() ?? string.Empty,
                valorCentavos,
                tipo,
                data,
                null,
                categoria,
                carteiraEfetiva));
        });
    }

    public void ExcluirTransacao(Guid transacaoId, Categoria? categoria = null)
    {
        Transacao? t = transacaoRepository.BuscarPorId(transacaoId);
        if (t is null)
        {
            return;
        }

        db.EmTransacao(() => ExcluirTransacaoInterna(t, categoria));
    }

    public long CalcularSaldoDisponivelCentavos() => carteiraService.CalcularSaldoTotalCentavos();

    public List<Transacao> ListarRecentes(int limite) => transacaoRepository.ListarRecentes(limite);

    public List<Transacao> ListarPorFiltro(TransacaoFiltro filtro) => transacaoRepository.ListarPorFiltro(filtro);

    public long CalcularReceitasMes(DateOnly primeiroDiaDoMes) =>
        transacaoRepository.CalcularTotalPorTipoEMes(TipoTransacao.Entrada, primeiroDiaDoMes);

    public long CalcularDespesasMes(DateOnly primeiroDiaDoMes) =>
        transacaoRepository.CalcularTotalPorTipoEMes(TipoTransacao.Saida, primeiroDiaDoMes);

    private void RegistrarAplicacaoMeta(Meta meta, long valorCentavos, string? comentario, DateOnly data, Guid carteiraId, Guid? id = null)
    {
        long permitido = Math.Min(valorCentavos, meta.RestanteParaAlvo);
        if (permitido <= 0)
        {
            throw new ArgumentException("Meta já alcançada");
        }

        ValidarSaldoSuficienteParaSaida(TipoTransacao.Saida, permitido, carteiraId);

        meta.Adicionar(permitido);
        metaRepository.Atualizar(meta);

        transacaoRepository.Inserir(new Transacao(
            id ?? Guid.NewGuid(),
            $"Aplicação em meta: {meta.Nome}",
            comentario?.Trim() ?? string.Empty,
            permitido,
            TipoTransacao.Saida,
            data,
            meta.Id,
            Categoria.AdicionarMeta,
            carteiraId));
    }

    private void RegistrarResgateMeta(Meta meta, long valorCentavos, string? comentario, DateOnly data, Guid carteiraId, Guid? id = null)
    {
        long retirado = meta.Retirar(valorCentavos);
        if (retirado <= 0)
        {
            throw new ArgumentException("Valor deve ser maior que 0");
        }

        metaRepository.Atualizar(meta);

        transacaoRepository.Inserir(new Transacao(
            id ?? Guid.NewGuid(),
            $"Resgate de meta: {meta.Nome}",
            comentario?.Trim() ?? string.Empty,
            retirado,
            TipoTransacao.Entrada,
            data,
            meta.Id,
            Categoria.RetirarMeta,
            carteiraId));
    }

    private void ExcluirTransacaoInterna(Transacao t, Categoria? categoriaArgumento = null)
    {
        Categoria? categoriaEfetiva = t.Categoria ?? categoriaArgumento;

        if (t.MetaId is not null && categoriaEfetiva is Categoria.AdicionarMeta or Categoria.RetirarMeta)
        {
            Meta? meta = metaRepository.BuscarPorId(t.MetaId.Value);
            if (meta is not null)
            {
                if (categoriaEfetiva == Categoria.AdicionarMeta)
                {
                    meta.Retirar(t.ValorCentavos);
                }
                else
                {
                    meta.Adicionar(t.ValorCentavos);
                }

                metaRepository.Atualizar(meta);
            }
        }

        transacaoRepository.Excluir(t.Id);
    }

    private void ValidarSaldoSuficienteParaSaida(TipoTransacao tipo, long valorCentavos, Guid carteiraId)
    {
        if (tipo != TipoTransacao.Saida)
        {
            return;
        }

        long saldo = carteiraService.CalcularSaldoCarteiraCentavos(carteiraId);
        if (valorCentavos > saldo)
        {
            throw new ArgumentException("Saldo insuficiente");
        }
    }
}
