package com.devhub.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommandRequest {

    @NotBlank
    @Size(max = 100)
    public String title;

    @NotBlank
    public String command;

    @Size(max = 500)
    public String description;

    @NotBlank
    @Size(max = 50)
    public String category;
}
