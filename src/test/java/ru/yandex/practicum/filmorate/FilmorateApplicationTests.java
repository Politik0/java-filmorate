package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.ObjectNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.dal.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmorateApplicationTests {
    private final JdbcTemplate jdbcTemplate;
    private final UserStorage userStorage;
    private final FilmGenreLineStorage filmGenreLineStorage;
    private final FilmStorage filmStorage;
    private final FriendsStorage friendsStorage;
    private final GenreStorage genreStorage;
    private final LikesStorage likesStorage;
    private final MpaStorage mpaStorage;


    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM LIKES");
        jdbcTemplate.update("DELETE FROM FILM_GENRE_LINE");
        jdbcTemplate.update("DELETE FROM FRIENDS");
        jdbcTemplate.update("DELETE FROM USERS");
        jdbcTemplate.update("DELETE FROM FILMS");
        jdbcTemplate.update("ALTER TABLE USERS ALTER COLUMN USER_ID RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE FILMS ALTER COLUMN FILM_ID RESTART WITH 1");
    }

    @Test
    void addUserTest() {
        User user = User.builder()
                .email("user@yandex.ru")
                .login("loginUser2022")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .friends(new ArrayList<>())
                .build();
        User newUser = userStorage.addUser(user);
        user.setId(1);
        assertThat(user, equalTo(newUser));
    }

    @Test
    void updateUserTest() {
        User user1 = User.builder()
                .email("user@yandex.ru")
                .login("loginUser2022")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .friends(new ArrayList<>())
                .build();
        User oldUser = userStorage.addUser(user1);
        User user2 = User.builder()
                .id(oldUser.getId())
                .email("newUser2@yandex.ru")
                .login("newLoginUser2022")
                .name("NewUser")
                .birthday(LocalDate.of(1990, 1, 2))
                .friends(new ArrayList<>())
                .build();
        User updateUser = userStorage.updateUser(user2);
        assertThat("Пользователь не обновлен", user2, equalTo(updateUser));
    }

    @Test
    void updateUserFailTest() {
        User user = User.builder()
                .id(999)
                .email("user@yandex.ru")
                .login("loginUser2022")
                .name("User")
                .birthday(LocalDate.of(1990, 1, 1))
                .friends(new ArrayList<>())
                .build();
        ObjectNotFoundException e = Assertions.assertThrows(
                ObjectNotFoundException.class, () -> userStorage.updateUser(user));
        assertThat("User with id 999 not found", equalTo(e.getMessage()));
    }

    @Test
    void getUsersByEmptyTest() {
        Collection<User> users = userStorage.getUsers();
        assertThat("Список пользователей не пуст", users, empty());
    }

    @Test
    void getUsersTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@yandex.ru")
                .login("user2")
                .name("User2")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User addUser1 = userStorage.addUser(user1);
        User addUser2 = userStorage.addUser(user2);
        assertThat("Список пользователей пуст", userStorage.getUsers(), hasSize(2));
        assertThat("User1 не найден", userStorage.getUsers(), hasItem(addUser1));
        assertThat("User2 не найден", userStorage.getUsers(), hasItem(addUser2));
    }

    @Test
    void getUserInvalidIdTest() {
        ObjectNotFoundException e = Assertions.assertThrows(
                ObjectNotFoundException.class, () -> userStorage.getUserById(1));
        assertThat("User with id 1 not found", equalTo(e.getMessage()));
    }

    @Test
    void getUserById() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User addUser = userStorage.addUser(user1);
        assertThat(addUser, equalTo(userStorage.getUserById(addUser.getId())));
    }

    @Test
    void getFriendsByEmptyTest() {
        Collection<Long> friends = friendsStorage.getListOfFriends(1);
        assertThat("Список друзей не пуст", friends, hasSize(0));
    }

    @Test
    void addAsFriendTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@yandex.ru")
                .login("user2")
                .name("User2")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User addUser1 = userStorage.addUser(user1);
        User addUser2 = userStorage.addUser(user2);
        friendsStorage.addAsFriend(addUser1.getId(), addUser2.getId());
        assertThat("User2 не добавлен в друзья User1",
                userStorage.getUserById(addUser1.getId()).getFriends(), hasItem(addUser2.getId()));
        assertThat("Список друзей User2 не пуст",
                userStorage.getUserById(addUser2.getId()).getFriends(), empty());
    }

    @Test
    void removeFromFriendsAsFriendTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@yandex.ru")
                .login("user2")
                .name("User2")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        User addUser1 = userStorage.addUser(user1);
        User addUser2 = userStorage.addUser(user2);
        friendsStorage.addAsFriend(addUser1.getId(), addUser2.getId());
        assertThat("Список друзей User1 пуст",
                userStorage.getUserById(addUser1.getId()).getFriends(), hasItem(addUser2.getId()));
        friendsStorage.removeFromFriends(addUser1.getId(), addUser2.getId());
        assertThat("Список друзей User1 не пуст",
                userStorage.getUserById(addUser1.getId()).getFriends(), empty());
    }

    @Test
    void getListOfFriendsTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@yandex.ru")
                .login("user2")
                .name("User2")
                .birthday(LocalDate.of(1992, 1, 1))
                .build();
        User user3 = User.builder()
                .email("user3@yandex.ru")
                .login("user3")
                .name("User3")
                .birthday(LocalDate.of(1993, 1, 1))
                .build();
        User addUser1 = userStorage.addUser(user1);
        User addUser2 = userStorage.addUser(user2);
        User addUser3 = userStorage.addUser(user3);
        friendsStorage.addAsFriend(addUser1.getId(), addUser2.getId());
        friendsStorage.addAsFriend(addUser1.getId(), addUser3.getId());
        assertThat("Список друзей User1 не содержит id User2 и User3",
                friendsStorage.getListOfFriends(addUser1.getId()), contains(addUser2.getId(), addUser3.getId()));
    }

    @Test
    void getAListOfMutualFriendsTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@yandex.ru")
                .login("user2")
                .name("User2")
                .birthday(LocalDate.of(1992, 1, 1))
                .build();
        User user3 = User.builder()
                .email("user3@yandex.ru")
                .login("user3")
                .name("User3")
                .birthday(LocalDate.of(1993, 1, 1))
                .build();
        User addUser1 = userStorage.addUser(user1);
        User addUser2 = userStorage.addUser(user2);
        User addUser3 = userStorage.addUser(user3);
        friendsStorage.addAsFriend(addUser1.getId(), addUser3.getId());
        friendsStorage.addAsFriend(addUser2.getId(), addUser3.getId());
        assertThat("Список друзей User1 не содержит id User2 и User3",
                friendsStorage.getAListOfMutualFriends(addUser1.getId(), addUser2.getId()),
                contains(addUser3.getId()));
    }

    @Test
    void addFilmTest() {
        Film film = Film.builder()
                .name("Psycho1")
                .description("Американский психологический хоррор 1960 года, снятый режиссёром Альфредом Хичкоком.")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(new ArrayList<>())
                .build();
        Film addFilm = filmStorage.addFilm(film);
        film.setId(1);
        assertThat(film, equalTo(addFilm));
    }

    @Test
    void updateFilmTest() {
        Film film1 = Film.builder()
                .name("Psycho1")
                .description("Американский психологический хоррор 1960 года.")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        Film oldFilm = filmStorage.addFilm(film1);
        Film film2 = Film.builder()
                .id(oldFilm.getId())
                .name("newPsycho1")
                .description("newАмериканский психологический хоррор 1960 года.")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(5)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(new ArrayList<>())
                .build();
        Film updateFilm = filmStorage.updateFilm(film2);
        assertThat("Фильм не обновлен", film2, equalTo(updateFilm));
    }

    @Test
    void updateFilmFailTest() {
        Film film = Film.builder()
                .id(999)
                .name("Psycho1")
                .description("Американский психологический хоррор 1960 года.")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        ObjectNotFoundException e = Assertions.assertThrows(
                ObjectNotFoundException.class, () -> filmStorage.updateFilm(film));
        assertThat("Film with id 999 not found", equalTo(e.getMessage()));
    }

    @Test
    void getFilmsByEmptyTest() {
        Collection<Film> films = filmStorage.getFilms();
        assertThat("Список фильмов не пуст", films, empty());
    }

    @Test
    void getFilmsTest() {
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        Film film2 = Film.builder()
                .name("Film2")
                .description("Description2")
                .releaseDate(LocalDate.of(1961, 1, 1))
                .duration(109)
                .rate(5)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(new ArrayList<>())
                .build();
        Film addFilm1 = filmStorage.addFilm(film1);
        Film addFilm2 = filmStorage.addFilm(film2);
        assertThat("Список пользователей пуст", filmStorage.getFilms(), hasSize(2));
        assertThat("Film1 не найден", filmStorage.getFilms(), hasItem(addFilm1));
        assertThat("Film2 не найден", filmStorage.getFilms(), hasItem(addFilm2));
    }

    @Test
    void getFilmInvalidIdTest() {
        ObjectNotFoundException e = Assertions.assertThrows(
                ObjectNotFoundException.class, () -> filmStorage.getFilmById(1));
        assertThat("Film with id 1 not found", equalTo(e.getMessage()));
    }

    @Test
    void getFilmById() {
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        Film addFilm = filmStorage.addFilm(film1);
        assertThat(addFilm, equalTo(filmStorage.getFilmById(addFilm.getId())));
    }

    @Test
    void getLikesByEmptyTest() {
        Collection<Long> likes = likesStorage.getListOfLikes(1);
        assertThat("Список лайков не пуст", likes, hasSize(0));
    }

    @Test
    void addLikeTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        User addUser1 = userStorage.addUser(user1);
        Film addFilm1 = filmStorage.addFilm(film1);
        likesStorage.addLike(addFilm1.getId(), addUser1.getId());
        assertThat(String.format("%s не поставил лайк %s", addUser1.getName(), addFilm1.getName()),
                filmStorage.getFilmById(addFilm1.getId()).getLikes(), hasItem(addUser1.getId()));
    }

    @Test
    void unlikeTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        User addUser1 = userStorage.addUser(user1);
        Film addFilm1 = filmStorage.addFilm(film1);
        likesStorage.addLike(addFilm1.getId(), addUser1.getId());
        assertThat(String.format("Список лайков %s пуст", addFilm1.getName()),
                filmStorage.getFilmById(addFilm1.getId()).getLikes(), hasItem(addUser1.getId()));
        likesStorage.unlike(addFilm1.getId(), addUser1.getId());
        assertThat(String.format("Список лайков %s не пуст", addFilm1.getName()),
                filmStorage.getFilmById(addFilm1.getId()).getLikes(), empty());
    }

    @Test
    void getListOfLikes() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        User addUser1 = userStorage.addUser(user1);
        Film addFilm1 = filmStorage.addFilm(film1);
        likesStorage.addLike(addFilm1.getId(), addUser1.getId());
        assertThat(String.format("Список лайков %s не содержит id %s = %s",
                        addFilm1.getName(), addUser1.getName(), addUser1.getId()),
                likesStorage.getListOfLikes(addFilm1.getId()), contains(addUser1.getId()));
    }

    @Test
    void getTheBestFilmsTest() {
        User user1 = User.builder()
                .email("user1@yandex.ru")
                .login("user1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        User user2 = User.builder()
                .email("user2@yandex.ru")
                .login("user2")
                .name("User2")
                .birthday(LocalDate.of(1992, 1, 1))
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        Film film2 = Film.builder()
                .name("Film2")
                .description("Description2")
                .releaseDate(LocalDate.of(1961, 1, 1))
                .duration(109)
                .rate(5)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(new ArrayList<>())
                .build();
        User addUser1 = userStorage.addUser(user1);
        User addUser2 = userStorage.addUser(user2);
        Film addFilm1 = filmStorage.addFilm(film1);
        Film addFilm2 = filmStorage.addFilm(film2);
        likesStorage.addLike(addFilm1.getId(), addUser1.getId());
        likesStorage.addLike(addFilm1.getId(), addUser2.getId());
        assertThat("Список лучших фильмов отличается от [1, 2]",
                likesStorage.getTheBestFilms(5), contains(addFilm1.getId(), addFilm2.getId()));
        assertThat("Список лучших фильмов отличается от [1]",
                likesStorage.getTheBestFilms(1), hasItem(addFilm1.getId()));
    }

    @Test
    void getGenresTest() {
        Genre genre = Genre.builder()
                .id(6)
                .name("Боевик")
                .build();
        assertThat(genreStorage.getGenres(), hasSize(6));
        assertThat(genreStorage.getGenres(), hasItem(genre));
    }

    @Test
    void getGenreByIdTest() {
        Genre genre1 = Genre.builder()
                .id(1)
                .name("Комедия")
                .build();
        Genre genre6 = Genre.builder()
                .id(6)
                .name("Боевик")
                .build();
        assertThat(genreStorage.getGenreById(1), equalTo(genre1));
        assertThat(genreStorage.getGenreById(6), equalTo(genre6));
    }

    @Test
    void addGenreTest() {
        Genre genre1 = Genre.builder()
                .id(1)
                .name("Комедия")
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(Genre.builder().id(2).name("Драма").build()))
                .build();
        Film addFilm1 = filmStorage.addFilm(film1);
        filmGenreLineStorage.addGenres(List.of(genre1), addFilm1.getId());
        assertThat(filmStorage.getFilmById(addFilm1.getId()).getGenres(), hasItem(genre1));
    }

    @Test
    void getListOfGenresTest() {
        Genre genre2 = Genre.builder()
                .id(2)
                .name("Драма")
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(genre2))
                .build();
        Film addFilm1 = filmStorage.addFilm(film1);
        assertThat(filmGenreLineStorage.getListOfGenres(addFilm1.getId()), hasItem(genre2.getId()));
    }

    @Test
    void deleteGenreTest() {
        Genre genre2 = Genre.builder()
                .id(2)
                .name("Драма")
                .build();
        Film film1 = Film.builder()
                .name("Film1")
                .description("Description1")
                .releaseDate(LocalDate.of(1960, 1, 1))
                .duration(109)
                .rate(1)
                .mpa(Mpa.builder().id(1).name("G").build())
                .likes(new ArrayList<>())
                .genres(List.of(genre2))
                .build();
        Film addFilm1 = filmStorage.addFilm(film1);
        filmGenreLineStorage.deleteGenres(addFilm1.getId());
        assertThat(filmGenreLineStorage.getListOfGenres(addFilm1.getId()), empty());
    }

    @Test
    void getMpaTest() {
        Mpa mpa = Mpa.builder()
                .id(1)
                .name("G")
                .build();
        assertThat(mpaStorage.getMpa(), hasSize(5));
        assertThat(mpaStorage.getMpa(), hasItem(mpa));
    }

    @Test
    void getMpaById() {
        Mpa mpa1 = Mpa.builder()
                .id(1)
                .name("G")
                .build();
        Mpa mpa5 = Mpa.builder()
                .id(5)
                .name("NC-17")
                .build();
        assertThat(mpaStorage.getMpaById(1), equalTo(mpa1));
        assertThat(mpaStorage.getMpaById(5), equalTo(mpa5));
    }
}