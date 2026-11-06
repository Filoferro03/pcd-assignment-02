package pcd.ass02.common;

import java.util.Arrays;


public final class FSReport {

    private final long totalFiles;
    private final long[] bands;
    private final long maxFS;
    private final int nBands;

    public FSReport(final long totalFiles, final long[] bands, final long maxFS, final int nBands) {
        if (nBands <= 0) {
            throw new IllegalArgumentException("The number of the bands (NB) must be > 0");
        }
        if (maxFS <= 0) {
            throw new IllegalArgumentException("MaxFS must be > 0");
        }
        if (bands == null || bands.length != nBands + 1) {
            throw new IllegalArgumentException("Bands' array must have size NB + 1");
        }
        this.totalFiles = totalFiles;
        this.bands = Arrays.copyOf(bands, bands.length);
        this.maxFS = maxFS;
        this.nBands = nBands;
    }

    public static FSReport empty(final long maxFS, final int nBands) {
        return new FSReport(0L, new long[nBands + 1], maxFS, nBands);
    }

    public static int computeBandIndex(final long fileSize, final long maxFS, final int nBands) {
        if (fileSize > maxFS) {
            return nBands;
        }
        long bandWidth = maxFS / nBands;
        if (bandWidth == 0) {
            return 0;
        }
        int index = (int) (fileSize / bandWidth);
        return Math.min(index, nBands - 1);
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public long[] getBands() {
        return Arrays.copyOf(bands, bands.length);
    }

    public long getMaxFS() {
        return maxFS;
    }

    public int getNBands() {
        return nBands;
    }

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("File System Statistics Report\n");
        sb.append(String.format("Total files found: %d\n", totalFiles));
        sb.append("Size distribution:\n");

        long bandWidth = maxFS / nBands;
        for (int i = 0; i < nBands; i++) {
            long min = i * bandWidth;
            long max = (i == nBands - 1) ? maxFS : ((i + 1) * bandWidth) - 1;
            double pct = totalFiles > 0 ? (bands[i] * 100.0 / totalFiles) : 0.0;
            sb.append(String.format("  Band %2d [%10d - %10d bytes]: %8d files (%5.1f%%)\n",
                    i + 1, min, max, bands[i], pct));
        }

        double overPct = totalFiles > 0 ? (bands[nBands] * 100.0 / totalFiles) : 0.0;
        sb.append(String.format("  Band > MaxFS (> %10d bytes)      : %8d files (%5.1f%%)\n",
                maxFS, bands[nBands], overPct));
        return sb.toString();
    }

    @Override
    public String toString() {
        return toFormattedString();
    }
}