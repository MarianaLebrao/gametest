package com.gametest.guessplaylist.modules.room.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameRoom {

  private final String code;
  private final String hostPlayerId;
  private final int roundCount;
  private final int roundTimer;
  private final String gameMode;
  private final List<Player> players = new ArrayList<>();
  private final List<Track> tracks = new ArrayList<>();

  public GameRoom(String code, String hostName, int roundCount, int roundTimer, String gameMode) {
    this.code = code;
    this.hostPlayerId = UUID.randomUUID().toString();
    this.roundCount = roundCount;
    this.roundTimer = roundTimer;
    this.gameMode = gameMode;
    players.add(new Player(hostPlayerId, hostName));
  }

  public GameRoom(String code, String hostPlayerId, int roundCount, int roundTimer, String gameMode,
                  List<Player> players, List<Track> tracks) {
    this.code = code;
    this.hostPlayerId = hostPlayerId;
    this.roundCount = roundCount;
    this.roundTimer = roundTimer;
    this.gameMode = gameMode;
    this.players.addAll(players);
    this.tracks.addAll(tracks);
  }

  public synchronized Player addPlayer(String name) {
    Player player = new Player(UUID.randomUUID().toString(), name);
    players.add(player);
    return player;
  }

  public synchronized Track addTrack(String playerId, String videoId, String title, String channelTitle, String thumbnailUrl) {
    Player owner = players.stream().filter(player -> player.id().equals(playerId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Player does not belong to this room."));
    if (tracks.stream().anyMatch(track -> track.videoId().equals(videoId))) {
      throw new IllegalArgumentException("This video is already in the room.");
    }
    Track track = new Track(UUID.randomUUID().toString(), videoId, title, channelTitle, thumbnailUrl, owner.id(), owner.name());
    tracks.add(track);
    return track;
  }

  public String code() { return code; }
  public String hostPlayerId() { return hostPlayerId; }
  public int roundCount() { return roundCount; }
  public int roundTimer() { return roundTimer; }
  public String gameMode() { return gameMode; }
  public synchronized List<Player> players() { return List.copyOf(players); }
  public synchronized List<Track> tracks() { return List.copyOf(tracks); }

  public record Player(String id, String name) {}
  public record Track(String id, String videoId, String title, String channelTitle, String thumbnailUrl,
                      String ownerPlayerId, String ownerName) {}
}
