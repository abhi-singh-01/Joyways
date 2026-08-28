package in.joyway;

import in.joyway.config.AdminBootstrapProperties;
import in.joyway.config.CorsProperties;
import in.joyway.config.JwtProperties;
import in.joyway.config.MailProperties;
import in.joyway.config.NotificationProperties;
import in.joyway.config.RateLimitProperties;
import in.joyway.config.RazorpayProperties;
import in.joyway.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        CorsProperties.class,
        JwtProperties.class,
        RazorpayProperties.class,
        AdminBootstrapProperties.class,
        StorageProperties.class,
        MailProperties.class,
        NotificationProperties.class,
        RateLimitProperties.class
})
public class JoywayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoywayApplication.class, args);
    }
}
