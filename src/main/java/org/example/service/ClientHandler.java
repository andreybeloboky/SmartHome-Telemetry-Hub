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
            log.info("Sensor connected: {}", clientSocket.getInetAddress());
            clientSocket.setSoTimeout(10000);
            String data = in.readLine();
            processRequest(data, out);
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } catch (SQLException e) {
            log.error("SQL error while processing client request", e);
            throw new RuntimeException(e);
        }
    }

    private void processRequest(String data, PrintWriter out) throws SQLException {
        if (data != null && data.contains(":")) {
            handleData(data);
            System.out.printf("Received and Saved: [%s] \n", data);
            out.println("STATUS:OK");
        } else {
            System.err.printf("INVALID FORMAT: [%s] \n ", data);
            out.printf("ERROR: Invalid Data Format: [%s] \n", data);
        }
    }

    private void handleData(String data) throws SQLException {
        String[] parts = data.split(":");
        String deviceId = parts[0].trim();
        double newTemp = Double.parseDouble(parts[1].trim());
        updateDeviceStatus(deviceId);
        log.debug("Device status updated for device_id={}", deviceId);
        stats.updateWithLock(newTemp);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            conn.setAutoCommit(false);
            stmt.setString(1, deviceId);
            stmt.setDouble(2, newTemp);
            stmt.executeUpdate();
            log.debug("Inserted sensor log: device_id={}, value={}", deviceId, newTemp);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            log.warn("Failed to insert sensor log for device", e);
            throw new DataAccessException("Failed to load investment from database", e);
        } finally {
            conn.setAutoCommit(true);
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
            log.error("Failed to update device status for device_id= {}", deviceId, e);
            throw new RuntimeException(e);
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
