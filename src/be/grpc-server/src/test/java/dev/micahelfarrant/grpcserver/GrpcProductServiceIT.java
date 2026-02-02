package dev.micahelfarrant.grpcserver;

import dev.michaelfarrant.grpc.generated.GetProductDetailsRequest;
import dev.michaelfarrant.grpc.generated.GetProductDetailsResponse;
import dev.michaelfarrant.grpc.generated.ProductServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GrpcProductServiceIT extends AbstractGrpcServerIT {

    private final ProductServiceGrpc.ProductServiceBlockingV2Stub blockingStub =
            ProductServiceGrpc.newBlockingV2Stub(sutChannel);

    @Test
    void getProductDetails_returnsNoErrors_forValidUUIDProductId() {
        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId(UUID.randomUUID().toString()).build();

        assertDoesNotThrow(() -> {
            GetProductDetailsResponse response = blockingStub.getProductDetails(request);
            assertEquals(request.getProductId(), response.getId());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Not a uuid!"})
    void getProductDetails_returnsInvalidArgument_forNonUUIDProductId(String invalidProductId) {
        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId(invalidProductId).build();

        StatusException statusException = assertThrows(StatusException.class, () -> {
            GetProductDetailsResponse _ = blockingStub.getProductDetails(request);
        }, "Expected StatusException to be thrown");

        assertEquals(Status.INVALID_ARGUMENT.getCode(), statusException.getStatus().getCode(), "Expected INVALID_ARGUMENT");
    }

}
