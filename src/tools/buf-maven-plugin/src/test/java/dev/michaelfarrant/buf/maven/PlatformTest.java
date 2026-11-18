package dev.michaelfarrant.buf.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformTest {

    @ParameterizedTest
    @CsvSource({
            "Linux, amd64, linux-x86_64",
            "Linux, aarch64, linux-aarch_64",
            "Mac OS X, x86_64, osx-x86_64",
            "Mac OS X, aarch64, osx-aarch_64",
            "Windows 11, amd64, windows-x86_64",
            "Windows Server 2022, arm64, windows-aarch_64",
    })
    void mapsToMavenCentralClassifier(String osName, String osArch, String expected) {
        assertEquals(expected, Platform.of(osName, osArch).classifier());
    }

    @Test
    void rejectsUnknownOs() {
        assertThrows(Platform.UnsupportedPlatformException.class, () -> Platform.of("SunOS", "amd64"));
    }

    @Test
    void rejectsUnknownArch() {
        assertThrows(Platform.UnsupportedPlatformException.class, () -> Platform.of("Linux", "ppc64le"));
    }
}
