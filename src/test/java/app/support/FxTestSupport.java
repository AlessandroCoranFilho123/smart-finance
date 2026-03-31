package app.support;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FxTestSupport {

    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private FxTestSupport() {
    }

    public static void initToolkit() {
        System.setProperty("prism.order", "sw");

        if (STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    latch.countDown();
                });
                await(latch);
            } catch (IllegalStateException alreadyStarted) {
                Platform.setImplicitExit(false);
            }
        }
    }

    public static void runOnFxThreadAndWait(Runnable runnable) {
        callOnFxThreadAndWait(() -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T callOnFxThreadAndWait(Callable<T> callable) {
        initToolkit();

        if (Platform.isFxApplicationThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);

        try {
            return task.get(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread de teste interrompida", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        } catch (TimeoutException e) {
            throw new RuntimeException("Tempo esgotado aguardando execução JavaFX", e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new RuntimeException("Tempo esgotado iniciando toolkit JavaFX");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread de teste interrompida", e);
        }
    }
}
