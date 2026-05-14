package app.security;

import java.util.Optional;

public interface CofreChaves {

    Optional<byte[]> ler(String nome);

    void salvar(String nome, byte[] segredo);

    void apagar(String nome);
}
