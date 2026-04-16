package com.devhub.note.service;

import com.devhub.note.dto.NoteRequest;
import com.devhub.note.dto.NoteResponse;
import com.devhub.note.entity.Note;
import com.devhub.note.repository.NoteRepository;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NoteService {

    @Inject
    NoteRepository noteRepository;

    @Inject
    UserProfileRepository userProfileRepository;

    public List<NoteResponse> getUserNotes(UUID userId) {
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");
        return noteRepository.findByUserProfileId(profile.id)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NoteResponse createNote(NoteRequest request, UUID userId) {
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");

        Note note = new Note();
        note.title = request.title;
        note.content = request.content;
        note.userProfile = profile;

        noteRepository.persist(note);
        return toResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(UUID noteId, NoteRequest request, UUID userId) {
        Note note = noteRepository.findById(noteId);
        if (note == null) throw new NotFoundException("Note not found");
        if (!note.userProfile.user.id.equals(userId)) throw new ForbiddenException("Access denied");

        if (request.title != null) note.title = request.title;
        if (request.content != null) note.content = request.content;

        noteRepository.persist(note);
        return toResponse(note);
    }

    @Transactional
    public void deleteNote(UUID noteId, UUID userId) {
        Note note = noteRepository.findById(noteId);
        if (note == null) throw new NotFoundException("Note not found");
        if (!note.userProfile.user.id.equals(userId)) throw new ForbiddenException("Access denied");
        noteRepository.delete(note);
    }

    private NoteResponse toResponse(Note note) {
        NoteResponse dto = new NoteResponse();
        dto.id = note.id;
        dto.title = note.title;
        dto.content = note.content;
        dto.createdAt = note.getCreatedAt();
        dto.updatedAt = note.getUpdatedAt();
        return dto;
    }
}
