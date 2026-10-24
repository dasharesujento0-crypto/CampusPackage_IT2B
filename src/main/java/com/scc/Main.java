package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseInitializer initializer = new DatabaseInitializer();
            initializer.initialize();

            UserDao userDao = new UserDao();
            Scanner scanner = new Scanner(System.in);
            Authentication authentication = new Authentication(userDao, scanner);
            authentication.start();
        } catch (IllegalStateException e) {
            System.err.println("Unable to start CampXpress: " + e.getMessage());
        }
    }
}
