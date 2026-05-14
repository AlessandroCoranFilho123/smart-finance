package app.security;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WindowsCredentialManagerCofre implements CofreChaves {

    private static final int CRED_TYPE_GENERIC = 1;
    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;
    private static final int ERROR_NOT_FOUND = 1168;
    private static final int MAX_CREDENTIAL_BLOB_BYTES = 2560;
    private static final String USUARIO_CREDENCIAL = "SmartFinance";

    public static WindowsCredentialManagerCofre padrao() {
        if (!suportado()) {
            throw new UnsupportedOperationException("Credential Manager esta disponivel apenas no Windows");
        }
        return new WindowsCredentialManagerCofre();
    }

    public static boolean suportado() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Override
    public Optional<byte[]> ler(String nome) {
        validarNome(nome);

        PointerByReference credencialRef = new PointerByReference();
        boolean ok = api().CredRead(nome, CRED_TYPE_GENERIC, 0, credencialRef);
        if (!ok) {
            int erro = Native.getLastError();
            if (erro == ERROR_NOT_FOUND) {
                return Optional.empty();
            }
            throw erroWindows("ler credencial", erro);
        }

        Pointer pointer = credencialRef.getValue();
        try {
            Credential credencial = new Credential(pointer);
            byte[] segredo = credencial.CredentialBlobSize == 0
                    ? new byte[0]
                    : credencial.CredentialBlob.getByteArray(0, credencial.CredentialBlobSize);
            return Optional.of(segredo);
        } finally {
            api().CredFree(pointer);
        }
    }

    @Override
    public void salvar(String nome, byte[] segredo) {
        validarNome(nome);
        Objects.requireNonNull(segredo, "Segredo e obrigatorio");
        if (segredo.length == 0 || segredo.length > MAX_CREDENTIAL_BLOB_BYTES) {
            throw new IllegalArgumentException("Segredo deve ter entre 1 e 2560 bytes");
        }

        Memory blob = new Memory(segredo.length);
        blob.write(0, segredo, 0, segredo.length);

        Credential credencial = new Credential();
        credencial.Flags = 0;
        credencial.Type = CRED_TYPE_GENERIC;
        credencial.TargetName = new WString(nome);
        credencial.Comment = new WString("SmartFinance local data encryption key");
        credencial.LastWritten = new WinBase.FILETIME();
        credencial.CredentialBlobSize = segredo.length;
        credencial.CredentialBlob = blob;
        credencial.Persist = CRED_PERSIST_LOCAL_MACHINE;
        credencial.AttributeCount = 0;
        credencial.Attributes = Pointer.NULL;
        credencial.TargetAlias = null;
        credencial.UserName = new WString(USUARIO_CREDENCIAL);
        credencial.write();

        boolean ok = api().CredWrite(credencial, 0);
        if (!ok) {
            throw erroWindows("salvar credencial", Native.getLastError());
        }
    }

    @Override
    public void apagar(String nome) {
        validarNome(nome);

        boolean ok = api().CredDelete(nome, CRED_TYPE_GENERIC, 0);
        if (!ok) {
            int erro = Native.getLastError();
            if (erro != ERROR_NOT_FOUND) {
                throw erroWindows("apagar credencial", erro);
            }
        }
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da credencial e obrigatorio");
        }
    }

    private RuntimeException erroWindows(String operacao, int codigo) {
        return new IllegalStateException(
                "Erro ao " + operacao + " no Windows Credential Manager (codigo "
                        + codigo + "): " + Kernel32Util.formatMessageFromLastErrorCode(codigo)
        );
    }

    private WindowsCredentialApi api() {
        return ApiHolder.API;
    }

    private static final class ApiHolder {
        private static final WindowsCredentialApi API = Native.load(
                "Advapi32",
                WindowsCredentialApi.class,
                W32APIOptions.UNICODE_OPTIONS
        );
    }

    private interface WindowsCredentialApi extends StdCallLibrary {
        boolean CredRead(String targetName, int type, int flags, PointerByReference credential);

        boolean CredWrite(Credential credential, int flags);

        boolean CredDelete(String targetName, int type, int flags);

        void CredFree(Pointer buffer);
    }

    @Structure.FieldOrder({
            "Flags",
            "Type",
            "TargetName",
            "Comment",
            "LastWritten",
            "CredentialBlobSize",
            "CredentialBlob",
            "Persist",
            "AttributeCount",
            "Attributes",
            "TargetAlias",
            "UserName"
    })
    public static class Credential extends Structure {
        public int Flags;
        public int Type;
        public WString TargetName;
        public WString Comment;
        public WinBase.FILETIME LastWritten;
        public int CredentialBlobSize;
        public Pointer CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public Pointer Attributes;
        public WString TargetAlias;
        public WString UserName;

        public Credential() {
            super();
        }

        public Credential(Pointer pointer) {
            super(pointer);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of(
                    "Flags",
                    "Type",
                    "TargetName",
                    "Comment",
                    "LastWritten",
                    "CredentialBlobSize",
                    "CredentialBlob",
                    "Persist",
                    "AttributeCount",
                    "Attributes",
                    "TargetAlias",
                    "UserName"
            );
        }
    }
}
