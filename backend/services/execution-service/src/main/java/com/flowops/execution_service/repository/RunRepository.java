package com.flowops.execution_service.repository;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.flowops.execution_service.model.Run;

public interface RunRepository extends MongoRepository<Run, UUID> {    
}
