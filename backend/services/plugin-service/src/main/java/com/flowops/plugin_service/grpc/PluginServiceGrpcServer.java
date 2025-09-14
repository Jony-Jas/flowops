package com.flowops.plugin_service.grpc;

import com.flowops.common.grpc.PluginServiceGrpc;
import com.flowops.common.grpc.PluginRequest;
import com.flowops.common.grpc.PluginMetadata;
import com.flowops.common.grpc.PluginIO;
import com.flowops.common.grpc.PluginIOType;
import com.flowops.common.grpc.PluginJarResponse;
import com.flowops.plugin_service.domain.entities.PluginMetaData;
import com.flowops.plugin_service.services.PluginService;

import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
public class PluginServiceGrpcServer extends PluginServiceGrpc.PluginServiceImplBase {

    @Autowired
    private PluginService pluginService;

    @Override
    public void getPluginMetadata(PluginRequest request, StreamObserver<PluginMetadata> responseObserver) {
        log.info("Received gprc Request for getPluginMetadata");
        try {
            PluginMetaData entity = pluginService.getPluginById(request.getPluginId(), request.getId());

            PluginMetadata response = PluginMetadata.newBuilder()
                    .setId(entity.getId().toString())
                    .setPluginId(entity.getPluginId())
                    .setVersion(entity.getVersion())
                    .setDescription(entity.getDescription())
                    .setAuthor(entity.getAuthor())
                    .setUploadTime(entity.getUploadTime().toString())
                    .addAllInputs(entity.getInputs().stream()
                        .map(io -> PluginIO.newBuilder()
                            .setName(io.getName())
                            .setType(PluginIOType.valueOf(io.getType().name())) // enum to string (if proto uses string)
                            .build())
                        .collect(Collectors.toList()))
                    .addAllOutputs(entity.getOutputs().stream()
                        .map(io -> PluginIO.newBuilder()
                            .setName(io.getName())
                            .setType(PluginIOType.valueOf(io.getType().name()))
                            .build())
                        .collect(Collectors.toList()))
                    .setJarFileUrl(entity.getJarFileUrl())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getPluginJar(PluginRequest request, StreamObserver<PluginJarResponse> responseObserver) {
        try {
            InputStream jarStream = pluginService.downloadPlugin(request.getPluginId(), request.getId());

            byte[] jarBytes = jarStream.readAllBytes(); // simple approach, consider streaming later
            PluginJarResponse response = PluginJarResponse.newBuilder()
                    .setJar(com.google.protobuf.ByteString.copyFrom(jarBytes))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
