package com.gametest.guessplaylist.shared.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final List<String> allowedOrigins;

  public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOriginsRaw) {
    this.allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isBlank())
        .toList();
  }

  @Override
  @SuppressWarnings("null")
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    String[] origins = allowedOrigins.stream()
        .map(Objects::requireNonNull)
        .toArray(String[]::new);

    registry.addMapping("/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
  }
}
