package local.njmu.autologin;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * No foreground service is created in low-visibility mode.
 * Execution of scheduled jobs can be deferred or cancelled by Android.
 */
public final class AuthJobService extends JobService {
    private static final class Running {
        final AtomicBoolean stopped = new AtomicBoolean(false);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
    }
    private final ConcurrentHashMap<Integer, Running> jobs = new ConcurrentHashMap<>();

    @Override
    public boolean onStartJob(JobParameters params) {
        if (!AuthScheduler.isEnabled(this) || !AuthScheduler.isQuietMode(this)) return false;
        final int id = params.getJobId();
        Running running = new Running();
        Running old = jobs.putIfAbsent(id, running);
        if (old != null) {
            running.executor.shutdownNow();
            return false;
        }
        try {
            running.executor.execute(() -> {
                try {
                    if (!running.stopped.get()
                            && AuthScheduler.isEnabled(getApplicationContext())
                            && AuthScheduler.isQuietMode(getApplicationContext())) {
                        new AuthEngine(getApplicationContext(), null, true).check();
                    }
                } catch (Throwable ex) {
                    AuthLog.log(getApplicationContext(), AuthLog.TYPE_ERROR,
                            "系统调度检查失败：" + ex.getClass().getSimpleName());
                } finally {
                    jobs.remove(id, running);
                    running.executor.shutdown();
                    if (!running.stopped.get()) jobFinished(params, false);
                }
            });
        } catch (RuntimeException ex) {
            jobs.remove(id, running);
            running.executor.shutdownNow();
            return false;
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Running running = jobs.remove(params.getJobId());
        if (running != null) {
            running.stopped.set(true);
            running.executor.shutdownNow();
        }
        // A periodic job is automatically scheduled again by the OS.
        // For a stopped one-shot job, request system-managed retry if still enabled.
        return params.getJobId() == AuthScheduler.JOB_TRIGGER
                && AuthScheduler.isEnabled(this) && AuthScheduler.isQuietMode(this);
    }

    @Override
    public void onDestroy() {
        for (Running running : jobs.values()) {
            running.stopped.set(true);
            running.executor.shutdownNow();
        }
        jobs.clear();
        super.onDestroy();
    }
}
