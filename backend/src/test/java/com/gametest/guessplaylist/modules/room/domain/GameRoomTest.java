package com.gametest.guessplaylist.modules.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GameRoomTest {

  @Test
  void associatesTrackWithThePlayerWhoSubmittedIt() {
    GameRoom room = new GameRoom("ABC123", "Mariana", 8, 30, "preview");
    GameRoom.Player friend = room.addPlayer("Mary");

    GameRoom.Track track = room.addTrack(friend.id(), "abcdefghijk", "A song", "A channel", "thumb");

    assertThat(track.ownerPlayerId()).isEqualTo(friend.id());
    assertThat(track.ownerName()).isEqualTo("Mary");
    assertThat(room.tracks()).containsExactly(track);
  }

  @Test
  void rejectsDuplicateVideos() {
    GameRoom room = new GameRoom("ABC123", "Mariana", 8, 30, "preview");
    room.addTrack(room.hostPlayerId(), "abcdefghijk", "A song", "A channel", "thumb");

    assertThatThrownBy(() -> room.addTrack(room.hostPlayerId(), "abcdefghijk", "A song", "A channel", "thumb"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("This video is already in the room.");
  }
}
