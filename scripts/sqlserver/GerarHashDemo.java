import org.springframework.security.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

class GerarHashDemo {
    public static void main(String[] args) throws java.io.IOException {
        if (args.length != 1) throw new IllegalArgumentException("Informe o arquivo temporário para o hash.");
        var console = System.console();
        if (console == null) throw new IllegalStateException("Abra o programa em um terminal interativo.");
        char[] senha = console.readPassword("Senha das contas descartáveis: ");
        if (senha == null) throw new IllegalStateException("Leitura cancelada.");
        try {
            String texto = new String(senha);
            int bytes = texto.getBytes(StandardCharsets.UTF_8).length;
            if (bytes < 12 || bytes > 72) throw new IllegalArgumentException("Use de 12 a 72 bytes em UTF-8.");
            Files.writeString(Path.of(args[0]), BCrypt.hashpw(texto, BCrypt.gensalt()));
        } finally {
            Arrays.fill(senha, '\0');
        }
    }
}
