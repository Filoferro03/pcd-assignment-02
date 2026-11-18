package pcd.ass02.reactive;

import io.reactivex.rxjava3.schedulers.Schedulers;
import pcd.ass02.common.FSReport;

public class FSStatRxExample {

    public static void main(String[] args) {
        String preferredDir = "C:\\Users\\filip\\Desktop\\Magistrale";
        String dir = args.length > 0 ? args[0]
                : (java.nio.file.Files.isDirectory(java.nio.file.Path.of(preferredDir))
                ? preferredDir
                : System.getProperty("user.dir"));
        long maxFS = args.length > 1 ? Long.parseLong(args[1]) : 500_000L;
        int nBands = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        System.out.println("FSStat Reactive (RxJava) Example");
        System.out.printf("Scanning directory: %s (MaxFS: %d bytes, NB: %d)%n%n", dir, maxFS, nBands);
        FSStatLibRx lib = new FSStatLibRx();
        long t0 = System.currentTimeMillis();
        try {
            FSReport report = lib.getFSReport(dir, maxFS, nBands).blockingGet();
            long elapsed = System.currentTimeMillis() - t0;
            System.out.println(report.toFormattedString());
            System.out.printf("Completed in %d ms.%n", elapsed);
        } catch (Exception err) {
            System.err.println("Error during reactive scan: " + err.getMessage());
        } finally {
            Schedulers.shutdown();
        }
    }
}