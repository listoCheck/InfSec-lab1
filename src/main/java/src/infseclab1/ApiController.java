package src.infseclab1;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping
public class ApiController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ApiController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest login) {
        try {
            // JdbcTemplate binds values as parameters; user input is never concatenated into SQL.
            String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE username = ?",
                    String.class, login.username());
            if (hash != null && passwordEncoder.matches(login.password(), hash)) {
                return new TokenResponse(jwtService.createToken(login.username()));
            }
        } catch (EmptyResultDataAccessException ignored) {
            // Return the same response for a non-existent account to prevent user enumeration.
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @GetMapping("/api/data")
    public List<PostResponse> getPosts() {
        return jdbc.query("SELECT id, author, content, created_at FROM posts ORDER BY id", (rs, row) ->
                new PostResponse(rs.getLong("id"), safeForHtml(rs.getString("author")),
                        safeForHtml(rs.getString("content")), rs.getTimestamp("created_at").toInstant()));
    }

    @PostMapping("/api/posts")
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest post, HttpServletRequest request) {
        String username = (String) request.getAttribute("authenticatedUser");
        jdbc.update("INSERT INTO posts(author, content) VALUES (?, ?)", username, post.content());
        return new PostResponse(0, safeForHtml(username), safeForHtml(post.content()), null);
    }

    private String safeForHtml(String value) {
        // JSON is not HTML, but encoding creates a safe representation for clients that render it in HTML.
        return HtmlUtils.htmlEscape(value);
    }

    public record LoginRequest(@NotBlank @Size(max = 64) String username,
                               @NotBlank @Size(min = 8, max = 128) String password) { }
    public record CreatePostRequest(@NotBlank @Size(max = 500) String content) { }
    public record TokenResponse(String token) { }
    public record PostResponse(long id, String author, String content, java.time.Instant createdAt) { }
}
