package com.devhub.note.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NoteResponse {

    public UUID id;
    public String title;
    public String content;
    public String emoji;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
