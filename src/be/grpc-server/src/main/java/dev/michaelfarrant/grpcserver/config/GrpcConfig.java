package dev.michaelfarrant.grpcserver.config;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import dev.michaelfarrant.grpcserver.infra.grpc.ValidationInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;

@Configuration
public class GrpcConfig {

    @Bean
    public Validator grpcValidator() {
        return ValidatorFactory.newBuilder().build();
    }

    @Bean
    @Order(100)
    @GlobalServerInterceptor
    public ServerInterceptor validationInterceptor(Validator validator) {
        return new ValidationInterceptor(validator);
    }

}
