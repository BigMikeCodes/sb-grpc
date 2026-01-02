package dev.michaelfarrant.grpcserver.infra.grpc;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import io.grpc.*;
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

        ServerCall.Listener<ReqT> delegate = next.startCall(call, metadata);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                if (message instanceof Message protobufMessage) {

                    try {

                        ValidationResult result = validator.validate(protobufMessage);
                        if (!result.isSuccess()) {
                            logger.debug("Validation failed for method: {}", call.getMethodDescriptor().getFullMethodName());

                            Status status = Status.INVALID_ARGUMENT
                                    .withDescription("Validation violations: " + result.getViolations().toString());
                            call.close(status, new Metadata());
                            return;
                        }

                        logger.debug("Validation successful for method: {}", call.getMethodDescriptor().getFullMethodName());
                    }
                    catch (ValidationException validationException){
                        logger.error("Error validating incoming message", validationException);

                        // Best error status?
                        Status status = Status.INTERNAL.withDescription("Unknown error occurred");
                        call.close(status, new Metadata());
                    }

                }
                super.onMessage(message);
            }
        };
    }
}
