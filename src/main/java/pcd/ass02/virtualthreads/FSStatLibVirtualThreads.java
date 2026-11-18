package pcd.ass02.virtualthreads;

import pcd.ass02.common.FSReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FSStatLibVirtualThreads {

    public interface ScanTask {
        FSReport get() throws Exception;
        void stop();
        boolean isStopped();
    }

    public FSReport getFSReport(final String dir, final long maxFS, final int nBands) throws Exception {
        ScanTask task = startInteractiveScan(dir, maxFS, nBands, null);
        return task.get();
    }

    public ScanTask startInteractiveScan(final String dir,
                                         final long maxFS,
                                         final int nBands,
                                         final Consumer<FSReport> onProgress) {

        Path rootPath = Path.of(dir);
        final ReentrantLock stateLock = new ReentrantLock();
        final long[] bands = new long[nBands + 1];
        final long[] totalFiles = new long[]{0L};
        final AtomicBoolean stopped = new AtomicBoolean(false);
        final long[] lastUpdateTs = new long[]{0L};
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        Future<FSReport> futureResult = virtualExecutor.submit(() -> {
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                throw new IllegalArgumentException("Path is not a valid directory: " + dir);
            }
            exploreConcurrently(rootPath, maxFS, nBands, bands, totalFiles, stopped, lastUpdateTs, onProgress, stateLock, virtualExecutor);
            stateLock.lock();
            try {
                return new FSReport(totalFiles[0], bands, maxFS, nBands);
            } finally {
                stateLock.unlock();
            }
        });

        return new ScanTask() {
            @Override
            public FSReport get() throws Exception {
                try {
                    return futureResult.get();
                } finally {
                    virtualExecutor.shutdownNow();
                }
            }

            @Override
            public void stop() {
                stopped.set(true);
                virtualExecutor.shutdownNow();
            }

            @Override
            public boolean isStopped() {
                return stopped.get();
            }
        };
    }

    private void exploreConcurrently(final Path currentDir,
                                     final long maxFS,
                                     final int nBands,
                                     final long[] bands,
                                     final long[] totalFiles,
                                     final AtomicBoolean stopped,
                                     final long[] lastUpdateTs,
                                     final Consumer<FSReport> onProgress,
                                     final ReentrantLock stateLock,
                                     final ExecutorService executor) {
        if (stopped.get() || Thread.currentThread().isInterrupted()) {
            return;
        }
        try (Stream<Path> stream = Files.list(currentDir)) {
            List<Future<?>> subtasks = new ArrayList<>();
            stream.forEach(entry -> {
                if (stopped.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    if (attrs.isDirectory()) {
                        Future<?> subtask = executor.submit(() -> {
                            exploreConcurrently(entry, maxFS, nBands, bands, totalFiles,
                                    stopped, lastUpdateTs, onProgress, stateLock, executor);
                        });
                        subtasks.add(subtask);
                    } else if (attrs.isRegularFile()) {
                        long size = attrs.size();
                        int bandIdx = FSReport.computeBandIndex(size, maxFS, nBands);
                        stateLock.lock();
                        boolean notifyNeeded = false;
                        FSReport snapshot = null;
                        try {
                            bands[bandIdx]++;
                            totalFiles[0]++;
                            if (onProgress != null) {
                                long now = System.currentTimeMillis();
                                if (totalFiles[0] % 250 == 0 || (now - lastUpdateTs[0]) >= 100) {
                                    lastUpdateTs[0] = now;
                                    notifyNeeded = true;
                                    snapshot = new FSReport(totalFiles[0], bands, maxFS, nBands);
                                }
                            }
                        } finally {
                            stateLock.unlock();
                        }
                        if (notifyNeeded && snapshot != null && onProgress != null) {
                            onProgress.accept(snapshot);
                        }
                    }
                } catch (IOException ignored) {
                }
            });
            for (Future<?> subtask : subtasks) {
                try {
                    subtask.get();
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }
}