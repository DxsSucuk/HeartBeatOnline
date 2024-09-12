package de.presti.heartmybeatonline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Setting up CORS");
        String allowedDomain = "https://heart.presti.me";
        log.info("Allowed Domain: " + allowedDomain);
        registry.addMapping("/**").allowedOriginPatterns(allowedDomain);
    }


}