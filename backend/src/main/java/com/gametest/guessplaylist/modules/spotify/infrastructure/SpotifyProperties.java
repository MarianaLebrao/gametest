package com.gametest.guessplaylist.modules.spotify.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spotify")
public record SpotifyProperties(
    String clientId,
    String clientSecret,
    String accountsBaseUrl,
    String apiBaseUrl,
    String redirectUri) {}
