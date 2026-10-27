import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** Internal helper: Python supplies the password through stdin, never arguments. */
class HashAdminPassword {
    public static void main(String[] args) throws Exception {
        String password = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        int length = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.isBlank() || length < 8 || length > 72) {
            throw new IllegalArgumentException("Password must contain 8-72 UTF-8 bytes");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        if (!BCrypt.checkpw(password, hash)) {
            throw new IllegalStateException("BCrypt verification failed");
        }
        System.out.print(hash);
    }
}
