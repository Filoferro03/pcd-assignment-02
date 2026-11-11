package pcd.ass02.eventloop;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import pcd.ass02.common.FSReport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FSStatLibEventLoop {

    private final Vertx vertx;

    public FSStatLibEventLoop(final Vertx vertx) {
        this.vertx = vertx;
    }

    public interface ScanTask {
        Future<FSReport> getFuture();
        void stop();
        boolean isStopped();
    }

    public Future<FSReport> getFSReport(final String dir, final long maxFS, final int nBands) {
        return startInteractiveScan(dir, maxFS, nBands, null).getFuture();
    }

    public ScanTask startInteractiveScan(final String dir,
                                         final long maxFS,
                                         final int nBands,
                                         final Consumer<FSReport> onProgress) {
        Promise<FSReport> reportPromise = Promise.promise();
        Context eventLoopContext = vertx.getOrCreateContext();

        final long[] bands = new long[nBands + 1];
        final long[] totalFiles = new long[]{0L};
        final boolean[] stopped = new boolean[]{false};
        final long[] lastUpdateTs = new long[]{0L};

        eventLoopContext.runOnContext(v -> {
            FileSystem fs = vertx.fileSystem();
            fs.props(dir).onComplete(ar -> {
                if (ar.failed()) {
                    reportPromise.fail(ar.cause());
                    return;
                }
                if (!ar.result().isDirectory()) {
                    reportPromise.fail(new IllegalArgumentException("Path is not a directory: " + dir));
                    return;
                }
                exploreDirectory(dir, maxFS, nBands, bands, totalFiles, stopped, lastUpdateTs, onProgress, fs)
                        .onComplete(scanRes -> {
                            FSReport finalReport = new FSReport(totalFiles[0], bands, maxFS, nBands);
                            if (onProgress != null) {
                                onProgress.accept(finalReport);
                            }
                            if (scanRes.failed()) {
                                reportPromise.fail(scanRes.cause());
                            } else {
                                reportPromise.complete(finalReport);
                            }
                        });
            });
        });

        return new ScanTask() {
            @Override
            public Future<FSReport> getFuture() {
                return reportPromise.future();
            }

            @Override
            public void stop() {
                eventLoopContext.runOnContext(v -> stopped[0] = true);
            }

            @Override
            public boolean isStopped() {
                return stopped[0];
            }
        };
    }

    private Future<Void> exploreDirectory(final String currentDir,
                                          final long maxFS,
                                          final int nBands,
                                          final long[] bands,
                                          final long[] totalFiles,
                                          final boolean[] stopped,
                                          final long[] lastUpdateTs,
                                          final Consumer<FSReport> onProgress,
                                          final FileSystem fs) {
        if (stopped[0]) {
            return Future.succeededFuture();
        }
        return fs.readDir(currentDir).<Void>compose(entries -> {
            if (stopped[0]) {
                return Future.succeededFuture();
            }
            List<Future<Void>> childrenFutures = new ArrayList<>(entries.size());
            for (String entryPath : entries) {
                Future<Void> childFuture = fs.props(entryPath).<Void>compose(props -> {
                    if (stopped[0]) {
                        return Future.succeededFuture();
                    }
                    if (props.isDirectory()) {
                        return exploreDirectory(entryPath, maxFS, nBands, bands, totalFiles,
                                stopped, lastUpdateTs, onProgress, fs);
                    } else if (props.isRegularFile()) {
                        int bandIdx = FSReport.computeBandIndex(props.size(), maxFS, nBands);
                        bands[bandIdx]++;
                        totalFiles[0]++;
                        if (onProgress != null) {
                            long now = System.currentTimeMillis();
                            if (totalFiles[0] % 250 == 0 || (now - lastUpdateTs[0]) >= 100) {
                                lastUpdateTs[0] = now;
                                onProgress.accept(new FSReport(totalFiles[0], bands, maxFS, nBands));
                            }
                        }
                        return Future.succeededFuture();
                    } else {
                        return Future.succeededFuture();
                    }
                }).recover(err -> Future.succeededFuture());
                childrenFutures.add(childFuture);
            }
            return Future.all(childrenFutures).<Void>mapEmpty();
        }).recover(err -> Future.succeededFuture());
    }
}