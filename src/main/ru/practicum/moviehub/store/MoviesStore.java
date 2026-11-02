package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {

    private final Map<Integer, Movie> movies = new LinkedHashMap<>();
    private int nextId = 1;


    public Movie add(Movie movie) {
        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public List<Movie> getByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }


}