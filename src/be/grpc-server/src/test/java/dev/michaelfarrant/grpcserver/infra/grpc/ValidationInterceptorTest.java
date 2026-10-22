package dev.michaelfarrant.grpcserver.infra.grpc;

import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.Violations;
import dev.michaelfarrant.grpc.generated.GetProductDetailsRequest;
import dev.michaelfarrant.grpc.generated.GetProductDetailsResponse;
import dev.michaelfarrant.grpc.generated.ProductServiceGrpc;
import dev.michaelfarrant.grpcserver.ProductGrpcServiceImpl;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the interceptor directly, without a transport, to check that it closes the call correctly and that the
 * handler never acts on a call the interceptor has already closed.
 */
public class ValidationInterceptorTest {

    private static final MethodDescriptor<GetProductDetailsRequest, GetProductDetailsResponse> METHOD =
            ProductServiceGrpc.getGetProductDetailsMethod();

    private static final GetProductDetailsRequest INVALID_REQUEST =
            GetProductDetailsRequest.newBuilder().setProductId("not a uuid").build();

    private final ValidationInterceptor interceptor = new ValidationInterceptor(ValidatorFactory.newBuilder().build());

    @Test
    void invalidRequest_closesOnceWithInvalidArgumentAndViolations_andHandlerIsNotInvoked() throws Exception {
        RecordingServerCall call = new RecordingServerCall();
        ServerCall.Listener<GetProductDetailsRequest> listener = interceptor.interceptCall(call, new Metadata(), productHandler());

        listener.onMessage(INVALID_REQUEST);
        listener.onHalfClose();
        listener.onComplete();

        assertEquals(List.of(Status.Code.INVALID_ARGUMENT), call.closeCodes);
        assertTrue(call.sentMessages.isEmpty());

        com.google.rpc.Status richStatus = StatusProto.fromStatusAndTrailers(call.closeStatus, call.closeTrailers);
        assertEquals(1, richStatus.getDetailsCount());
        Violations violations = richStatus.getDetails(0).unpack(Violations.class);
        assertEquals("string.uuid", violations.getViolations(0).getRuleId());
        assertEquals("product_id", violations.getViolations(0).getField().getElements(0).getFieldName());
    }

    /**
     * isReady() reports outbound flow control. A valid request must reach the handler even when the transport is
     * not ready (e.g. outbound buffer full), otherwise the half-close is lost and the call hangs.
     */
    @ParameterizedTest(name = "isReady={0}")
    @ValueSource(booleans = {true, false})
    void validRequest_isPassedToHandler_regardlessOfOutboundReadiness(boolean ready) {
        RecordingServerCall call = new RecordingServerCall();
        call.ready = ready;
        ServerCall.Listener<GetProductDetailsRequest> listener = interceptor.interceptCall(call, new Metadata(), productHandler());

        String productId = UUID.randomUUID().toString();
        listener.onMessage(GetProductDetailsRequest.newBuilder().setProductId(productId).build());
        listener.onHalfClose();
        listener.onComplete();

        assertEquals(List.of(Status.Code.OK), call.closeCodes);
        assertEquals(1, call.sentMessages.size());
        assertEquals(productId, call.sentMessages.getFirst().getId());
    }

    @Test
    void validatorError_throwsInternal_andHandlerIsNotInvoked() {
        ValidationInterceptor failingInterceptor = new ValidationInterceptor(message -> {
            throw new ExecutionException("boom");
        });
        RecordingServerCall call = new RecordingServerCall();
        ServerCall.Listener<GetProductDetailsRequest> listener = failingInterceptor.interceptCall(call, new Metadata(), productHandler());

        GetProductDetailsRequest request = GetProductDetailsRequest.newBuilder().setProductId(UUID.randomUUID().toString()).build();
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> listener.onMessage(request));

