package dev.micahelfarrant.grpcserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

public class GrpcWaitStrategy extends AbstractWaitStrategy {

    private final int port;
    private static final Logger log = LoggerFactory.getLogger(GrpcWaitStrategy.class);

    public GrpcWaitStrategy(int port) {
        this.port = port;
    }

    @Override
    protected void waitUntilReady() {

        String host = waitStrategyTarget.getHost();
        int containerPort = waitStrategyTarget.getMappedPort(port);
        String target = host + ":" + containerPort;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        HealthGrpc.HealthBlockingStub healthStub = HealthGrpc.newBlockingStub(channel);

        int timeoutSeconds = (int) startupTimeout.getSeconds();

        retryUntilSuccess(timeoutSeconds, TimeUnit.SECONDS, () -> {

            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("")
                    .build();

            HealthCheckResponse response = healthStub.check(request);
            log.info("Health check status was {}", response.getStatus());

            if (response.getStatus() == HealthCheckResponse.ServingStatus.SERVING){
                return true;
            }

            throw new RuntimeException("Not ready!");
        });

        channel.shutdownNow();
    }
}
