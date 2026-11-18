package pcd.ass02.virtualthreads;

import pcd.ass02.common.FSReport;

public class FSStatVirtualThreadsExample {
    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : System.getProperty("user.dir");
        long maxFS = args.length > 1 ? Long.parseLong(args[1]) : 1_000_000L;
        int nBands = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        System.out.println("FSStat Virtual Threads Example");
        System.out.printf("Scanning directory: %s (MaxFS: %d bytes, NB: %d)%n%n", dir, maxFS, nBands);
        FSStatLibVirtualThreads lib = new FSStatLibVirtualThreads();
        long t0 = System.currentTimeMillis();
        try {
            FSReport report = lib.getFSReport(dir, maxFS, nBands);
            long elapsed = System.currentTimeMillis() - t0;
            System.out.println(report.toFormattedString());
            System.out.printf("Completed in %d ms.%n", elapsed);
        } catch (Exception err) {
            System.err.println("Error during virtual threads scan: " + err.getMessage());
        }
    }
}