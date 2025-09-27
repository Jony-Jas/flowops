package com.flowops.execution_service.repository;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.flowops.execution_service.model.Flow;

public interface FlowRepository extends MongoRepository<Flow, UUID> {   
}
