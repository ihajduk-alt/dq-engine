package io.dqengine.example.http;

import io.dqengine.core.adapter.json.JsonRuleCatalog;
import io.dqengine.core.port.in.ValidationEngine;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@SpringBootApplication(proxyBeanMethods = false)
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

    // Rules are loaded once at startup, from dq.rules-url or else the bundled rules.json, and the app doesn't
    // start without them. A production client would add timeouts and retries
    @Bean
    ValidationEngine validationEngine(@Value("${dq.rules-url:}") String rulesUrl) throws IOException {
        Resource rules = rulesUrl.isEmpty() ? new ClassPathResource("rules.json") : new UrlResource(rulesUrl);
        try (InputStream in = rules.getInputStream()) {
            return ValidationEngine.create(JsonRuleCatalog.read(in, new SpelRuleLogicCompiler()));
        }
    }
}
