package com.flowops.execution_engine.grpc;

import com.flowops.common.grpc.PluginRequest;
import com.flowops.common.grpc.PluginMetadata;
import com.flowops.common.grpc.PluginJarResponse;
import com.flowops.common.grpc.PluginServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PluginServiceClient {
    private final ManagedChannel channel;
    private final PluginServiceGrpc.PluginServiceBlockingStub blockingStub;

    public PluginServiceClient(@Value("${pluginservice.host:plugin-service}") String host,
                               @Value("${pluginservice.port:9001}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.blockingStub = PluginServiceGrpc.newBlockingStub(channel);
    }

    public PluginMetadata getMetadata(String pluginId, String id) {
        PluginRequest req = PluginRequest.newBuilder().setPluginId(pluginId).setId(id == null ? "" : id).build();
        return blockingStub.getPluginMetadata(req);
    }

    public byte[] getJarBytes(String pluginId, String id) {
        PluginRequest req = PluginRequest.newBuilder().setPluginId(pluginId).setId(id == null ? "" : id).build();
        PluginJarResponse resp = blockingStub.getPluginJar(req);
        return resp.getJar().toByteArray();
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) channel.shutdown();
    }
}
