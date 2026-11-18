package dev.michaelfarrant.buf.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

/**
 * Locates the buf binary to run. Either an explicitly configured executable, or the
 * {@code build.buf:buf:<version>:exe:<classifier>} artifact resolved from the project's remote repositories.
 */
class BufExecutableResolver {

    static final String BUF_GROUP_ID = "build.buf";
    static final String BUF_ARTIFACT_ID = "buf";
    static final String BUF_EXTENSION = "exe";

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepositories;
    private final Log log;

    BufExecutableResolver(RepositorySystem repositorySystem,
                          RepositorySystemSession session,
                          List<RemoteRepository> remoteRepositories,
                          Log log) {
        this.repositorySystem = repositorySystem;
        this.session = session;
        this.remoteRepositories = remoteRepositories;
        this.log = log;
    }

    /**
     * @param bufExecutable explicit binary to use, may be null
     * @param bufVersion    version of build.buf:buf to resolve when no explicit binary is given
     * @param classifier    platform classifier, e.g. linux-x86_64
     * @param installDir    directory the resolved binary is copied to and made executable in
     */
    Path resolve(File bufExecutable, String bufVersion, String classifier, Path installDir)
            throws MojoExecutionException {

        if (bufExecutable != null) {
            return useConfiguredExecutable(bufExecutable.toPath());
        }

        if (bufVersion == null || bufVersion.isBlank()) {
            throw new MojoExecutionException(
                    "No buf version configured. Set <bufVersion> (or -Dbuf.version), or point <bufExecutable> at a local binary");
        }

        Artifact artifact = new DefaultArtifact(BUF_GROUP_ID, BUF_ARTIFACT_ID, classifier, BUF_EXTENSION, bufVersion);
        Path resolved = resolveArtifact(artifact);

        String fileName = "buf-" + bufVersion + "-" + classifier + (classifier.startsWith("windows") ? ".exe" : "");
        return install(resolved, installDir.resolve(fileName));
    }

    private Path useConfiguredExecutable(Path executable) throws MojoExecutionException {
        if (!Files.isRegularFile(executable)) {
            throw new MojoExecutionException("Configured bufExecutable does not exist: " + executable);
        }
        if (!Files.isExecutable(executable)) {
            throw new MojoExecutionException("Configured bufExecutable is not executable: " + executable);
        }
        log.debug("Using configured buf executable " + executable);
        return executable;
    }

    private Path resolveArtifact(Artifact artifact) throws MojoExecutionException {
        log.debug("Resolving " + artifact);
        try {
            ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);
            return repositorySystem.resolveArtifact(session, request).getArtifact().getFile().toPath();
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(
                    "Unable to resolve buf binary " + artifact + ". Check that the version and classifier exist on "
                            + "Maven Central (https://repo1.maven.org/maven2/build/buf/buf/), override the platform "
                            + "with -Dbuf.classifier, or use a local binary with -Dbuf.executable", e);
        }
    }

    /**
     * Files in the local repository are not executable, so copy the binary somewhere we can chmod it.
     */
    private Path install(Path source, Path target) throws MojoExecutionException {
        try {
            if (!Files.exists(target) || Files.size(target) != Files.size(source)) {
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Copied " + source + " to " + target);
            }

            PosixFileAttributeView posix = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            if (posix != null) {
                posix.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x"));
            }
            return target;
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to install buf binary to " + target, e);
        }
    }
}
