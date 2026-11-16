package pcd.ass02.reactive;

import io.reactivex.rxjava3.disposables.Disposable;
import pcd.ass02.common.FSReport;
import pcd.ass02.common.FSStatView;

public class FSStatRxGUI {

    public static void main(String[] args) {
        FSStatLibRx lib = new FSStatLibRx();
        String defaultDir = System.getProperty("user.home");
        FSStatView view = new FSStatView("Reactive (RxJava)", defaultDir, 10_000_000L, 5);
        final Disposable[] activeSubscription = new Disposable[1];
        final FSReport[] lastPartialReport = new FSReport[1];
        view.setViewListener(new FSStatView.ViewListener() {

            @Override
            public void onStartRequested(String dirPath, long maxFS, int nBands) {
                lastPartialReport[0] = FSReport.empty(maxFS, nBands);
                long t0 = System.currentTimeMillis();
                activeSubscription[0] = lib.getFSReportStream(dirPath, maxFS, nBands)
                        .subscribe(
                                report -> {
                                    lastPartialReport[0] = report;
                                    view.updateReport(report);
                                },
                                err -> view.notifyError(err),
                                () -> {
                                    long elapsed = System.currentTimeMillis() - t0;
                                    view.notifyCompleted(lastPartialReport[0], elapsed);
                                }
                        );
            }

            @Override
            public void onStopRequested() {
                if (activeSubscription[0] != null && !activeSubscription[0].isDisposed()) {
                    activeSubscription[0].dispose();
                    view.notifyStopped(lastPartialReport[0]);
                }
            }
        });
        view.display();
    }
}