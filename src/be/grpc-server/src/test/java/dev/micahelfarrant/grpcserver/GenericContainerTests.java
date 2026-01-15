package dev.micahelfarrant.grpcserver;

import dev.michaelfarrant.grpc.generated.GetProductDetailsRequest;
import dev.michaelfarrant.grpc.generated.ProductServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class GenericContainerTests {

    private static final int GRPC_PORT = 9090;

    @Container
    private static final GenericContainer<?> SUT = new GenericContainer<>(DockerImageName.parse("bigmikecodes/grpc-server:13a8cf765fdf80b86c1affcaff1b5d2b2f7d94d4"))
            .withExposedPorts(GRPC_PORT)
            .waitingFor(new GrpcWaitStrategy(GRPC_PORT));

    @Test
    void testContainerStarts() throws StatusException {

        String containerTarget = SUT.getHost() + ":" + SUT.getMappedPort(GRPC_PORT);
        ManagedChannel channel = ManagedChannelBuilder.forTarget(containerTarget).usePlaintext().build();

        var blockingStub = ProductServiceGrpc.newBlockingV2Stub(channel);
        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId(UUID.randomUUID().toString()).build();

        var response = blockingStub.getProductDetails(request);

        assertEquals(request.getProductId(), response.getId());
    }

}
