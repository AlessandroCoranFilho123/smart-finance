package app.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CriptografiaDados")
class CriptografiaDadosTest {

    @Test
    @DisplayName("criptografa e descriptografa texto com AES-GCM")
    void roundTrip() throws Exception {
        SecretKey chave = CriptografiaDados.gerarChave();
        String texto = "Salario recebido: R$ 5.000,00";

        String cifrado = CriptografiaDados.criptografar(texto, chave);

        assertAll(
                () -> assertNotEquals(texto, cifrado),
                () -> assertEquals(texto, CriptografiaDados.descriptografar(cifrado, chave))
        );
    }

    @Test
    @DisplayName("usa nonce aleatorio para o mesmo texto")
    void nonceAleatorio() throws Exception {
        SecretKey chave = CriptografiaDados.gerarChave();

        String primeiro = CriptografiaDados.criptografar("mesmo texto", chave);
        String segundo = CriptografiaDados.criptografar("mesmo texto", chave);

        assertNotEquals(primeiro, segundo);
    }

    @Test
    @DisplayName("falha ao descriptografar com chave errada")
    void chaveErradaFalha() throws Exception {
        String cifrado = CriptografiaDados.criptografar(
                "dado sensivel",
                CriptografiaDados.gerarChave()
        );

        assertThrows(GeneralSecurityException.class, () ->
                CriptografiaDados.descriptografar(cifrado, CriptografiaDados.gerarChave()));
    }

    @Test
    @DisplayName("serializa chave em Base64 para armazenamento seguro externo")
    void chaveBase64() {
        SecretKey chave = CriptografiaDados.gerarChave();
        String base64 = CriptografiaDados.chaveParaBase64(chave);

        assertArrayEquals(chave.getEncoded(), CriptografiaDados.chaveDeBase64(base64).getEncoded());
    }

    @Test
    @DisplayName("exporta copia defensiva dos bytes da chave")
    void chaveParaBytesCopiaDefensiva() {
        SecretKey chave = CriptografiaDados.gerarChave();

        byte[] bytes = CriptografiaDados.chaveParaBytes(chave);
        bytes[0] = (byte) ~bytes[0];

        assertNotEquals(bytes[0], CriptografiaDados.chaveParaBytes(chave)[0]);
    }

    @Test
    @DisplayName("rejeita chave fraca")
    void rejeitaChaveFraca() {
        SecretKey chaveFraca = new SecretKeySpec(new byte[16], "AES");

        assertThrows(IllegalArgumentException.class, () ->
                CriptografiaDados.criptografar("texto", chaveFraca));
    }
}
