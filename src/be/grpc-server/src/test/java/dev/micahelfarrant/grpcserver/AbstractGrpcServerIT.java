package dev.micahelfarrant.grpcserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests of the backend gRPC server
 */
public abstract class AbstractGrpcServerIT {

    private static final int GRPC_PORT = 9090;
    private static final String IMAGE_PROPERTY_KEY = "sut.image.name";

    /**
     * System under test - containerised backend gRPC server
     */
    static final GenericContainer<?> sut = new GenericContainer<>(getSutImageName())
            .withExposedPorts(GRPC_PORT)
            .waitingFor(new GrpcWaitStrategy(GRPC_PORT));

    /**
     * Channel to SUT
     */
    static final ManagedChannel sutChannel;

    static {
        sut.start();
        sutChannel = ManagedChannelBuilder.forTarget(getSutTarget())
                .usePlaintext()
                .build();
    }

    private static DockerImageName getSutImageName() {
        var imageName = System.getProperty(IMAGE_PROPERTY_KEY);
        return DockerImageName.parse(imageName);
    }

    private static String getSutTarget() {
        return sut.getHost() + ":" + sut.getMappedPort(GRPC_PORT);
    }

}
