package dev.michaelfarrant.buf.maven;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerateMojoTest {

    private static final Path BUF = Path.of("/opt/buf");
    private static final Path OUT = Path.of("/project/target/generated-sources/buf");

    @Test
    void minimalCommand() {
        GenerateMojo mojo = new GenerateMojo(null);

        assertEquals(
                List.of("/opt/buf", "generate", "--output", OUT.toString()),
                mojo.command(BUF, OUT));
    }

    @Test
    void fullCommand() throws Exception {
        GenerateMojo mojo = new GenerateMojo(null);
        set(mojo, "input", "https://github.com/example/protos.git");
        set(mojo, "template", "buf.gen.java.yaml");
        set(mojo, "additionalArgs", List.of("--include-imports"));

        assertEquals(
                List.of("/opt/buf", "generate", "https://github.com/example/protos.git",
                        "--template", "buf.gen.java.yaml",
                        "--output", OUT.toString(),
                        "--include-imports"),
                mojo.command(BUF, OUT));
    }

    private static void set(Object target, String field, Object value) throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
