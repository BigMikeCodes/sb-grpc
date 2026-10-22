package dev.michaelfarrant.grpcserver.infra.grpc;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationInterceptor implements ServerInterceptor {

    private final Validator validator;
    private static final Logger logger = LoggerFactory.getLogger(ValidationInterceptor.class);

    public ValidationInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> next) {

        logger.debug("Starting validation for method: {}", call.getMethodDescriptor().getFullMethodName());

        ServerCall.Listener<ReqT> listener = next.startCall(call, metadata);
        return new RequestValidationServerCallListener<>(listener, validator, call);
    }

    private static class RequestValidationServerCallListener<ReqT, RespT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final Validator validator;
        private final ServerCall<ReqT, RespT> serverCall;

        // Set once this listener has closed the call. ServerCall exposes no "is closed" query (isReady() reflects
        // outbound flow control, not lifecycle), so the closer must remember it. Listener callbacks are serialised
        // by gRPC, so no synchronisation is needed.
        private boolean closed = false;

        protected RequestValidationServerCallListener(
                ServerCall.Listener<ReqT> delegate,
                Validator validator,
                ServerCall<ReqT, RespT> serverCall) {
            super(delegate);
            this.validator = validator;
            this.serverCall = serverCall;
        }

        // Once closed, the handler must not see further inbound events or it will act on (or close) a dead call.
        // onCancel & onComplete are still forwarded so the handler can release any resources it holds.
        @Override
        public void onHalfClose() {
            if (!closed) {
                super.onHalfClose();
            }
        }

        @Override
        public void onReady() {
            if (!closed) {
                super.onReady();
            }
        }

        @Override
        public void onMessage(ReqT message) {

            if (closed) {
                return;
            }

            if (message instanceof Message protobufMessage) {
                try {
                    ValidationResult result = validator.validate(protobufMessage);

                    if (result.isSuccess()) {
                       // Validation passed
                        super.onMessage(message);
                    }
                    else {
                        // Validation failed
                        Status status = Status.newBuilder()
                                            .setCode(Code.INVALID_ARGUMENT.getNumber())
                                            .setMessage(Code.INVALID_ARGUMENT.getValueDescriptor().getName())
                                            .addDetails(Any.pack(result.toProto()))
                                            .build();

                        close(status);
                    }

                } catch (ValidationException e) {

                    Status status = Status.newBuilder()
                            .setCode(Code.INTERNAL.getNumber())
                            .setMessage(Code.INTERNAL.getValueDescriptor().getName())
                            .build();

                    throw StatusProto.toStatusRuntimeException(status);
                }
            }
        }

        private void close(Status status) {
            closed = true;
            StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
            serverCall.close(statusRuntimeException.getStatus(), statusRuntimeException.getTrailers());
        }
    }
}
