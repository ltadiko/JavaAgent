package com.jobagent.jobagent.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds all custom `app.*` properties from application.properties / YAML.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Embedding embedding = new Embedding();
    private Storage storage = new Storage();
    private Encryption encryption = new Encryption();
    private Cors cors = new Cors();

    @Getter @Setter
    public static class Embedding {
        private int dimensions = 768;
    }

    @Getter @Setter
    public static class Storage {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String region = "us-east-1";
        private Bucket bucket = new Bucket();

        @Getter @Setter
        public static class Bucket {
            private String cv = "jobagent-cv";
            private String letters = "jobagent-letters";
        }
    }

    @Getter @Setter
    public static class Encryption {
        private String secretKey;
    }

    @Getter @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }
}
