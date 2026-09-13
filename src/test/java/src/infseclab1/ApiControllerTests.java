package src.infseclab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiControllerTests {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        HttpResponse<String> response = send("/api/data", "GET", null, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void loginThenAccessDataAndEscapePost() throws Exception {
        HttpResponse<String> login = send("/auth/login", "POST",
                "{\"username\":\"student\",\"password\":\"SecurePass123!\"}", null);
        assertEquals(200, login.statusCode());
        Matcher matcher = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"").matcher(login.body());
        assertTrue(matcher.find());
        String token = matcher.group(1);

        assertEquals(200, send("/api/data", "GET", null, token).statusCode());
        HttpResponse<String> post = send("/api/posts", "POST",
                "{\"content\":\"<script>alert(1)</script>\"}", token);
        assertEquals(200, post.statusCode());
        assertTrue(post.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    private HttpResponse<String> send(String path, String method, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