        // Closing the call is left to the exception handling interceptor further up the chain
        assertEquals(Status.Code.INTERNAL, exception.getStatus().getCode());
        assertTrue(call.closeCodes.isEmpty());
        assertTrue(call.sentMessages.isEmpty());
    }

    @Test
    void afterClose_onlyOnCompleteReachesHandler() {
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler handler = new RecordingHandler();
        ServerCall.Listener<GetProductDetailsRequest> listener = interceptor.interceptCall(call, new Metadata(), handler);

        listener.onMessage(INVALID_REQUEST);
        listener.onMessage(GetProductDetailsRequest.newBuilder().setProductId(UUID.randomUUID().toString()).build());
        listener.onMessage(INVALID_REQUEST);
        listener.onReady();
        listener.onHalfClose();
        listener.onComplete();

        assertEquals(List.of(Status.Code.INVALID_ARGUMENT), call.closeCodes);
        assertEquals(List.of("onComplete"), handler.events);
    }

    @Test
    void afterClose_onCancelStillReachesHandler() {
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler handler = new RecordingHandler();
        ServerCall.Listener<GetProductDetailsRequest> listener = interceptor.interceptCall(call, new Metadata(), handler);

        listener.onMessage(INVALID_REQUEST);
        listener.onCancel();

        assertEquals(List.of("onCancel"), handler.events);
    }

    @Test
    void beforeClose_allEventsReachHandler() {
        RecordingServerCall call = new RecordingServerCall();
        RecordingHandler handler = new RecordingHandler();
        ServerCall.Listener<GetProductDetailsRequest> listener = interceptor.interceptCall(call, new Metadata(), handler);

        listener.onReady();
        listener.onMessage(GetProductDetailsRequest.newBuilder().setProductId(UUID.randomUUID().toString()).build());
        listener.onHalfClose();
        listener.onComplete();

        assertTrue(call.closeCodes.isEmpty());
        assertEquals(List.of("onReady", "onMessage", "onHalfClose", "onComplete"), handler.events);
    }

    @SuppressWarnings("unchecked")
    private static ServerCallHandler<GetProductDetailsRequest, GetProductDetailsResponse> productHandler() {
        return (ServerCallHandler<GetProductDetailsRequest, GetProductDetailsResponse>) new ProductGrpcServiceImpl()
                .bindService()
                .getMethod(METHOD.getFullMethodName())
                .getServerCallHandler();
    }

    /**
     * Handler that records which inbound events it receives.
     */
    private static final class RecordingHandler implements ServerCallHandler<GetProductDetailsRequest, GetProductDetailsResponse> {

        final List<String> events = new ArrayList<>();

        @Override
        public ServerCall.Listener<GetProductDetailsRequest> startCall(
                ServerCall<GetProductDetailsRequest, GetProductDetailsResponse> call, Metadata headers) {
            return new ServerCall.Listener<>() {
                @Override
                public void onMessage(GetProductDetailsRequest message) {
                    events.add("onMessage");
                }

                @Override
                public void onHalfClose() {
                    events.add("onHalfClose");
                }

                @Override
                public void onCancel() {
                    events.add("onCancel");
                }

                @Override
                public void onComplete() {
                    events.add("onComplete");
                }

                @Override
                public void onReady() {
                    events.add("onReady");
                }
            };
        }
    }

    /**
     * Mirrors the state checks in grpc-java's ServerCallImpl: sending or closing after close is an error, and
     * isReady() is false once closed, otherwise it reflects transport readiness.
     */
    private static final class RecordingServerCall extends ServerCall<GetProductDetailsRequest, GetProductDetailsResponse> {

        final List<Status.Code> closeCodes = new ArrayList<>();
        final List<GetProductDetailsResponse> sentMessages = new ArrayList<>();
        Status closeStatus;
        Metadata closeTrailers;
        boolean ready = true;

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
            assertOpen();
        }

        @Override
        public void sendMessage(GetProductDetailsResponse message) {
            assertOpen();
            sentMessages.add(message);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            assertOpen();
            closeCodes.add(status.getCode());
            closeStatus = status;
            closeTrailers = trailers;
        }

        @Override
        public boolean isReady() {
            return closeCodes.isEmpty() && ready;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<GetProductDetailsRequest, GetProductDetailsResponse> getMethodDescriptor() {
            return METHOD;
        }

        private void assertOpen() {
            if (!closeCodes.isEmpty()) {
                throw new IllegalStateException("call already closed");
            }
        }
    }
}
