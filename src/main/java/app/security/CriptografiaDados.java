package app.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public final class CriptografiaDados {

    private static final byte VERSAO_ATUAL = 1;
    private static final String ALGORITMO_CHAVE = "AES";
    private static final String ALGORITMO_CIPHER = "AES/GCM/NoPadding";
    private static final int TAMANHO_CHAVE_BYTES = 32;
    private static final int TAMANHO_NONCE_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CriptografiaDados() {
    }

    public static SecretKey gerarChave() {
        byte[] chave = new byte[TAMANHO_CHAVE_BYTES];
        RANDOM.nextBytes(chave);
        return new SecretKeySpec(chave, ALGORITMO_CHAVE);
    }

    public static SecretKey chaveDeBase64(String base64) {
        Objects.requireNonNull(base64, "Chave em Base64 e obrigatoria");
        return chaveDeBytes(Base64.getDecoder().decode(base64));
    }

    public static SecretKey chaveDeBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "Bytes da chave sao obrigatorios");
        if (bytes.length != TAMANHO_CHAVE_BYTES) {
            throw new IllegalArgumentException("Chave AES-256 deve ter 32 bytes");
        }
        return new SecretKeySpec(Arrays.copyOf(bytes, bytes.length), ALGORITMO_CHAVE);
    }

    public static String chaveParaBase64(SecretKey chave) {
        return Base64.getEncoder().encodeToString(chaveParaBytes(chave));
    }

    public static byte[] chaveParaBytes(SecretKey chave) {
        validarChave(chave);
        byte[] bytes = chave.getEncoded();
        return Arrays.copyOf(bytes, bytes.length);
    }

    public static String criptografar(String texto, SecretKey chave) throws GeneralSecurityException {
        Objects.requireNonNull(texto, "Texto e obrigatorio");
        validarChave(chave);

        byte[] nonce = new byte[TAMANHO_NONCE_BYTES];
        RANDOM.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITMO_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, nonce));
        byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

        ByteBuffer payload = ByteBuffer.allocate(1 + nonce.length + cifrado.length);
        payload.put(VERSAO_ATUAL);
        payload.put(nonce);
        payload.put(cifrado);
        return Base64.getEncoder().encodeToString(payload.array());
    }

    public static String descriptografar(String payloadBase64, SecretKey chave) throws GeneralSecurityException {
        Objects.requireNonNull(payloadBase64, "Payload e obrigatorio");
        validarChave(chave);

        byte[] payload = Base64.getDecoder().decode(payloadBase64);
        if (payload.length <= 1 + TAMANHO_NONCE_BYTES) {
            throw new IllegalArgumentException("Payload criptografado invalido");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte versao = buffer.get();
        if (versao != VERSAO_ATUAL) {
            throw new IllegalArgumentException("Versao de criptografia nao suportada");
        }

        byte[] nonce = new byte[TAMANHO_NONCE_BYTES];
        buffer.get(nonce);
        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);

        Cipher cipher = Cipher.getInstance(ALGORITMO_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, nonce));
        return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
    }

    private static void validarChave(SecretKey chave) {
        Objects.requireNonNull(chave, "Chave e obrigatoria");
        byte[] bytes = chave.getEncoded();
        if (!ALGORITMO_CHAVE.equalsIgnoreCase(chave.getAlgorithm())
                || bytes == null
                || bytes.length != TAMANHO_CHAVE_BYTES) {
            throw new IllegalArgumentException("Use uma chave AES-256 exportavel");
        }
    }
}
