import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptCheck {
    public static void main(String[] args) {
        char[] chars = System.console().readPassword("Enter password to verify: ");
        String password = new String(chars);

        String storedHash = System.console().readLine("Paste stored BCrypt hash: ");

        boolean matches = new BCryptPasswordEncoder().matches(password, storedHash);

        System.out.println("Password matches hash: " + matches);
    }
}
