package pcd.ass02.eventloop;

import io.vertx.core.Vertx;

public class FSStatEventLoopExample {

    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : System.getProperty("user.dir");
        long maxFS = args.length > 1 ? Long.parseLong(args[1]) : 1_000_000L;
        int nBands = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        System.out.println("FSStat Event-Loop (Vert.x) Example ");
        System.out.printf("Scanning directory: %s (MaxFS: %d bytes, NB: %d)%n%n", dir, maxFS, nBands);
        Vertx vertx = Vertx.vertx();
        FSStatLibEventLoop lib = new FSStatLibEventLoop(vertx);
        long t0 = System.currentTimeMillis();
        lib.getFSReport(dir, maxFS, nBands)
                .onSuccess(report -> {
                    long elapsed = System.currentTimeMillis() - t0;
                    System.out.println(report.toFormattedString());
                    System.out.printf("Completed in %d ms.%n", elapsed);
                    vertx.close();
                })
                .onFailure(err -> {
                    System.err.println("Error during FS scan: " + err.getMessage());
                    vertx.close();
                });
    }
}