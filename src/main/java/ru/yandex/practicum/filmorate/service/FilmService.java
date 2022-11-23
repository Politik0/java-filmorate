package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ObjectNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.log.Logger;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.dal.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.dal.FilmStorage;
import ru.yandex.practicum.filmorate.storage.dal.LikesStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final LikesStorage likesStorage;

    public Collection<Film> getFilms() {
        Collection<Film> filmsInStorage = filmStorage.getFilms();
        Logger.logSave(HttpMethod.GET, "/films", filmsInStorage.toString());
        return filmsInStorage;
    }

    public Film addFilm(Film film) {
        Film filmInStorage = filmStorage.addFilm(checkValidation(film));
        Logger.logSave(HttpMethod.POST, "/films", filmInStorage.toString());
        return filmInStorage;
    }

    public Film updateFilm(Film film) {
        Film filmInStorage = filmStorage.updateFilm(checkValidation(film));
        Logger.logSave(HttpMethod.PUT, "/films", filmInStorage.toString());
        return filmInStorage;
    }

    public Film getFilmById(long id) {
        Film filmInStorage = filmStorage.getFilmById(id);
        Logger.logSave(HttpMethod.GET, "/films/" + id, filmInStorage.toString());
        return filmInStorage;
    }

    public void addLike(long id, long userId) {
        boolean addition;
        filmStorage.getFilmById(id);
        userService.getUserById(userId);
        addition = likesStorage.addLike(id, userId);
        Logger.logSave(HttpMethod.PUT, "/films/" + id + "/like/" + userId, ((Boolean) addition).toString());
    }

    public void unlike(long id, long userId) {
        boolean removal;
        filmStorage.getFilmById(id);
        userService.getUserById(userId);
        removal = likesStorage.unlike(id, userId);
        if (!removal) {
            throw new ObjectNotFoundException(String.format("User with id %s did not like the movie with id %s",
                    userId, id));
        }
        Logger.logSave(HttpMethod.DELETE, "/films/" + id + "/like/" + userId, ((Boolean) removal).toString());
    }

    public List<Long> getListOfLikes(long id) {
        filmStorage.getFilmById(id);
        List<Long> likeList = likesStorage.getListOfLikes(id);
        Logger.logSave(HttpMethod.GET, "/films/" + id + "/likes", likeList.toString());
        return likeList;
    }

    public List<Film> getTheBestFilms(int count) {
        List<Film> bestFilms = likesStorage.getTheBestFilms(count).stream()
                .map(filmStorage::getFilmById)
                .collect(Collectors.toList());
        Logger.logSave(HttpMethod.GET, "/films/popular?count=" + count, bestFilms.toString());
        return bestFilms;
    }

    private Film checkValidation(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate()
                .isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Release date must not be earlier than 12-28-1895");
        }
        return film;
    }
    public List<Film> getSortedDirectorFilms(long filmId, String sortBy) {
        List<Film> sortedDirectorFilms = new ArrayList<>();
        if (sortBy.equals("year")) {
            sortedDirectorFilms = filmStorage.getListOfDirectorFilms(filmId).stream()
                    .sorted(Comparator.comparing(o->o.getReleaseDate()))
                    .collect(Collectors.toList());
        } else if (sortBy.equals("likes")) {
            sortedDirectorFilms = filmStorage.getListOfDirectorFilms(filmId).stream()
                    .sorted(Comparator.comparing(o->o.getLikes().size()))
                    .collect(Collectors.toList());
        }
        Logger.logSave(HttpMethod.GET, "/films/director/" + filmId +"&sortBy=" + sortBy,
                sortedDirectorFilms.toString());
        return sortedDirectorFilms;
    }
}
