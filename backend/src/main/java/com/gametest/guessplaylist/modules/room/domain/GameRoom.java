package com.gametest.guessplaylist.modules.room.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameRoom {
  public static final int MAX_PLAYERS = 6;

  private final String code;
  private final String hostPlayerId;
  private final int roundCount;
  private final int roundTimer;
  private final String gameMode;
  private final List<Player> players = new ArrayList<>();
  private final List<Track> tracks = new ArrayList<>();
  private final List<String> roundTrackIds = new ArrayList<>();
  private final Map<String, String> votes = new LinkedHashMap<>();
  private final Map<String, Integer> scores = new LinkedHashMap<>();
  private String status = "LOBBY";
  private int currentRound;

  public GameRoom(String code, String hostName, int roundCount, int roundTimer, String gameMode) {
    this.code = code;
    this.hostPlayerId = UUID.randomUUID().toString();
    this.roundCount = roundCount;
    this.roundTimer = roundTimer;
    this.gameMode = gameMode;
    players.add(new Player(hostPlayerId, hostName));
    scores.put(hostPlayerId, 0);
  }

  public GameRoom(String code, String hostPlayerId, int roundCount, int roundTimer, String gameMode,
                  List<Player> players, List<Track> tracks, String status, int currentRound,
                  List<String> roundTrackIds, Map<String, String> votes, Map<String, Integer> scores) {
    this.code = code;
    this.hostPlayerId = hostPlayerId;
    this.roundCount = roundCount;
    this.roundTimer = roundTimer;
    this.gameMode = gameMode;
    this.players.addAll(players == null ? List.of() : players);
    this.tracks.addAll(tracks == null ? List.of() : tracks);
    this.status = status == null ? "LOBBY" : status;
    this.currentRound = currentRound;
    this.roundTrackIds.addAll(roundTrackIds == null ? List.of() : roundTrackIds);
    this.votes.putAll(votes == null ? Map.of() : votes);
    if (scores == null || scores.isEmpty()) {
      this.players.forEach(player -> this.scores.put(player.id(), 0));
    } else {
      this.scores.putAll(scores);
    }
  }

  public synchronized Player addPlayer(String name) {
    requireLobby();
    if (players.size() >= MAX_PLAYERS) {
      throw new IllegalArgumentException("This room already has the maximum of 6 players.");
    }
    if (players.stream().anyMatch(player -> player.name().equalsIgnoreCase(name))) {
      throw new IllegalArgumentException("This name is already being used in the room.");
    }
    Player player = new Player(UUID.randomUUID().toString(), name);
    players.add(player);
    scores.put(player.id(), 0);
    return player;
  }

  public synchronized Track addTrack(String playerId, String videoId, String title, String channelTitle,
                                     String thumbnailUrl, int previewStartSeconds) {
    requireLobby();
    Player owner = player(playerId);
    if (tracks.stream().anyMatch(track -> track.videoId().equals(videoId))) {
      throw new IllegalArgumentException("This video is already in the room.");
    }
    Track track = new Track(UUID.randomUUID().toString(), videoId, title, channelTitle, thumbnailUrl,
        owner.id(), owner.name(), previewStartSeconds);
    tracks.add(track);
    return track;
  }

  public synchronized void start(String playerId, List<String> shuffledTrackIds) {
    requireHost(playerId);
    requireLobby();
    if (players.size() < 2) {
      throw new IllegalArgumentException("At least 2 players are required to start.");
    }
    boolean everyoneAddedSong = players.stream()
        .allMatch(player -> tracks.stream().anyMatch(track -> track.ownerPlayerId().equals(player.id())));
    if (!everyoneAddedSong) {
      throw new IllegalArgumentException("Every player must add at least one song.");
    }
    roundTrackIds.clear();
    roundTrackIds.addAll(shuffledTrackIds.stream().limit(roundCount).toList());
    currentRound = 0;
    votes.clear();
    status = "PLAYING";
  }

  public synchronized void submitVote(String voterPlayerId, String targetPlayerId) {
    if (!"PLAYING".equals(status)) {
      throw new IllegalArgumentException("This round is not accepting votes.");
    }
    player(voterPlayerId);
    player(targetPlayerId);
    if (votes.putIfAbsent(voterPlayerId, targetPlayerId) != null) {
      throw new IllegalArgumentException("You already voted in this round.");
    }
    if (votes.size() == players.size()) {
      scoreRound();
      status = "RESULTS";
    }
  }

  public synchronized void advance(String playerId) {
    requireHost(playerId);
    if (!"RESULTS".equals(status)) {
      throw new IllegalArgumentException("The current round is not finished yet.");
    }
    votes.clear();
    currentRound++;
    status = currentRound >= roundTrackIds.size() ? "FINISHED" : "PLAYING";
  }

  public synchronized Track currentTrack() {
    if (roundTrackIds.isEmpty() || currentRound >= roundTrackIds.size()) return null;
    String trackId = roundTrackIds.get(currentRound);
    return tracks.stream().filter(track -> track.id().equals(trackId)).findFirst().orElse(null);
  }

  private void scoreRound() {
    Track track = currentTrack();
    if (track == null) return;
    votes.forEach((voterId, targetId) -> {
      if (track.ownerPlayerId().equals(targetId)) {
        scores.compute(voterId, (ignored, score) -> (score == null ? 0 : score) + 100);
      }
    });
  }

  private Player player(String playerId) {
    return players.stream().filter(player -> player.id().equals(playerId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Player does not belong to this room."));
  }

  private void requireHost(String playerId) {
    if (!hostPlayerId.equals(playerId)) {
      throw new IllegalArgumentException("Only the host can perform this action.");
    }
  }

  private void requireLobby() {
    if (!"LOBBY".equals(status)) {
      throw new IllegalArgumentException("The game has already started.");
    }
  }

  public String code() { return code; }
  public String hostPlayerId() { return hostPlayerId; }
  public int roundCount() { return roundCount; }
  public int roundTimer() { return roundTimer; }
  public String gameMode() { return gameMode; }
  public String status() { return status; }
  public int currentRound() { return currentRound; }
  public synchronized List<Player> players() { return List.copyOf(players); }
  public synchronized List<Track> tracks() { return List.copyOf(tracks); }
  public synchronized List<String> roundTrackIds() { return List.copyOf(roundTrackIds); }
  public synchronized Map<String, String> votes() { return Map.copyOf(votes); }
  public synchronized Map<String, Integer> scores() { return Map.copyOf(scores); }

  public record Player(String id, String name) {}
  public record Track(String id, String videoId, String title, String channelTitle, String thumbnailUrl,
                      String ownerPlayerId, String ownerName, int previewStartSeconds) {}
}
