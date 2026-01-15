package com.mycompany.javafxapplication1;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserService.
 */
public class UserServiceTest {

    private static class StubMySQLDB extends MySQLDB {
        private final Map<String, User> users = new HashMap<>();

        @Override
        public void ensureDefaultAdmin() {
        }

        @Override
        public User getUserByName(String username) {
            return users.get(username);
        }

        @Override
        public User getUserById(int userId) {
            return users.values().stream().filter(u -> u.getUserId() == userId).findFirst().orElse(null);
        }

        @Override
        public void addUser(String username, String hash, String role) {
            users.put(username, new User(users.size() + 1, username, hash, role));
        }

        @Override
        public void deleteUser(int userId) {
            users.values().removeIf(u -> u.getUserId() == userId);
        }

        @Override
        public void updateRole(int userId, String role) {
            User user = getUserById(userId);
            if (user != null) {
                user.setRole(role);
            }
        }

        @Override
        public void updatePassword(int userId, String hash) {
            User user = getUserById(userId);
            if (user != null) {
                users.put(user.getUsername(), new User(userId, user.getUsername(), hash, user.getRole()));
            }
        }

        @Override
        public ObservableList<User> getAllUsers() {
            return FXCollections.observableArrayList(users.values());
        }

        @Override
        public void log(Integer userId, String username, String action, String detail) {
        }
    }

    private static class StubSQLiteDB extends SQLiteDB {
        private User session;

        @Override
        public void saveSession(User user) {
            session = user;
        }

        @Override
        public User loadSession() {
            return session;
        }

        @Override
        public void clearSession() {
            session = null;
        }
    }

    private UserService buildService(StubMySQLDB remote, StubSQLiteDB local) throws Exception {
        UserService service = new UserService();
        Field remoteField = UserService.class.getDeclaredField("remote");
        remoteField.setAccessible(true);
        remoteField.set(service, remote);
        Field localField = UserService.class.getDeclaredField("local");
        localField.setAccessible(true);
        localField.set(service, local);
        return service;
    }

    @Test
    @DisplayName("login stores session for valid credentials")
    public void testLoginSuccess() throws Exception {
        StubMySQLDB remote = new StubMySQLDB();
        StubSQLiteDB local = new StubSQLiteDB();
        String hash = remote.hashPassword("secret");
        remote.addUser("alice", hash, "admin");

        UserService service = buildService(remote, local);
        service.login("alice", "secret");

        User session = service.getSessionUser();
        assertNotNull(session);
        assertEquals("alice", session.getUsername());
    }

    @Test
    @DisplayName("login throws USER_NOT_FOUND when account missing")
    public void testLoginMissingUser() throws Exception {
        UserService service = buildService(new StubMySQLDB(), new StubSQLiteDB());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.login("missing", "pw"));
        assertEquals("USER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("createUser validates username and password rules")
    public void testCreateUserValidation() throws Exception {
        UserService service = buildService(new StubMySQLDB(), new StubSQLiteDB());

        assertEquals("USERNAME_EMPTY", assertThrows(IllegalArgumentException.class,
                () -> service.createUser(null, "pw", "standard")).getMessage());
        assertEquals("USERNAME_SPACE", assertThrows(IllegalArgumentException.class,
                () -> service.createUser("has space", "pw", "standard")).getMessage());
        assertEquals("PASSWORD_EMPTY", assertThrows(IllegalArgumentException.class,
                () -> service.createUser("bob", "", "standard")).getMessage());
    }

    @Test
    @DisplayName("deleteUser rejects deleting default admin")
    public void testDeleteUserRejectsAdmin() throws Exception {
        StubMySQLDB remote = new StubMySQLDB();
        StubSQLiteDB local = new StubSQLiteDB();
        remote.addUser("admin", remote.hashPassword("admin"), "admin");
        User admin = remote.getUserByName("admin");

        UserService service = buildService(remote, local);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteUser(admin, admin));
        assertEquals("CANNOT_DELETE_DEFAULT_ADMIN", ex.getMessage());
    }

    @Test
    @DisplayName("updatePassword rejects empty new password")
    public void testUpdatePasswordRejectsEmpty() throws Exception {
        StubMySQLDB remote = new StubMySQLDB();
        StubSQLiteDB local = new StubSQLiteDB();
        String hash = remote.hashPassword("old");
        remote.addUser("jane", hash, "standard");
        User user = remote.getUserByName("jane");

        UserService service = buildService(remote, local);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updatePassword(user, "old", ""));
        assertEquals("PASSWORD_EMPTY", ex.getMessage());
    }
}
