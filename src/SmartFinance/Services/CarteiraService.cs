using SmartFinance.Data;
using SmartFinance.Models;

namespace SmartFinance.Services;

public sealed class CarteiraService(CarteiraRepository carteiraRepository, TransacaoRepository transacaoRepository)
{
    public List<Carteira> ListarCarteiras() => carteiraRepository.ListarTodas();

    public Carteira? BuscarPorId(Guid id) => carteiraRepository.BuscarPorId(id);

    public Carteira? CarteiraPadrao() => carteiraRepository.BuscarPorId(Database.CarteiraBancoId);

    public Guid CarteiraPadraoId() => CarteiraPadrao()?.Id ?? Database.CarteiraBancoId;

    public List<CarteiraResumo> ListarResumos()
    {
        return ListarCarteiras()
            .Select(carteira => new CarteiraResumo(carteira, CalcularSaldoCarteiraCentavos(carteira.Id)))
            .ToList();
    }

    public long CalcularSaldoTotalCentavos() => ListarResumos().Sum(r => r.SaldoCentavos);

    public long CalcularSaldoCarteiraCentavos(Guid carteiraId)
    {
        Carteira? carteira = carteiraRepository.BuscarPorId(carteiraId);
        if (carteira is null)
        {
            return 0L;
        }

        long saldoTransacoes = transacaoRepository.CalcularSaldoPorCarteira(carteira.Id);
        long transferenciasEntrada = carteiraRepository.CalcularTransferenciasEntrada(carteira.Id);
        long transferenciasSaida = carteiraRepository.CalcularTransferenciasSaida(carteira.Id);

        return carteira.SaldoInicialCentavos + saldoTransacoes + transferenciasEntrada - transferenciasSaida;
    }

    public void AtualizarSaldoInicial(Guid carteiraId, long saldoInicialCentavos)
    {
        if (saldoInicialCentavos < 0)
        {
            throw new ArgumentException("Saldo inicial não pode ser negativo");
        }

        Carteira carteira = carteiraRepository.BuscarPorId(carteiraId)
            ?? throw new ArgumentException("Carteira não encontrada");

        long saldoSemInicial = CalcularSaldoCarteiraCentavos(carteira.Id) - carteira.SaldoInicialCentavos;
        if (saldoInicialCentavos + saldoSemInicial < 0)
        {
            throw new ArgumentException("Saldo inicial insuficiente para cobrir movimentações da carteira");
        }

        carteiraRepository.AtualizarSaldoInicial(carteiraId, saldoInicialCentavos);
    }

    public void Transferir(
        Guid origemCarteiraId,
        Guid destinoCarteiraId,
        long valorCentavos,
        DateOnly data,
        string comentario)
    {
        if (origemCarteiraId == destinoCarteiraId)
        {
            throw new ArgumentException("Escolha carteiras diferentes");
        }

        if (valorCentavos <= 0)
        {
            throw new ArgumentException("Valor deve ser positivo");
        }

        if (carteiraRepository.BuscarPorId(origemCarteiraId) is null)
        {
            throw new ArgumentException("Carteira de origem não encontrada");
        }

        if (carteiraRepository.BuscarPorId(destinoCarteiraId) is null)
        {
            throw new ArgumentException("Carteira de destino não encontrada");
        }

        if (valorCentavos > CalcularSaldoCarteiraCentavos(origemCarteiraId))
        {
            throw new ArgumentException("Saldo insuficiente na carteira de origem");
        }

        carteiraRepository.InserirTransferencia(new TransferenciaCarteira(
            Guid.NewGuid(),
            origemCarteiraId,
            destinoCarteiraId,
            valorCentavos,
            data,
            comentario?.Trim() ?? string.Empty));
    }
}
