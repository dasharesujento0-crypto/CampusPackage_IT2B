
package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Objects;
import java.util.Scanner;


public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public Authentication() {
        this(new UserDao(), new Scanner(System.in));
    }

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public void start() {
        new CampXpressUI(this).run();
    }

    /** Call only after a successful login. */
    public void recordLogin(User user) {
        Objects.requireNonNull(user, "user");
        activityLogDao.logAction(user.id(), "USER_LOGIN",
                "User " + user.username() + " (ID: " + user.id() + ") logged in.");
    }

    /** Call once when an authenticated session ends. */
    public void recordLogout(User user) {
        Objects.requireNonNull(user, "user");
        activityLogDao.logAction(user.id(), "USER_LOGOUT",
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }
}
