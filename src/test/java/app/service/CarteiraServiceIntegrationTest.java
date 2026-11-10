package app.service;

import app.database.Database;
import app.model.Categoria;
import app.model.TipoTransacao;
import app.repository.CarteiraDAO;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CarteiraService - integração")
class CarteiraServiceIntegrationTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("saldo inicial compõe saldo disponível sem entrar como receita")
    void saldoInicialNaoEntraComoReceita() {
        prepararBanco("saldo-inicial.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoService transacaoService = new TransacaoService(
                new TransacaoDAO(),
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 1500_00L);

        assertAll(
                () -> assertEquals(1500_00L, transacaoService.calcularSaldoDisponivelCentavos()),
                () -> assertEquals(0L, transacaoService.calcularReceitasMes(YearMonth.now()))
        );
    }

    @Test
    @DisplayName("transação afeta somente a carteira escolhida")
    void transacaoAfetaCarteiraEscolhida() {
        prepararBanco("transacao-carteira.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoService transacaoService = new TransacaoService(
                new TransacaoDAO(),
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 1000_00L);
        carteiraService.atualizarSaldoInicial(CarteiraDAO.DINHEIRO_FISICO_ID, 300_00L);
        transacaoService.registrar(
                TipoTransacao.Saida,
                Categoria.Alimentacao,
                50_00L,
                null,
                "padaria",
                LocalDate.now(),
                CarteiraDAO.DINHEIRO_FISICO_ID
        );

        assertAll(
                () -> assertEquals(1000_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID)),
                () -> assertEquals(250_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.DINHEIRO_FISICO_ID)),
                () -> assertEquals(1250_00L, carteiraService.calcularSaldoTotalCentavos())
        );
    }

    @Test
    @DisplayName("transferência muda carteiras sem criar transação")
    void transferenciaNaoCriaTransacao() {
        prepararBanco("transferencia.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoDAO transacaoDAO = new TransacaoDAO();

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 1000_00L);
        carteiraService.transferir(
                CarteiraDAO.BANCO_ID,
                CarteiraDAO.DINHEIRO_FISICO_ID,
                200_00L,
                LocalDate.now(),
                "saque"
        );

        assertAll(
                () -> assertEquals(800_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID)),
                () -> assertEquals(200_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.DINHEIRO_FISICO_ID)),
                () -> assertEquals(1000_00L, carteiraService.calcularSaldoTotalCentavos()),
                () -> assertTrue(transacaoDAO.listarTodas().isEmpty())
        );
    }

    @Test
    @DisplayName("saída comum não pode deixar carteira negativa")
    void saidaComumNaoPodeDeixarCarteiraNegativa() {
        prepararBanco("saida-negativa.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        TransacaoService transacaoService = new TransacaoService(
                transacaoDAO,
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.DINHEIRO_FISICO_ID, 10_00L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transacaoService.registrar(
                        TipoTransacao.Saida,
                        Categoria.Alimentacao,
                        20_00L,
                        null,
                        "mercado",
                        LocalDate.now(),
                        CarteiraDAO.DINHEIRO_FISICO_ID
                ));

        assertAll(
                () -> assertEquals("Saldo insuficiente", exception.getMessage()),
                () -> assertTrue(transacaoDAO.listarTodas().isEmpty()),
                () -> assertEquals(10_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.DINHEIRO_FISICO_ID))
        );
    }

    @Test
    @DisplayName("atualização não pode transformar saída em saldo negativo")
    void atualizacaoNaoPodeTransformarSaidaEmSaldoNegativo() {
        prepararBanco("atualizacao-saida-negativa.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        TransacaoService transacaoService = new TransacaoService(
                transacaoDAO,
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 100_00L);
        transacaoService.registrar(
                TipoTransacao.Saida,
                Categoria.Aluguel,
                20_00L,
                null,
                "original",
                LocalDate.now(),
                CarteiraDAO.BANCO_ID
        );
        var original = transacaoDAO.listarTodas().getFirst();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                transacaoService.atualizar(
                        original.id(),
                        TipoTransacao.Saida,
                        Categoria.Aluguel,
                        120_00L,
                        null,
                        "reajuste",
                        LocalDate.now(),
                        CarteiraDAO.BANCO_ID
                ));

        assertAll(
                () -> assertEquals("Saldo insuficiente", exception.getMessage()),
                () -> assertEquals(20_00L, transacaoDAO.buscarPorId(original.id()).valorCentavos()),
                () -> assertEquals(80_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID))
        );
    }

    @Test
    @DisplayName("saldo inicial não pode deixar carteira negativa")
    void saldoInicialNaoPodeDeixarCarteiraNegativa() {
        prepararBanco("saldo-inicial-negativo.db");
        CarteiraService carteiraService = new CarteiraService();
        TransacaoService transacaoService = new TransacaoService(
                new TransacaoDAO(),
                new MetaDAO(),
                carteiraService
        );

        carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 100_00L);
        transacaoService.registrar(
                TipoTransacao.Saida,
                Categoria.Aluguel,
                80_00L,
                null,
                "aluguel",
                LocalDate.now(),
                CarteiraDAO.BANCO_ID
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, 50_00L));

        assertAll(
                () -> assertEquals("Saldo inicial insuficiente para cobrir movimentações da carteira", exception.getMessage()),
                () -> assertEquals(20_00L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID))
        );
    }

    @Test
    @DisplayName("saldo inicial negativo é recusado")
    void saldoInicialNegativoEhRecusado() {
        prepararBanco("saldo-inicial-menor-que-zero.db");
        CarteiraService carteiraService = new CarteiraService();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                carteiraService.atualizarSaldoInicial(CarteiraDAO.BANCO_ID, -1L));

        assertAll(
                () -> assertEquals("Saldo inicial não pode ser negativo", exception.getMessage()),
                () -> assertEquals(0L, carteiraService.calcularSaldoCarteiraCentavos(CarteiraDAO.BANCO_ID))
        );
    }

    private void prepararBanco(String nomeArquivo) {
        Database.overrideUrlForTests("jdbc:sqlite:" + tempDir.resolve(nomeArquivo).toAbsolutePath());
        Database.inicializar();
    }
}
