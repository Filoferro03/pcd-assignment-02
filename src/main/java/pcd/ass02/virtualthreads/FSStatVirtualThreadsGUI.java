package pcd.ass02.virtualthreads;

import pcd.ass02.common.FSReport;
import pcd.ass02.common.FSStatView;

public class FSStatVirtualThreadsGUI {
    public static void main(String[] args) {
        FSStatLibVirtualThreads lib = new FSStatLibVirtualThreads();
        String defaultDir = System.getProperty("user.home");
        FSStatView view = new FSStatView("Virtual Threads", defaultDir, 10_000_000L, 5);
        final FSStatLibVirtualThreads.ScanTask[] activeTask = new FSStatLibVirtualThreads.ScanTask[1];
        final FSReport[] lastPartialReport = new FSReport[1];
        view.setViewListener(new FSStatView.ViewListener() {

            @Override
            public void onStartRequested(String dirPath, long maxFS, int nBands) {
                lastPartialReport[0] = FSReport.empty(maxFS, nBands);
                long t0 = System.currentTimeMillis();
                Thread.ofVirtual().start(() -> {
                    try {
                        activeTask[0] = lib.startInteractiveScan(
                                dirPath,
                                maxFS,
                                nBands,
                                partialReport -> {
                                    lastPartialReport[0] = partialReport;
                                    view.updateReport(partialReport);
                                }
                        );
                        FSReport finalReport = activeTask[0].get();
                        long elapsed = System.currentTimeMillis() - t0;
                        if (activeTask[0].isStopped()) {
                            view.notifyStopped(finalReport);
                        } else {
                            view.notifyCompleted(finalReport, elapsed);
                        }
                    } catch (Exception err) {
                        view.notifyError(err);
                    }
                });
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