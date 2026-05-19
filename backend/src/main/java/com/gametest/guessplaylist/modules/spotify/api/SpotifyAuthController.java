package com.gametest.guessplaylist.modules.spotify.api;

import com.gametest.guessplaylist.modules.spotify.infrastructure.SpotifyApiClient;
import com.gametest.guessplaylist.modules.spotify.infrastructure.SpotifyProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyAuthController {

  private static final String SCOPES = "playlist-read-private playlist-read-collaborative";

  private final SpotifyProperties properties;
  private final SpotifyApiClient spotifyApiClient;

  public SpotifyAuthController(SpotifyProperties properties, SpotifyApiClient spotifyApiClient) {
    this.properties = properties;
    this.spotifyApiClient = spotifyApiClient;
  }

  @GetMapping("/login-url")
  public ResponseEntity<SpotifyLoginUrlResponse> loginUrl() {
    String url = UriComponentsBuilder
        .fromUriString(properties.accountsBaseUrl() + "/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", properties.clientId())
        .queryParam("scope", SCOPES)
        .queryParam("redirect_uri", properties.redirectUri())
        .build()
        .toUriString();

    return ResponseEntity.ok(new SpotifyLoginUrlResponse(url));
  }

  @GetMapping("/callback")
  public ResponseEntity<SpotifyTokenExchangeResponse> callback(@RequestParam("code") String code) {
    var token = spotifyApiClient.exchangeAuthorizationCode(code);

    SpotifyTokenExchangeResponse response = new SpotifyTokenExchangeResponse(
        token.accessToken(),
        token.refreshToken(),
        token.expiresIn(),
        token.scope(),
        token.tokenType());

    return ResponseEntity.ok(response);
  }
}
