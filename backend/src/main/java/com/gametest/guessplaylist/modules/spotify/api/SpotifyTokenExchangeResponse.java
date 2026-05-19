package com.gametest.guessplaylist.modules.spotify.api;

public record SpotifyTokenExchangeResponse(
    String accessToken,
    String refreshToken,
    Integer expiresIn,
    String scope,
    String tokenType) {}
