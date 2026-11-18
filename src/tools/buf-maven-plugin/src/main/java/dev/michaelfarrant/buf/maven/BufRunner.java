package dev.michaelfarrant.buf.maven;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs buf as a child process, forwarding its combined stdout/stderr to the Maven log.
 */
class BufRunner {

    private final Log log;

    BufRunner(Log log) {
        this.log = log;
    }

    /**
     * @return the process exit code
     */
    int run(List<String> command, Path workingDirectory, Map<String, String> environment)
            throws IOException, InterruptedException {

        log.debug("Running " + String.join(" ", command) + " in " + workingDirectory);

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true);
        if (environment != null) {
            builder.environment().putAll(environment);
        }

        Process process = builder.start();
        try (BufferedReader output = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = output.readLine()) != null) {
                log.info("[buf] " + line);
            }
        }
        return process.waitFor();
    }
}
