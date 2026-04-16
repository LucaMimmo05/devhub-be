package com.devhub.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NoteRequest {

    @NotBlank
    @Size(max = 100)
    public String title;

    public String content;
}
