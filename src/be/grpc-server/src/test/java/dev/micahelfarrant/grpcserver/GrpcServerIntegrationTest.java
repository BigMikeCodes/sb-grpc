package dev.micahelfarrant.grpcserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests of the backend gRPC server
 */
public abstract class GrpcServerIntegrationTest {

    private static final int GRPC_PORT = 9090;

    /**
     * System under test - containerised backend gRPC server
     */
    static final GenericContainer<?> sut = new GenericContainer<>(DockerImageName.parse("bigmikecodes/grpc-server:13a8cf765fdf80b86c1affcaff1b5d2b2f7d94d4"))
            .withExposedPorts(GRPC_PORT)
            .waitingFor(new GrpcWaitStrategy(GRPC_PORT));

    /**
     * Channel to SUT
     */
    static final ManagedChannel sutChannel;

    static {
        sut.start();
        sutChannel = ManagedChannelBuilder.forTarget(sut.getHost() + ":" + sut.getMappedPort(GRPC_PORT))
                .usePlaintext()
                .build();
    }

}
