package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmartHomeHub {

    private static final String URL = System.getenv("DB_URL");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final String SQL_INSERT = "INSERT INTO sensor_logs (device_name, reading_value) VALUES (?, ?)";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Hub started. Waiting for sensors...");
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                    System.out.println("Sensor connected: " + clientSocket.getInetAddress());
                    String data = in.readLine();
                    processRequest(data, out);
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    private static void processRequest(String data, PrintWriter out) {
        if (data != null && data.contains(":")) {
            handleData(data);
            System.out.printf("Received and Saved: [%s] \n", data);
            out.println("STATUS:OK");
        } else {
            System.err.printf("INVALID FORMAT: [%s] \n ", data);
            out.printf("ERROR: Invalid Data Format: [%s] \n", data);
        }
    }

    private static void handleData(String data) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            String[] parts = data.split(":");
            String name = parts[0].trim();
            String number = parts[1].trim();
            stmt.setString(1, name);
            stmt.setDouble(2, Double.parseDouble(number));
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.warn("Error while loading", e);
            throw new DataAccessException("Failed to load investment from database", e);
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
