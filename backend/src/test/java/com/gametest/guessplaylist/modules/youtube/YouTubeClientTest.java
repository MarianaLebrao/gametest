package com.gametest.guessplaylist.modules.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YouTubeClientTest {

  @Test
  void loadsVideoMetadataUsingTheConfiguredBaseUrl() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    YouTubeClient client = new YouTubeClient(builder, "test-key");

    server.expect(requestTo(
            "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=dQw4w9WgXcQ&key=test-key"))
        .andRespond(withSuccess("""
            {"items":[{"id":"dQw4w9WgXcQ","snippet":{"title":"Test song","channelTitle":"Test artist",
            "thumbnails":{"high":{"url":"https://example.com/thumbnail.jpg"}}}}]}
            """, MediaType.APPLICATION_JSON));

    YouTubeVideo video = client.findVideo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");

    assertThat(video.title()).isEqualTo("Test song");
    assertThat(video.channelTitle()).isEqualTo("Test artist");
    server.verify();
  }
}
