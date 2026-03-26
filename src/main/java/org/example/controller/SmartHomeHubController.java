package org.example.controller;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.example.service.ClientHandler;
import org.example.service.SystemStats;

@Slf4j
public class SmartHomeHubController {

    public static void main(String[] args) {
        SystemStats systemStats = new SystemStats();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(() -> {
            System.out.println("Live Dashboard");
            System.out.println("Total readings: " + systemStats.readTotalRWLock());
            System.out.println("Highest temperature: " + systemStats.readHighTempRWLock());
        }, 0, 15, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            executor.shutdown();
            monitor.shutdown();
        }));
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Hub started. Waiting for sensors...");
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(new ClientHandler(socket, systemStats));
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}
