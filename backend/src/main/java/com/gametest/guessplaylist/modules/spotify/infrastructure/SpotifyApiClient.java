package com.gametest.guessplaylist.modules.spotify.infrastructure;

import com.gametest.guessplaylist.modules.spotify.domain.SpotifyPlaylistData;
import com.gametest.guessplaylist.modules.spotify.domain.SpotifyTrackOwnership;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@SuppressWarnings("null")
public class SpotifyApiClient {

  private final SpotifyProperties properties;
  private final RestClient restClient;

  public SpotifyApiClient(SpotifyProperties properties) {
    this.properties = properties;
    this.restClient = RestClient.create();
  }

  public SpotifyPlaylistData fetchPlaylistData(String playlistId) {
    validateCredentials();
    String token = Objects.requireNonNull(fetchAppAccessToken());
    return fetchPlaylistDataWithToken(playlistId, token);
  }

  public SpotifyPlaylistData fetchPlaylistDataWithUserToken(String playlistId, String userAccessToken) {
    if (userAccessToken == null || userAccessToken.isBlank()) {
      throw new IllegalArgumentException("Spotify user access token is required");
    }
    return fetchPlaylistDataWithToken(playlistId, userAccessToken);
  }

  private SpotifyPlaylistData fetchPlaylistDataWithToken(String playlistId, String token) {
    SpotifyPlaylistResponse playlist = restClient.get()
        .uri(properties.apiBaseUrl() + "/playlists/{id}", playlistId)
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .body(SpotifyPlaylistResponse.class);

    List<SpotifyTrackOwnership> ownerships = fetchAllPlaylistOwnerships(playlistId, token);

    return new SpotifyPlaylistData(
        playlist != null ? playlist.id() : playlistId,
        playlist != null ? playlist.name() : "Unknown Playlist",
        ownerships);
  }

  public SpotifyUserTokenResponse exchangeAuthorizationCode(String authorizationCode) {
    validateCredentials();

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("code", authorizationCode);
    body.add("redirect_uri", properties.redirectUri());

    return restClient.post()
        .uri(properties.accountsBaseUrl() + "/api/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
        .body(body)
        .retrieve()
        .body(SpotifyUserTokenResponse.class);
  }

  private List<SpotifyTrackOwnership> fetchAllPlaylistOwnerships(String playlistId, String token) {
    List<SpotifyTrackOwnership> ownerships = new ArrayList<>();
    String nextUrl = properties.apiBaseUrl() + "/playlists/" + playlistId + "/tracks?limit=100";

    while (nextUrl != null && !nextUrl.isBlank()) {
      SpotifyPlaylistItemsPageResponse page;
      try {
        page = restClient.get()
            .uri(nextUrl)
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .body(SpotifyPlaylistItemsPageResponse.class);
      } catch (HttpClientErrorException.Forbidden ex) {
        throw new IllegalStateException(
            "Spotify denied access to playlist items (403). Use host login with Spotify and retry import.",
            ex);
      }

      if (page == null || page.items() == null) {
        break;
      }

      for (SpotifyPlaylistItem item : page.items()) {
        if (item == null || item.track() == null || item.addedBy() == null) {
          continue;
        }
        if (item.track().id() == null || item.addedBy().id() == null) {
          continue;
        }

        ownerships.add(new SpotifyTrackOwnership(
            item.track().name(),
            item.addedBy().id(),
            item.addedBy().displayName() != null ? item.addedBy().displayName() : item.addedBy().id()));
      }

      nextUrl = page.next();
    }

    return ownerships;
  }

  private void validateCredentials() {
    if (properties.clientId() == null || properties.clientId().isBlank()
        || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
      throw new IllegalStateException("Missing Spotify credentials. Set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET");
    }
  }

  private String fetchAppAccessToken() {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");

    SpotifyTokenResponse tokenResponse = restClient.post()
        .uri(properties.accountsBaseUrl() + "/api/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
        .body(body)
        .retrieve()
        .body(SpotifyTokenResponse.class);

    if (tokenResponse == null || tokenResponse.accessToken() == null) {
      throw new IllegalStateException("Failed to obtain Spotify app token");
    }
    return Objects.requireNonNull(tokenResponse.accessToken());
  }
}
