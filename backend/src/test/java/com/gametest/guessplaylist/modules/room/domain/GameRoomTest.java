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

    room.rematch(room.hostPlayerId());
    assertThat(room.status()).isEqualTo("LOBBY");
    assertThat(room.players()).hasSize(2);
    assertThat(room.tracks()).hasSize(2);
    assertThat(room.roundTrackIds()).isEmpty();
    assertThat(room.scores().values()).containsOnly(0);
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

  @Test
  void transfersHostAndRemovesTheirTracksWhenHostLeavesLobby() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Friend");
    room.addTrack(room.hostPlayerId(), "abcdefghijk", "A song", "A channel", "thumb", 20);

    room.removePlayer(room.hostPlayerId(), room.hostPlayerId());
    assertThat(room.hostPlayerId()).isEqualTo(friend.id());
    assertThat(room.tracks()).isEmpty();
  }

  @Test
  void keepsEmptyRoomAndMakesNextPlayerTheHost() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    String previousHostId = room.hostPlayerId();
    room.removePlayer(previousHostId, previousHostId);

    GameRoom.Player replacement = room.addPlayer("New host");

    assertThat(room.players()).containsExactly(replacement);
    assertThat(room.hostPlayerId()).isEqualTo(replacement.id());
  }

  @Test
  void onlyHostCanRemoveAnotherPlayer() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Friend");
    GameRoom.Player other = room.addPlayer("Other");

    assertThatThrownBy(() -> room.removePlayer(friend.id(), other.id()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("anfitrião");
  }

  @Test
  void limitsEachPlayerToTenTracks() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    for (int index = 0; index < 10; index++) {
      room.addTrack(room.hostPlayerId(), "video-id-" + index, "Song", "Channel", "thumb", 20);
    }

    assertThatThrownBy(() -> room.addTrack(room.hostPlayerId(), "video-id-10", "Song", "Channel", "thumb", 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("máximo 10 músicas");
  }

  @Test
  void requiresOneTrackForEveryConfiguredRound() {
    GameRoom room = new GameRoom("ABC123", "Host", 5, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Friend");
    room.addTrack(room.hostPlayerId(), "host-track1", "Song", "Channel", "thumb", 20);
    room.addTrack(friend.id(), "friend-trk1", "Song", "Channel", "thumb", 20);

    assertThatThrownBy(() -> room.start(room.hostPlayerId(),
        room.tracks().stream().map(GameRoom.Track::id).toList()))
        .hasMessageContaining("precisa de 5 músicas");
  }

  @Test
  void ownerCanEditAndRemoveOwnTrackInLobby() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    GameRoom.Track track = room.addTrack(room.hostPlayerId(), "abcdefghijk", "Song", "Channel", "thumb", 20);

    GameRoom.Track updated = room.updateTrackTime(room.hostPlayerId(), track.id(), 83);
    assertThat(updated.previewStartSeconds()).isEqualTo(83);
    assertThat(room.tracks()).containsExactly(updated);

    room.removeTrack(room.hostPlayerId(), track.id());
    assertThat(room.tracks()).isEmpty();
  }

  @Test
  void playerCannotEditOrRemoveAnotherPlayersTrack() {
    GameRoom room = new GameRoom("ABC123", "Host", 8, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Friend");
    GameRoom.Track track = room.addTrack(room.hostPlayerId(), "abcdefghijk", "Song", "Channel", "thumb", 20);

    assertThatThrownBy(() -> room.updateTrackTime(friend.id(), track.id(), 50))
        .hasMessageContaining("próprias músicas");
    assertThatThrownBy(() -> room.removeTrack(friend.id(), track.id()))
        .hasMessageContaining("próprias músicas");
  }
}
