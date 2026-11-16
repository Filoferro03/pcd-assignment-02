package pcd.ass02.reactive;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import pcd.ass02.common.FSReport;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class FSStatLibRx {

    public Single<FSReport> getFSReport(final String dir, final long maxFS, final int nBands) {
        return createFileSizeStream(new File(dir))
                .subscribeOn(Schedulers.io())
                .reduce(
                        new MutableAccumulator(maxFS, nBands),
                        MutableAccumulator::accumulate
                )
                .map(MutableAccumulator::toImmutableReport);
    }

    public Flowable<FSReport> getFSReportStream(final String dir, final long maxFS, final int nBands) {
        return createFileSizeStream(new File(dir))
                .subscribeOn(Schedulers.io())
                .scan(
                        new MutableAccumulator(maxFS, nBands),
                        MutableAccumulator::accumulate
                )
                .map(MutableAccumulator::toImmutableReport)
                .sample(80, TimeUnit.MILLISECONDS, true);
    }

    private Flowable<Long> createFileSizeStream(final File rootDir) {
        return Flowable.defer(() -> {
            if (!rootDir.exists() || !rootDir.isDirectory()) {
                return Flowable.error(new IllegalArgumentException("Path is not a valid directory: " + rootDir));
            }
            return exploreDirRx(rootDir);
        });
    }

    private Flowable<Long> exploreDirRx(final File dir) {
        return Flowable.fromCallable(() -> {
                    File[] files = dir.listFiles();
                    return files != null ? files : new File[0];
                })
                .subscribeOn(Schedulers.io())
                .flatMap(files -> Flowable.fromArray(files)
                        .flatMap(entry -> {
                            if (entry.isDirectory()) {
                                return exploreDirRx(entry);
                            } else if (entry.isFile()) {
                                return Flowable.just(entry.length());
                            } else {
                                return Flowable.empty();
                            }
                        })
                )
                .onErrorResumeNext(err -> Flowable.empty());
    }

    private static final class MutableAccumulator {
        private final long maxFS;
        private final int nBands;
        private final long[] bands;
        private long totalFiles;

        MutableAccumulator(final long maxFS, final int nBands) {
            this.maxFS = maxFS;
            this.nBands = nBands;
            this.bands = new long[nBands + 1];
            this.totalFiles = 0L;
        }

        MutableAccumulator accumulate(final long fileSize) {
            int idx = FSReport.computeBandIndex(fileSize, maxFS, nBands);
            this.bands[idx]++;
            this.totalFiles++;
            return this;
        }

        FSReport toImmutableReport() {
            return new FSReport(totalFiles, bands, maxFS, nBands);
        }
    }
}