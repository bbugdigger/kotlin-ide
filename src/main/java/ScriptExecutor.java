import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.concurrent.*;

public class ScriptExecutor {

    public interface OutputListener {
        void onOutput(String line);
        void onError(String line);
        void onComplete(int exitCode);
    }

    private static final String SCRIPT_FILE = "tmp.kts";

    private String scriptContent;
    private OutputListener listener;
    private Process process;
    private ExecutorService executor;
    private boolean running;

    public ScriptExecutor(String scriptContent, OutputListener listener) {
        this.scriptContent = scriptContent;
        this.listener = listener;
        this.executor = Executors.newFixedThreadPool(2);
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                execute();
            } catch (Exception e) {
                listener.onError("Error: " + e.getMessage());
                listener.onComplete(-1);
            }
        }).start();
    }

    private void execute() {
        int exitCode = -1;

        try {
            File scriptFile = writeScriptToFile();
            String scriptPath = scriptFile.getAbsolutePath();

            ProcessBuilder pb = new ProcessBuilder();
            pb.command("cmd.exe", "/c", "kotlinc", "-script", scriptPath);

            // Set working directory to temporary OS folder
            pb.directory(new File(System.getProperty("java.io.tmpdir")));

            pb.redirectErrorStream(false);
            process = pb.start();

            // Stream stdout
            Future<?> stdoutFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && running) {
                        listener.onOutput(line);
                    }
                } catch (IOException e) {
                    if (running) {
                        listener.onError("Error reading stdout: " + e.getMessage());
                    }
                }
            });

            // Stream stderr
            Future<?> stderrFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && running) {
                        listener.onError(line);
                    }
                } catch (IOException e) {
                    if (running) {
                        listener.onError("Error reading stderr: " + e.getMessage());
                    }
                }
            });

            exitCode = process.waitFor();
            stdoutFuture.get(2, TimeUnit.SECONDS);
            stderrFuture.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            listener.onError("Script execution interrupted");
            exitCode = -1;
        } catch (TimeoutException e) {
            listener.onError("Timeout waiting for output streams");
        } catch (IOException e) {
            listener.onError("Error executing script: " + e.getMessage());
            listener.onError("Make sure 'kotlinc' is installed and in your PATH environment variable");
            exitCode = -1;
        } catch (Exception e) {
            listener.onError("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            exitCode = -1;
        } finally {
            running = false;
            cleanup();
            listener.onComplete(exitCode);
        }
    }

    private File writeScriptToFile() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File file = new File(tempDir, SCRIPT_FILE);
        Files.write(file.toPath(), scriptContent.getBytes());
        return file;
    }

    public void stop() {
        running = false;
        if (process != null && process.isAlive())
            process.destroyForcibly();

        cleanup();
    }

    private void cleanup() {
        if (executor != null && !executor.isShutdown())
            executor.shutdownNow();

        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            File scriptFile = new File(tempDir, SCRIPT_FILE);
            Files.deleteIfExists(scriptFile.toPath());
        } catch (IOException e) {}
    }
}
