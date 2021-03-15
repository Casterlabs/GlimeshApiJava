package co.casterlabs.glimeshapijava;

public class ThreadHelper {
    private static int threadCount = 0;

    public static void executeAsync(String name, Runnable run) {
        Thread t = new Thread(run);

        t.setName(name + " - GlimeshApi Async Thread #" + threadCount++);
        t.start();
    }

}
