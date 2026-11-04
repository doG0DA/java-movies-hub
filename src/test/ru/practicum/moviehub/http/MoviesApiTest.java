package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {


        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_withArrays_returnArrays() throws Exception {

        store.add(new Movie("Матрица", 1990));

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size());
        assertEquals("Матрица", movies.get(0).getTitle());

    }

    @Test
    void postMovie_whenValid_returns201WithId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Матрица\",\"year\":1999}", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie movie = gson.fromJson(resp.body(), Movie.class);
        assertTrue(movie.getId() > 0);
        assertEquals("Матрица", movie.getTitle());
        assertEquals(1999, movie.getYear());
        assertEquals(1, store.getAll().size());

    }

    @Test
    void postMovie_whenTitleEmpty_returns422() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"\",\"year\":1999}", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        JsonElement jsonElement = JsonParser.parseString(resp.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        assertTrue(jsonObject.has("error"));
        assertTrue(store.getAll().isEmpty());

    }


    @Test
    void postMovie_whenTitleEmpty_returns415() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "plan")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"Матрица\",\"year\":1999}", StandardCharsets.UTF_8))
                .build();


        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void postMovie_whenJSisIncorrect_returns420() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":", StandardCharsets.UTF_8))
                .build();


        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void getMovieById_whenExists_returns200AndMovie() throws Exception {

        Movie saved = store.add(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/" + saved.getId()))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie current = gson.fromJson(resp.body(), Movie.class);

        assertEquals(200, resp.statusCode());
        assertEquals(saved.getTitle(), current.getTitle());
        assertEquals(saved.getId(), current.getId());
        assertEquals(saved.getYear(), current.getYear());

    }

    @Test
    void getMovieById_whenNoExists_returns404() throws Exception {

        Movie saved = store.add(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/" + 1999))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        JsonElement jsonElement = JsonParser.parseString(resp.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        assertTrue(jsonObject.has("error"));
    }

    @Test
    void getMovieByIncorrectId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/abc"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
        JsonElement jsonElement = JsonParser.parseString(resp.body());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        assertTrue(jsonObject.has("error"));
    }


    @Test
    void deleteMovieByID_return204() throws Exception {

        Movie saved = store.add(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json")
                .uri(URI.create(BASE + "/movies/" + saved.getId()))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
        assertTrue(store.getAll().size() == 0);
    }

    @Test
    void deleteMovieByNonExistatntID_return404() throws Exception {
        Movie saved = store.add(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json")
                .uri(URI.create(BASE + "/movies/" + 1999))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertEquals(1, store.getAll().size());
    }

    @Test
    void deleteMovieIncorrectId_return400() throws Exception {
        Movie saved = store.add(new Movie("Матрица", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json")
                .uri(URI.create(BASE + "/movies/abc"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void getMovies_withYear_returnsThatYear() throws Exception {

        store.add(new Movie("Матрица", 1999));
        store.add(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size());
    }

    @Test
    void getMovies_withEmptyYear_returns200() throws Exception {

        store.add(new Movie("Матрица", 1999));
        store.add(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(0, movies.size());
    }

    @Test
    void getMovies_withInvalidYear_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());

        JsonObject error = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(error.has("error"));
    }


}