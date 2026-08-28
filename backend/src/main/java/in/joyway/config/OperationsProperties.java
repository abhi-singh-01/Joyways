package in.joyway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "joyway.operations")
@Getter
@Setter
public class OperationsProperties {

    private boolean exposeDriverContactToCustomers = false;
}
