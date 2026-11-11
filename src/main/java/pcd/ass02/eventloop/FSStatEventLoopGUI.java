package pcd.ass02.eventloop;

import io.vertx.core.Vertx;
import pcd.ass02.common.FSReport;
import pcd.ass02.common.FSStatView;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FSStatEventLoopGUI {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        FSStatLibEventLoop lib = new FSStatLibEventLoop(vertx);
        String defaultDir = System.getProperty("user.home");
        FSStatView view = new FSStatView("Event-Loop (Vert.x)", defaultDir, 10_000_000L, 5);
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                vertx.close();
            }
        });
        final FSStatLibEventLoop.ScanTask[] activeTask = new FSStatLibEventLoop.ScanTask[1];
        final FSReport[] lastPartialReport = new FSReport[1];
        view.setViewListener(new FSStatView.ViewListener() {
            @Override
            public void onStartRequested(String dirPath, long maxFS, int nBands) {
                lastPartialReport[0] = FSReport.empty(maxFS, nBands);
                long t0 = System.currentTimeMillis();
                activeTask[0] = lib.startInteractiveScan(
                        dirPath,
                        maxFS,
                        nBands,
                        partialReport -> {
                            lastPartialReport[0] = partialReport;
                            view.updateReport(partialReport);
                        }
                );
                activeTask[0].getFuture()
                        .onSuccess(finalReport -> {
                            long elapsed = System.currentTimeMillis() - t0;
                            if (activeTask[0].isStopped()) {
                                view.notifyStopped(finalReport);
                            } else {
                                view.notifyCompleted(finalReport, elapsed);
                            }
                        })
                        .onFailure(view::notifyError);
            }

            @Override
            public void onStopRequested() {
                if (activeTask[0] != null) {
                    activeTask[0].stop();
                }
            }
        });
        view.display();
    }
}