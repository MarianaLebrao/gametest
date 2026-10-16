package com.gametest.guessplaylist.modules.room.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public class GameRoom {
  public static final int MAX_PLAYERS = 6;
  public static final int MAX_TRACKS_PER_PLAYER = 10;

  private final String code;
  private String hostPlayerId;
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
  private Instant roundEndsAt;

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
                  List<String> roundTrackIds, Map<String, String> votes, Map<String, Integer> scores,
                  Instant roundEndsAt) {
    this.code = code;
    this.hostPlayerId = hostPlayerId;
    this.roundCount = roundCount;
    this.roundTimer = roundTimer;
    this.gameMode = gameMode;
    this.players.addAll(players == null ? List.of() : players);
    this.tracks.addAll(tracks == null ? List.of() : tracks);
    this.status = status == null ? "LOBBY" : status;
    this.currentRound = currentRound;
    this.roundEndsAt = "PLAYING".equals(this.status) && roundEndsAt == null
        ? Instant.now().plusSeconds(roundTimer)
        : roundEndsAt;
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
    if (players.size() == 1) hostPlayerId = player.id();
    scores.put(player.id(), 0);
    return player;
  }

  public synchronized Track addTrack(String playerId, String videoId, String title, String channelTitle,
                                     String thumbnailUrl, int previewStartSeconds) {
    requireLobby();
    Player owner = player(playerId);
    long playerTrackCount = tracks.stream().filter(track -> track.ownerPlayerId().equals(playerId)).count();
    if (playerTrackCount >= MAX_TRACKS_PER_PLAYER) {
      throw new IllegalArgumentException("Cada jogador pode adicionar no máximo 10 músicas.");
    }
    if (tracks.stream().anyMatch(track -> track.videoId().equals(videoId))) {
      throw new IllegalArgumentException("Essa música já foi adicionada por outro jogador. Escolha outra música.");
    }
    Track track = new Track(UUID.randomUUID().toString(), videoId, title, channelTitle, thumbnailUrl,
        owner.id(), owner.name(), previewStartSeconds);
    tracks.add(track);
    return track;
  }

  public synchronized Track updateTrackTime(String playerId, String trackId, int previewStartSeconds) {
    requireLobby();
    player(playerId);
    int index = trackIndex(trackId);
    Track current = tracks.get(index);
    requireTrackOwner(current, playerId);
    Track updated = new Track(current.id(), current.videoId(), current.title(), current.channelTitle(),
        current.thumbnailUrl(), current.ownerPlayerId(), current.ownerName(), previewStartSeconds);
    tracks.set(index, updated);
    return updated;
  }

  public synchronized void removeTrack(String playerId, String trackId) {
    requireLobby();
    player(playerId);
    int index = trackIndex(trackId);
    requireTrackOwner(tracks.get(index), playerId);
    tracks.remove(index);
  }

  public synchronized void removePlayer(String requesterPlayerId, String targetPlayerId) {
    requireLobby();
    player(requesterPlayerId);
    player(targetPlayerId);
    if (!requesterPlayerId.equals(targetPlayerId) && !hostPlayerId.equals(requesterPlayerId)) {
      throw new IllegalArgumentException("Apenas o anfitrião pode remover outro jogador.");
    }
    players.removeIf(player -> player.id().equals(targetPlayerId));
    tracks.removeIf(track -> track.ownerPlayerId().equals(targetPlayerId));
    scores.remove(targetPlayerId);
    votes.remove(targetPlayerId);
    votes.entrySet().removeIf(entry -> entry.getValue().equals(targetPlayerId));
    if (targetPlayerId.equals(hostPlayerId) && !players.isEmpty()) hostPlayerId = players.getFirst().id();
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
    if (tracks.size() < roundCount) {
      throw new IllegalArgumentException("A sala precisa de " + roundCount + " músicas para jogar "
          + roundCount + " rodadas.");
    }
    roundTrackIds.clear();
    roundTrackIds.addAll(shuffledTrackIds.stream().limit(roundCount).toList());
    currentRound = 0;
    votes.clear();
    status = "PLAYING";
    roundEndsAt = Instant.now().plusSeconds(roundTimer);
  }

  public synchronized void submitVote(String voterPlayerId, String targetPlayerId) {
    finishRoundIfExpired(Instant.now());
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
      roundEndsAt = null;
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
    roundEndsAt = "PLAYING".equals(status) ? Instant.now().plusSeconds(roundTimer) : null;
  }

  public synchronized boolean finishRoundIfExpired(Instant now) {
    if (!"PLAYING".equals(status) || roundEndsAt == null || now.isBefore(roundEndsAt)) return false;
    scoreRound();
    status = "RESULTS";
    roundEndsAt = null;
    return true;
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

  private int trackIndex(String trackId) {
    for (int index = 0; index < tracks.size(); index++) {
      if (tracks.get(index).id().equals(trackId)) return index;
    }
    throw new IllegalArgumentException("Música não encontrada nesta sala.");
  }

  private void requireTrackOwner(Track track, String playerId) {
    if (!track.ownerPlayerId().equals(playerId)) {
      throw new IllegalArgumentException("Você só pode editar ou excluir suas próprias músicas.");
    }
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
  public synchronized Instant roundEndsAt() { return roundEndsAt; }

  public record Player(String id, String name) {}
  public record Track(String id, String videoId, String title, String channelTitle, String thumbnailUrl,
                      String ownerPlayerId, String ownerName, int previewStartSeconds) {}
}
