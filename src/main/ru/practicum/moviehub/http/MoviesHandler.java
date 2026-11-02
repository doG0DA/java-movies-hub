package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;


    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) {
        try {
            String method = ex.getRequestMethod();
            String[] parts = ex.getRequestURI().getPath().split("/");


            if (parts.length == 2) {
                switch (method) {
                    case "GET" -> handelGetAll(ex);
                    case "POST" -> postHandle(ex);
                    default -> sendError(ex, 405, "Некорректный запрос");
                }
            } else if (parts.length == 3) {
                switch (method) {
                    case "GET" -> handleGetById(ex, parts[2]);
                    case "DELETE" -> deleteById(ex, parts[2]);
                    default -> sendError(ex, 405, "Неккоректный запрос");
                }
            } else {
                sendError(ex, 404, "Неккоректный эндпоинт");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

    }


    public void handleGetById(HttpExchange httpExchange, String idSt) throws IOException {
        Optional<Integer> id = parseId(idSt);

        if (id.isEmpty()) {
            sendError(httpExchange, 400, "Некорректный ID");
            return;
        }

        Optional<Movie> movie = store.getById(id.get());
        if (movie.isEmpty()) {
            sendError(httpExchange, 404, "Фильм не найден");
        }

        sendJson(httpExchange, 200, gson.toJson(movie.get()));
    }

    public void handelGetAll(HttpExchange httpExchange) throws IOException {
        String query = httpExchange.getRequestURI().getQuery();

        if (query == null) {
            sendJson(httpExchange, 200, gson.toJson(store.getAll()));
            return;
        }

        if (!query.startsWith("year=")) {
            sendError(httpExchange, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        String value = query.substring("year=".length());

        int year;

        try {
            year = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sendError(httpExchange, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        sendJson(httpExchange, 200, gson.toJson(store.getByYear(year)));
    }

    public void deleteById(HttpExchange httpExchange, String idSt) throws IOException {
        Optional<Integer> id = parseId(idSt);

        if (id.isEmpty()) {
            sendError(httpExchange, 400, "Некорректный ID");
            return;
        }

        boolean deleted = store.delete(id.get());
        if (!deleted) {
            sendError(httpExchange, 404, "Фильм не найден");
            return;
        }

        sendNoContent(httpExchange);
    }

    public Optional<Integer> parseId(String id) {
        try {
            return Optional.of(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    protected void postHandle(HttpExchange httpExchange) throws IOException {
        String contentType = httpExchange.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null || !contentType.startsWith("application/json")) {
            sendError(httpExchange, 415, "Неподдерживаемый тип данных");
            return;
        }

        InputStream inputStream = httpExchange.getRequestBody();
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException e) {
            sendError(httpExchange, 400, "Некорректный JSON");
            return;
        }
        if (movie == null) {
            sendError(httpExchange, 400, "Некорректный JSON");
            return;
        }

        List<String> errors = validate(movie);

        if (!errors.isEmpty()) {
            sendErrorWithDetails(httpExchange, 422, "Ошибка валидации", errors);
            return;
        }

        Movie created = store.add(movie);

        sendJson(httpExchange, 201, gson.toJson(created));

    }

    public List<String> validate(Movie movie) {
        List<String> errors = new ArrayList<>();
        int maxYear = Year.now().getValue() + 1;

        if (movie.getTitle().isBlank() || movie.getTitle() == null) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("Слишком длинное назание");
        }

        if (movie.getYear() < 1888 || movie.getYear() > maxYear) {
            errors.add("год должен быть между 1888 и " + maxYear);
        }

        return errors;
    }


}


