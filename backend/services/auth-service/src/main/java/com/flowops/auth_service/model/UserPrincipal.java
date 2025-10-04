package com.flowops.auth_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    private String userId;
    private String username;
    private String email;
    private List<String> roles;
}
