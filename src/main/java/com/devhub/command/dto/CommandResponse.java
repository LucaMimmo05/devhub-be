package com.devhub.command.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CommandResponse {
    public UUID id;
    public String title;
    public String command;
    public String description;
    public String category;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
