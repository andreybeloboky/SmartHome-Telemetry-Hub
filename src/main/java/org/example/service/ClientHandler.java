package org.example.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.DataAccessException;

@Slf4j
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final SystemStats stats;
    private final Connection conn;
    private static final String SQL_INSERT = "INSERT INTO sensor_logs (device_name, reading_value) VALUES (?, ?)";
    private static final String SQL_INSERT_DEVICE = "INSERT INTO device_status (device_id, total_messages_sent) VALUES (?,1)";
    private static final String SQL_UPDATE_DEVICE = "UPDATE device_status SET total_messages_sent = total_messages_sent+1, last_seen = CURRENT_TIMESTAMP WHERE device_id = ?";
    private static final String SQL_SELECT_DEVICE = "SELECT device_id FROM device_status WHERE device_id = ? FOR UPDATE";

    public ClientHandler(Socket clientSocket, SystemStats stats, Connection conn) {
        this.clientSocket = clientSocket;
        this.stats = stats;
        this.conn = conn;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            System.out.println("Sensor connected: " + clientSocket.getInetAddress());
            clientSocket.setSoTimeout(10000);
            String data = in.readLine();
            processRequest(data, out);
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private void processRequest(String data, PrintWriter out) {
        if (data != null && data.contains(":")) {
            handleData(data);
            System.out.printf("Received and Saved: [%s] \n", data);
            out.println("STATUS:OK");
        } else {
            System.err.printf("INVALID FORMAT: [%s] \n ", data);
            out.printf("ERROR: Invalid Data Format: [%s] \n", data);
        }
    }

    private void handleData(String data) {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            String[] parts = data.split(":");
            String deviceId = parts[0].trim();
            String newTemp = parts[1].trim();
            updateDeviceStatus(deviceId);
            stats.updateWithLock(Double.parseDouble(newTemp));
            stmt.setString(1, deviceId);
            stmt.setDouble(2, Double.parseDouble(newTemp));
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.warn("Error while loading", e);
            throw new DataAccessException("Failed to load investment from database", e);
        }
    }

    private void updateDeviceStatus(String deviceId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_DEVICE);
             PreparedStatement update = conn.prepareStatement(SQL_UPDATE_DEVICE);
             PreparedStatement insert = conn.prepareStatement(SQL_INSERT_DEVICE)) {
            conn.setAutoCommit(false);
            stmt.setString(1, deviceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                update.setString(1, deviceId);
                update.executeUpdate();
            } else {
                insert.setString(1, deviceId);
                insert.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new RuntimeException(e);
        }finally {
            conn.setAutoCommit(true);
        }
    }
}
