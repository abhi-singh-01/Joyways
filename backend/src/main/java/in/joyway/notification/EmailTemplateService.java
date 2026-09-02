package in.joyway.notification;

import in.joyway.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final Set<String> LAYOUT_RAW_HTML_KEYS = Set.of("content");

    private final MailProperties mailProperties;

    public String render(String templateName, Map<String, String> variables) {
        String layout = loadTemplate("layout.html");
        String body = loadTemplate(templateName + ".html");
        String merged = merge(body, variables, Set.of());
        return merge(layout, Map.of(
                "content", merged,
                "brandName", "JOYWAY",
                "tagline", "Travel made simple.",
                "supportEmail", mailProperties.getSupportFrom(),
                "bookingsEmail", mailProperties.getBookingsFrom(),
                "year", String.valueOf(java.time.Year.now().getValue())
        ), LAYOUT_RAW_HTML_KEYS);
    }

    public String preview(String templateName, Map<String, String> variables) {
        return render(templateName, variables);
    }

    private String loadTemplate(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + fileName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Email template not found: " + fileName);
        }
    }

    private String merge(String template, Map<String, String> variables, Set<String> rawHtmlKeys) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            if (rawHtmlKeys.contains(entry.getKey())) {
                result = result.replace("{{" + entry.getKey() + "}}", value);
            } else {
                result = result.replace("{{" + entry.getKey() + "}}", escapeHtml(value));
            }
        }
        return result;
    }

    static String escapeHtml(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
