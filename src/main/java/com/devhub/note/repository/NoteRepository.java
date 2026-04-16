package com.devhub.note.repository;

import com.devhub.note.entity.Note;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NoteRepository implements PanacheRepositoryBase<Note, UUID> {

    public List<Note> findByUserProfileId(UUID userProfileId) {
        return list("userProfile.id = ?1 ORDER BY createdAt DESC", userProfileId);
    }
}
