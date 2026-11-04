package ru.practicum.moviehub.api;


import java.util.List;

public class ErrorResponse {

    private final String error;
    private List<String> errors;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String error, List<String> errors) {
        this.error = error;
        this.errors = errors;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getAllErrors() {
        return errors;
    }
}