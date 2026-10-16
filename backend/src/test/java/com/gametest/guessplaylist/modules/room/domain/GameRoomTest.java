package com.gametest.guessplaylist.modules.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import java.time.Instant;

class GameRoomTest {

  @Test
  void associatesTrackWithThePlayerWhoSubmittedIt() {
    GameRoom room = new GameRoom("ABC123", "Mariana", 8, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Mary");

    GameRoom.Track track = room.addTrack(friend.id(), "abcdefghijk", "A song", "A channel", "thumb", 45);

    assertThat(track.ownerPlayerId()).isEqualTo(friend.id());
    assertThat(track.ownerName()).isEqualTo("Mary");
    assertThat(room.tracks()).containsExactly(track);
  }

  @Test
  void rejectsDuplicateVideos() {
    GameRoom room = new GameRoom("ABC123", "Mariana", 8, 30, "preview");
    room.addTrack(room.hostPlayerId(), "abcdefghijk", "A song", "A channel", "thumb", 30);

    assertThatThrownBy(() -> room.addTrack(room.hostPlayerId(), "abcdefghijk", "A song", "A channel", "thumb", 30))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("já foi adicionada");
  }

  @Test
  void limitsRoomsToSixPlayers() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    for (int index = 1; index < 6; index++) room.addPlayer("Player " + index);

    assertThatThrownBy(() -> room.addPlayer("Player 6"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maximum of 6 players");
  }

  @Test
  void scoresVotesAndFinishesTheRound() {
    GameRoom room = new GameRoom("ABC123", "Host", 1, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Friend");
    GameRoom.Track track = room.addTrack(friend.id(), "abcdefghijk", "A song", "A channel", "thumb", 20);
    room.addTrack(room.hostPlayerId(), "12345678901", "Other song", "Other channel", "thumb", 30);
    room.start(room.hostPlayerId(), java.util.List.of(track.id()));

    room.submitVote(room.hostPlayerId(), friend.id());
    room.submitVote(friend.id(), friend.id());

    assertThat(room.status()).isEqualTo("RESULTS");
    assertThat(room.scores()).containsEntry(room.hostPlayerId(), 100).containsEntry(friend.id(), 100);
    room.advance(room.hostPlayerId());
    assertThat(room.status()).isEqualTo("FINISHED");
  }

  @Test
  void finishesVotingWhenServerDeadlineExpires() {
    GameRoom room = new GameRoom("ABC123", "Host", 1, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Friend");
    GameRoom.Track track = room.addTrack(friend.id(), "abcdefghijk", "A song", "A channel", "thumb", 20);
    room.addTrack(room.hostPlayerId(), "12345678901", "Other song", "Other channel", "thumb", 30);
    room.start(room.hostPlayerId(), java.util.List.of(track.id()));
    room.submitVote(room.hostPlayerId(), friend.id());

    assertThat(room.finishRoundIfExpired(Instant.now().plusSeconds(31))).isTrue();
    assertThat(room.status()).isEqualTo("RESULTS");
    assertThat(room.scores()).containsEntry(room.hostPlayerId(), 100).containsEntry(friend.id(), 0);
    assertThat(room.roundEndsAt()).isNull();
  }
}
