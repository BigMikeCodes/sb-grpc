package dev.michaelfarrant.buf.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BufExecutableResolverTest {

    @TempDir
    Path tmp;

    private final BufExecutableResolver resolver =
            new BufExecutableResolver(null, null, List.of(), new SystemStreamLog());

    @Test
    void usesConfiguredExecutable() throws Exception {
        Path buf = Files.createFile(tmp.resolve("buf"));
        buf.toFile().setExecutable(true);

        assertEquals(buf, resolver.resolve(buf.toFile(), null, "linux-x86_64", tmp));
    }

    @Test
    void rejectsMissingConfiguredExecutable() {
        assertThrows(MojoExecutionException.class,
                () -> resolver.resolve(tmp.resolve("missing").toFile(), null, "linux-x86_64", tmp));
    }

    @Test
    void requiresVersionWhenNoExecutableConfigured() {
        assertThrows(MojoExecutionException.class,
                () -> resolver.resolve(null, " ", "linux-x86_64", tmp));
    }
}
