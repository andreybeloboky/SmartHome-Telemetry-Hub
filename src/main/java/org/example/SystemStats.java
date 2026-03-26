package org.example;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SystemStats {

    @Getter
    private int totalReadingProcessed;
    @Getter
    private double highestTemperatureProcessed;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger atomicTotalReading = new AtomicInteger();
    private final AtomicReference<Double> atomicHighTemp = new AtomicReference<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();


    public void updateWithLock(double newTemp) {
        lock.lock();
        try {
            totalReadingProcessed++;
            if (newTemp > highestTemperatureProcessed) {
                highestTemperatureProcessed = newTemp;
            }
        } finally {
            lock.unlock();
        }
    }

    public synchronized void updateSynchronized(double newTemp) {
        totalReadingProcessed++;
        if (newTemp > highestTemperatureProcessed) {
            highestTemperatureProcessed = newTemp;
        }
    }

    public void updateAtomic(double newTemp) {
        atomicTotalReading.incrementAndGet();
        atomicHighTemp.updateAndGet(current -> Math.max(current, newTemp));
    }

    public void updateRWLock(double newTemp) {
        rwLock.writeLock().lock();
        try {
            totalReadingProcessed++;
            if (newTemp > highestTemperatureProcessed) {
                highestTemperatureProcessed = newTemp;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public double readHighTempRWLock() {
        rwLock.readLock().lock();
        try {
            return highestTemperatureProcessed;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public double readTotalRWLock() {
        rwLock.readLock().lock();
        try {
            return totalReadingProcessed;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
