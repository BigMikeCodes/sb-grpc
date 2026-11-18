package dev.michaelfarrant.buf.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs {@code buf generate} and adds the generated sources to the compile source roots.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /**
     * Version of buf to use, resolved as {@code build.buf:buf:<version>:exe:<classifier>}.
     * Not needed when {@code bufExecutable} is set.
     */
    @Parameter(property = "buf.version")
    private String bufVersion;

    /**
     * A local buf binary to use instead of resolving one from the remote repositories.
     */
    @Parameter(property = "buf.executable")
    private File bufExecutable;

    /**
     * Platform classifier of the buf binary, e.g. {@code linux-x86_64}. Detected from the running JVM when unset.
     */
    @Parameter(property = "buf.classifier")
    private String bufClassifier;

    /**
     * Directory buf is run from, normally the one containing {@code buf.yaml} and {@code buf.gen.yaml}.
     */
    @Parameter(property = "buf.workingDirectory", defaultValue = "${project.basedir}")
    private File workingDirectory;

    /**
     * Generation template passed as {@code --template}. Either a path or inline YAML/JSON.
     * Defaults to {@code buf.gen.yaml} in the working directory.
     */
    @Parameter(property = "buf.template")
    private String template;

    /**
     * Input to generate from, overriding any {@code inputs} in the template.
     */
    @Parameter(property = "buf.input")
    private String input;

    /**
     * Base output directory, passed as {@code buf generate -o}. It is prepended to each plugin's {@code out}.
     */
    @Parameter(property = "buf.outputDirectory", defaultValue = "${project.build.directory}/generated-sources/buf")
    private File outputDirectory;

    /**
     * Delete {@code outputDirectory} before generating so stale sources do not survive.
     */
    @Parameter(property = "buf.cleanOutputDirectory", defaultValue = "true")
    private boolean cleanOutputDirectory;

    /**
     * Add {@code outputDirectory} to the project's compile source roots.
     */
    @Parameter(property = "buf.addCompileSourceRoot", defaultValue = "true")
    private boolean addCompileSourceRoot;

    /**
     * Extra arguments appended to the {@code buf generate} command line.
     */
    @Parameter
    private List<String> additionalArgs;

    /**
     * Extra environment variables for the buf process, e.g. {@code BUF_TOKEN}.
     */
    @Parameter
    private Map<String, String> environment;

    /**
     * Skip generation.
     */
    @Parameter(property = "buf.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySession;

    private final RepositorySystem repositorySystem;

    @Inject
    public GenerateMojo(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping buf generate");
            return;
        }

        Path buf = new BufExecutableResolver(
                repositorySystem, repositorySession, project.getRemoteProjectRepositories(), getLog())
                .resolve(bufExecutable, bufVersion, classifier(),
                        Path.of(project.getBuild().getDirectory(), "buf"));

        Path output = outputDirectory.toPath();
        prepareOutputDirectory(output);

        List<String> command = command(buf, output);
        int exitCode;
        try {
            exitCode = new BufRunner(getLog()).run(command, workingDirectory.toPath(), environment);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to run " + buf, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while running buf", e);
        }

        if (exitCode != 0) {
            throw new MojoFailureException("buf generate failed with exit code " + exitCode);
        }

        if (addCompileSourceRoot) {
            project.addCompileSourceRoot(output.toString());
        }
    }

    List<String> command(Path buf, Path output) {
        List<String> command = new ArrayList<>();
        command.add(buf.toString());
        command.add("generate");
        if (input != null && !input.isBlank()) {
            command.add(input);
        }
        if (template != null && !template.isBlank()) {
            command.add("--template");
            command.add(template);
        }
        command.add("--output");
        command.add(output.toString());
        if (additionalArgs != null) {
            command.addAll(additionalArgs);
        }
        return command;
    }

    private String classifier() throws MojoExecutionException {
        if (bufClassifier != null && !bufClassifier.isBlank()) {
            return bufClassifier;
        }
        try {
            return Platform.detect().classifier();
        } catch (Platform.UnsupportedPlatformException e) {
            throw new MojoExecutionException(e.getMessage() + ". Set -Dbuf.classifier or -Dbuf.executable", e);
        }
    }

    private void prepareOutputDirectory(Path output) throws MojoExecutionException {
        try {
            if (cleanOutputDirectory && Files.exists(output)) {
                deleteRecursively(output);
            }
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to prepare output directory " + output, e);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
