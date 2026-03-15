package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class SmartHomeHub {

    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final String SQL_INSERT = "INSERT INTO sensor_logs (device_name, reading_value) VALUES (?, ?)";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Hub started. Waiting for sensors...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Sensor connected: " + clientSocket.getInetAddress());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String data = in.readLine();
            saveToDatabase(data);
            System.out.println("Received: " + data);
            out.println("OK: Data received");
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveToDatabase(String data) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            String[] parts = data.split(":"); // например "temp:22.5"
            stmt.setString(1, parts[0]);
            stmt.setDouble(2, Double.parseDouble(parts[1]));
            stmt.executeUpdate();
        }
    }
}
