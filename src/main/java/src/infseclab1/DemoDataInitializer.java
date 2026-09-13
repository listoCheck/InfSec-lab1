package src.infseclab1;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {
    @Bean
    CommandLineRunner seedDemoData(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return args -> {
            Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (users != null && users == 0) {
                jdbc.update("INSERT INTO users(username, password_hash) VALUES (?, ?)",
                        "student", encoder.encode("SecurePass123!"));
                jdbc.update("INSERT INTO posts(author, content) VALUES (?, ?)", "system", "Welcome to the secure API");
            }
        };
    }
}
