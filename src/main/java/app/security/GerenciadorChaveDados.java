package app.security;

import javax.crypto.SecretKey;
import java.util.Objects;

public final class GerenciadorChaveDados {

    public static final String NOME_CHAVE_DADOS = "SmartFinance/DataEncryptionKey/v1";

    private final CofreChaves cofre;

    public GerenciadorChaveDados(CofreChaves cofre) {
        this.cofre = Objects.requireNonNull(cofre, "Cofre de chaves e obrigatorio");
    }

    public static GerenciadorChaveDados padrao() {
        return new GerenciadorChaveDados(WindowsCredentialManagerCofre.padrao());
    }

    public SecretKey obterOuCriarChave() {
        return cofre.ler(NOME_CHAVE_DADOS)
                .map(this::chaveArmazenada)
                .orElseGet(this::criarEGuardarChave);
    }

    private SecretKey chaveArmazenada(byte[] segredo) {
        try {
            return CriptografiaDados.chaveDeBytes(segredo);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Chave de dados armazenada esta invalida", e);
        }
    }

    private SecretKey criarEGuardarChave() {
        SecretKey chave = CriptografiaDados.gerarChave();
        cofre.salvar(NOME_CHAVE_DADOS, CriptografiaDados.chaveParaBytes(chave));
        return chave;
    }
}
