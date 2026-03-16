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
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                    System.out.println("Sensor connected: " + clientSocket.getInetAddress());
                    String data = in.readLine();
                    if (data != null && data.contains(":")) {
                        try {
                            saveToDatabase(data);
                            System.out.println("Received and Saved: " + data);
                            out.println("STATUS:OK");
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            out.println("ERROR: Invalid Data Format");
                        }
                    }
                } catch (IOException | SQLException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        }catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }

    private static void saveToDatabase(String data) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD); PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            String[] parts = data.split(":");
            String name = parts[0].trim();
            String number = parts[1].trim();
            stmt.setString(1, name);
            stmt.setDouble(2, Double.parseDouble(number));
            stmt.executeUpdate();
        }
    }
}
