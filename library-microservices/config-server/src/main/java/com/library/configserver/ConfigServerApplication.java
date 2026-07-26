package com.library.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.config.EnableConfigServer;

/**
 * Centralized configuration server.
 *
 * Every other service (book/member/loan/api-gateway) pulls its config from
 * here at startup via "spring.config.import: optional:configserver:http://..."
 * instead of relying solely on its own local application.yml + env vars.
 *
 * Backed by "native" mode here (a local folder, config-repo/) rather than a
 * git repo, purely to keep the demo self-contained with no external git
 * dependency. In a real production setup you'd point this at a git URL
 * (spring.cloud.config.server.git.uri) so config changes are versioned,
 * reviewable, and auditable just like application code.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
