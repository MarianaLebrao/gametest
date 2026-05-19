package com.gametest.guessplaylist.modules.spotify.api;

import jakarta.validation.constraints.NotBlank;

public record SpotifyRefreshTokenInput(@NotBlank String refreshToken) {}
