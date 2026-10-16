package com.gametest.guessplaylist.modules.youtube;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YouTubeClient {

  private static final Pattern[] VIDEO_ID_PATTERNS = {
      Pattern.compile("(?:youtube\\.com/watch\\?.*?v=)([a-zA-Z0-9_-]{11})"),
      Pattern.compile("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
      Pattern.compile("(?:youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})")
  };

  private final RestClient restClient;
  private final String apiKey;

  public YouTubeClient(RestClient.Builder restClientBuilder, @Value("${youtube.api-key:}") String apiKey) {
    this.restClient = restClientBuilder.baseUrl("https://www.googleapis.com/youtube/v3").build();
    this.apiKey = apiKey;
  }

  public YouTubeVideo findVideo(String videoUrl) {
    String videoId = extractVideoId(videoUrl);
    if (apiKey.isBlank()) {
      throw new IllegalStateException("YouTube API key is not configured on the backend.");
    }

    VideoListResponse response = restClient.get()
        .uri(uriBuilder -> uriBuilder.path("/videos")
            .queryParam("part", "snippet")
            .queryParam("id", videoId)
            .queryParam("key", apiKey)
            .build())
        .retrieve()
        .body(VideoListResponse.class);
    if (response == null || response.items() == null || response.items().isEmpty()) {
      throw new IllegalArgumentException("YouTube video was not found or is not public.");
    }

    VideoItem item = response.items().getFirst();
    VideoSnippet snippet = item.snippet();
    return new YouTubeVideo(item.id(), snippet.title(), snippet.channelTitle(), thumbnail(snippet.thumbnails()));
  }

  private String extractVideoId(String videoUrl) {
    String value = videoUrl == null ? "" : videoUrl.trim();
    for (Pattern pattern : VIDEO_ID_PATTERNS) {
      Matcher matcher = pattern.matcher(value);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    throw new IllegalArgumentException("Enter a valid YouTube video link.");
  }

  private String thumbnail(Map<String, Thumbnail> thumbnails) {
    if (thumbnails == null) {
      return "";
    }
    for (String quality : List.of("high", "medium", "default")) {
      Thumbnail thumbnail = thumbnails.get(quality);
      if (thumbnail != null && thumbnail.url() != null) {
        return thumbnail.url();
      }
    }
    return "";
  }

  private record VideoListResponse(List<VideoItem> items) {}
  private record VideoItem(String id, VideoSnippet snippet) {}
  private record VideoSnippet(String title, String channelTitle, Map<String, Thumbnail> thumbnails) {}
  private record Thumbnail(String url) {}
}
