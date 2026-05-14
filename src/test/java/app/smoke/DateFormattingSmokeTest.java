package app.smoke;

import app.controller.TransacaoDialogController;
import app.database.Database;
import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.support.FxTestSupport;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Smoke - formatação de datas nas telas")
class DateFormattingSmokeTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initJavaFx() {
        FxTestSupport.initToolkit();
    }

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("janela de nova transação exibe data no formato dd/MM/yyyy")
    void janelaNovaTransacaoExibeDataNoFormatoBrasileiro() {
        LoadedView view = carregarView("/app/view/transacao_dialog.fxml", tempDir.resolve("nova-transacao.db"));
        DatePicker dateTransacao = obterDatePicker(view.loader(), "dateTransacao");

        FxTestSupport.runOnFxThreadAndWait(() -> dateTransacao.setValue(LocalDate.of(2026, 3, 30)));

        assertAll(
                () -> assertNotNull(dateTransacao.getValue()),
                () -> assertEquals("30/03/2026", textoRenderizado(dateTransacao)),
                () -> assertEquals(LocalDate.of(2026, 3, 30),
                        dateTransacao.getConverter().fromString("30/03/2026"))
        );
    }

    @Test
    @DisplayName("detalhes da transação mantêm a data formatada ao carregar edição")
    void detalhesDaTransacaoMantemDataFormatadaAoEditar() {
        LoadedView view = carregarView("/app/view/transacao_dialog.fxml", tempDir.resolve("detalhes-transacao.db"));
        TransacaoDialogController controller = view.loader().getController();
        DatePicker dateTransacao = obterDatePicker(view.loader(), "dateTransacao");
        Button btnSalvar = (Button) view.loader().getNamespace().get("btnSalvar");
        Button btnExcluir = (Button) view.loader().getNamespace().get("btnExcluir");

        Transacao transacao = new Transacao(
                UUID.randomUUID(),
                "Mercado",
                "compras do mês",
                350_00L,
                TipoTransacao.Saida,
                LocalDate.of(2026, 3, 30),
                null,
                Categoria.Alimentacao
        );

        FxTestSupport.runOnFxThreadAndWait(() -> controller.configurarParaEditar(transacao));

        assertAll(
                () -> assertEquals("30/03/2026", textoRenderizado(dateTransacao)),
                () -> assertEquals("Atualizar", FxTestSupport.callOnFxThreadAndWait(btnSalvar::getText)),
                () -> assertTrue(FxTestSupport.callOnFxThreadAndWait(btnExcluir::isVisible))
        );
    }

    @Test
    @DisplayName("janela de transações exibe filtros de data no formato dd/MM/yyyy")
    void janelaTransacoesExibeFiltrosDeDataNoFormatoBrasileiro() {
        LoadedView view = carregarView("/app/view/transacoes_view.fxml", tempDir.resolve("transacoes-view.db"));
        DatePicker dateInicio = obterDatePicker(view.loader(), "dateInicio");
        DatePicker dateFim = obterDatePicker(view.loader(), "dateFim");

        FxTestSupport.runOnFxThreadAndWait(() -> {
            dateInicio.setValue(LocalDate.of(2026, 3, 1));
            dateFim.setValue(LocalDate.of(2026, 3, 31));
        });

        assertAll(
                () -> assertEquals("01/03/2026", textoRenderizado(dateInicio)),
                () -> assertEquals("31/03/2026", textoRenderizado(dateFim)),
                () -> assertEquals(LocalDate.of(2026, 3, 31),
                        dateFim.getConverter().fromString("31/03/2026"))
        );
    }

    private LoadedView carregarView(String resourcePath, Path dbPath) {
        Database.overrideUrlForTests(jdbcUrl(dbPath));
        Database.inicializar();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
        Parent root = FxTestSupport.callOnFxThreadAndWait(loader::load);
        Scene scene = FxTestSupport.callOnFxThreadAndWait(() -> {
            Scene localScene = new Scene(root);
            root.applyCss();
            root.layout();
            return localScene;
        });
        return new LoadedView(loader, root, scene);
    }

    private static DatePicker obterDatePicker(FXMLLoader loader, String fxId) {
        return (DatePicker) loader.getNamespace().get(fxId);
    }

    private static String textoRenderizado(DatePicker datePicker) {
        return FxTestSupport.callOnFxThreadAndWait(() -> {
            if (datePicker.getScene() != null) {
                datePicker.getScene().getRoot().applyCss();
                datePicker.getScene().getRoot().layout();
            }
            return datePicker.getEditor().getText();
        });
    }

    private static String jdbcUrl(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    private record LoadedView(FXMLLoader loader, Parent root, Scene scene) {
    }
}
