package dev.micahelfarrant.grpcserver;

import build.buf.validate.Violation;
import build.buf.validate.Violations;
import com.google.protobuf.InvalidProtocolBufferException;
import dev.michaelfarrant.grpc.generated.GetProductDetailsRequest;
import dev.michaelfarrant.grpc.generated.GetProductDetailsResponse;
import dev.michaelfarrant.grpc.generated.ProductServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GrpcProductServiceIT extends AbstractGrpcServerIT {

    private final ProductServiceGrpc.ProductServiceBlockingV2Stub blockingStub =
            ProductServiceGrpc.newBlockingV2Stub(sutChannel);

    @Test
    void getProductDetails_returnsOk_forValidUUIDProductId() {
        String productId = UUID.randomUUID().toString();
        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId(productId).build();

        GetProductDetailsResponse response = assertDoesNotThrow(() -> blockingStub.getProductDetails(request));

        assertEquals(productId, response.getId());
    }

    @Test
    void getProductDetails_returnsInvalidArgument_forNonUUIDProductId() throws InvalidProtocolBufferException {
        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId("Not a uuid!").build();

        StatusException statusException = assertThrows(StatusException.class, () -> blockingStub.getProductDetails(request));

        assertEquals(Status.Code.INVALID_ARGUMENT, statusException.getStatus().getCode());

        com.google.rpc.Status status = StatusProto.fromThrowable(statusException);
        assertNotNull(status);
        Violations violations = status.getDetails(0).unpack(Violations.class);
        assertEquals(1, violations.getViolationsCount());

        Violation violation = violations.getViolations(0);
        assertEquals("string.uuid", violation.getRuleId());
        assertEquals("must be a valid UUID", violation.getMessage());
        assertEquals("product_id", violation.getField().getElements(0).getFieldName());
    }

    @Test
    void getProductDetails_returnsInvalidArgument_forEmptyProductId() throws InvalidProtocolBufferException {
        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId("").build();

        StatusException statusException = assertThrows(StatusException.class, () -> blockingStub.getProductDetails(request));

        assertEquals(Status.Code.INVALID_ARGUMENT, statusException.getStatus().getCode());

        com.google.rpc.Status status = StatusProto.fromThrowable(statusException);
        assertNotNull(status);
        Violations violations = status.getDetails(0).unpack(Violations.class);
        assertEquals(1, violations.getViolationsCount());

        Violation violation = violations.getViolations(0);
        assertEquals("string.uuid_empty", violation.getRuleId());
        assertEquals("value is empty, which is not a valid UUID", violation.getMessage());
        assertEquals("product_id", violation.getField().getElements(0).getFieldName());
    }

}
