package app.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.WINDOWS)
@DisplayName("WindowsCredentialManagerCofre")
class WindowsCredentialManagerCofreTest {

    private final WindowsCredentialManagerCofre cofre = WindowsCredentialManagerCofre.padrao();
    private final String nome = "SmartFinance/Test/" + UUID.randomUUID();

    @AfterEach
    void limpar() {
        cofre.apagar(nome);
    }

    @Test
    @DisplayName("salva, le e apaga segredo no Credential Manager")
    void salvaLeApaga() {
        byte[] segredo = "segredo temporario".getBytes(StandardCharsets.UTF_8);

        cofre.salvar(nome, segredo);

        assertAll(
                () -> assertArrayEquals(segredo, cofre.ler(nome).orElseThrow()),
                () -> {
                    cofre.apagar(nome);
                    assertTrue(cofre.ler(nome).isEmpty());
                }
        );
    }

    @Test
    @DisplayName("rejeita segredo vazio")
    void rejeitaSegredoVazio() {
        assertThrows(IllegalArgumentException.class, () -> cofre.salvar(nome, new byte[0]));
    }
}
