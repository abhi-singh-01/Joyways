import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptGen {
    public static void main(String[] args) {
        char[] chars = System.console().readPassword("Enter new admin password: ");
        String password = new String(chars);
        System.out.println(new BCryptPasswordEncoder().encode(password));
    }
}
