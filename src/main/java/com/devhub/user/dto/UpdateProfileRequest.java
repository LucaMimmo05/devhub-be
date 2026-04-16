package com.devhub.user.dto;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 100)
    public String firstName;

    @Size(max = 100)
    public String lastName;

    @Size(max = 255)
    public String avatarUrl;
}
