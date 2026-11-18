package dev.michaelfarrant.buf.maven;

import java.util.Locale;

/**
 * Maps the current JVM's os/arch to the classifier used by the {@code build.buf:buf} artifacts on Maven Central,
 * e.g. {@code linux-x86_64}, {@code osx-aarch_64}. These match the classifiers produced by os-maven-plugin.
 */
record Platform(String os, String arch) {

    static Platform detect() {
        return of(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    static Platform of(String osName, String osArch) {
        return new Platform(normaliseOs(osName), normaliseArch(osArch));
    }

    String classifier() {
        return os + "-" + arch;
    }

    private static String normaliseOs(String osName) {
        String name = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (name.startsWith("linux")) {
            return "linux";
        }
        if (name.startsWith("mac") || name.startsWith("osx") || name.startsWith("darwin")) {
            return "osx";
        }
        if (name.startsWith("windows")) {
            return "windows";
        }
        throw new UnsupportedPlatformException("Unsupported os.name '" + osName + "'");
    }

    private static String normaliseArch(String osArch) {
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        return switch (arch) {
            case "amd64", "x86_64", "x64" -> "x86_64";
            case "aarch64", "arm64" -> "aarch_64";
            default -> throw new UnsupportedPlatformException("Unsupported os.arch '" + osArch + "'");
        };
    }

    static final class UnsupportedPlatformException extends RuntimeException {
        UnsupportedPlatformException(String message) {
            super(message);
        }
    }
}
