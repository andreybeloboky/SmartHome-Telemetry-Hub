package org.example.controller;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.DataAccessException;
import org.example.service.ClientHandler;
import org.example.service.SystemStats;

@Slf4j
public class SmartHomeHubController {

    private static final String URL = System.getenv("DB_URL");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {
        SystemStats systemStats = new SystemStats();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(() -> {
            System.out.println("Live Dashboard");
            System.out.println("Total readings: " + systemStats.readTotalRWLock());
            System.out.println("Highest temperature: " + systemStats.readHighTempRWLock());
        }, 0, 60, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            executor.shutdown();
            monitor.shutdown();
        }));
        try (Connection connection = openConnection();
             ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Hub started. Waiting for sensors...");
            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(new ClientHandler(socket, systemStats, connection));
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection openConnection() throws SQLException {
        try {
            log.info("Opening database connection");
            return DriverManager.getConnection(URL, LOGIN, PASSWORD);
        } catch (SQLException e) {
            log.warn("Unable to establish database connection", e);
            throw new DataAccessException("Impossible connect with database", e);
        }
    }
}
