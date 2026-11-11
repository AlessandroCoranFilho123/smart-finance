using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

/// <summary>
/// Regras de registro, atualização e exclusão de transações, incluindo o
/// fluxo especial das categorias Aplicar/Resgatar meta. Todas as operações que
/// combinam mais de um comando SQL são executadas atomicamente.
/// O banco é a fonte da verdade: instâncias de Meta vindas da UI são recarregadas
/// antes de qualquer uso (SF-004).
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

        if (!categoria.EhValidaPara(tipo))
        {
            throw new ArgumentException("Categoria incompatível com o tipo de transação");
        }

        Guid carteiraEfetiva = carteiraId ?? carteiraService.CarteiraPadraoId();

        if (categoria == Categoria.AdicionarMeta)
        {
            ArgumentNullException.ThrowIfNull(meta);
            db.EmTransacao(() => RegistrarAplicacaoMeta(meta.Id, valorCentavos, comentario, data, carteiraEfetiva));
            return;
        }

        if (categoria == Categoria.RetirarMeta)
        {
            ArgumentNullException.ThrowIfNull(meta);
            db.EmTransacao(() => RegistrarResgateMeta(meta.Id, valorCentavos, comentario, data, carteiraEfetiva));
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

        if (!categoria.EhValidaPara(tipo))
        {
            throw new ArgumentException("Categoria incompatível com o tipo de transação");
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
            // Falha aqui derruba a transação SQL inteira (nada é alterado).
            ExcluirTransacaoInterna(original);

            if (categoria == Categoria.AdicionarMeta)
            {
                RegistrarAplicacaoMeta(meta!.Id, valorCentavos, comentario, data, carteiraEfetiva, idOriginal);
                return;
            }

            if (categoria == Categoria.RetirarMeta)
            {
                RegistrarResgateMeta(meta!.Id, valorCentavos, comentario, data, carteiraEfetiva, idOriginal);
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

    /// <summary>Recarrega a meta do banco: instâncias vindas da UI podem estar obsoletas (SF-004).</summary>
    private Meta RecarregarMeta(Guid metaId) =>
        metaRepository.BuscarPorId(metaId)
        ?? throw new ArgumentException("Meta não encontrada");

    private void RegistrarAplicacaoMeta(Guid metaId, long valorCentavos, string? comentario, DateOnly data, Guid carteiraId, Guid? id = null)
    {
        Meta meta = RecarregarMeta(metaId);

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

    private void RegistrarResgateMeta(Guid metaId, long valorCentavos, string? comentario, DateOnly data, Guid carteiraId, Guid? id = null)
    {
        Meta meta = RecarregarMeta(metaId);

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
            Meta meta = RecarregarMeta(t.MetaId.Value);

            if (categoriaEfetiva == Categoria.AdicionarMeta)
            {
                // A reversão precisa devolver o valor integral à meta; se parte já
                // foi resgatada, excluir criaria dinheiro do nada (SF-006/D-05).
                if (meta.AtualCentavos < t.ValorCentavos)
                {
                    throw new ArgumentException(
                        "Não é possível excluir: parte deste valor já foi resgatado da meta. Exclua primeiro os resgates mais recentes.");
                }

                meta.Retirar(t.ValorCentavos);
            }
            else
            {
                meta.Adicionar(t.ValorCentavos);
            }

            metaRepository.Atualizar(meta);
        }

        transacaoRepository.Excluir(t.Id);

        // Nenhuma operação pode deixar carteira com saldo negativo (SF-005/D-04):
        // remover uma entrada reduz o saldo da carteira de origem.
        if (t.Tipo == TipoTransacao.Entrada && t.CarteiraId is not null
            && carteiraService.CalcularSaldoCarteiraCentavos(t.CarteiraId.Value) < 0)
        {
            throw new ArgumentException("Operação bloqueada: excluir esta entrada deixaria a carteira com saldo negativo.");
        }
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
