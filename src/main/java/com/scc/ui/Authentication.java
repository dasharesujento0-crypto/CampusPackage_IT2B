
package com.scc.ui;

import com.scc.dao.UserDao;

import java.util.Objects;
import java.util.Scanner;

/**
 * Starts the CampXpress user interface.
 */
public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;
    private final CampXpressUI campXpressUI;

    public Authentication() {
        this(new UserDao(), new Scanner(System.in));
    }

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.campXpressUI = new CampXpressUI();
    }

    public void start() {
        campXpressUI.run();
    }
}
