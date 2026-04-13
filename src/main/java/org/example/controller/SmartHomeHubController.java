package org.example.controller;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.DataAccessException;
import org.example.service.ClientHandler;
import org.example.service.SystemStats;

@Slf4j
public class SmartHomeHubController {

    private static final String URL = System.getenv("DB_URL");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final HikariDataSource dataSource;

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
            dataSource.close();
        }));
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Hub started. Waiting for sensors...");
            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(() -> {
                    try (Socket s = socket;
                         Connection connection = openConnection()) {
                        new ClientHandler(s, systemStats, connection).run();
                    } catch (SQLException e) {
                        log.error("SQL error while processing client request", e);
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        log.error("Could not start server on port 8080", e);
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            log.error("Could not start server on port 8080", e);
            throw new RuntimeException(e);
        }
    }

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(LOGIN);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    private static Connection openConnection() throws SQLException {
        try {
            log.info("Opening database connection");
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.warn("Unable to establish database connection", e);
            throw new DataAccessException("Impossible connect with database", e);
        }
    }
}
