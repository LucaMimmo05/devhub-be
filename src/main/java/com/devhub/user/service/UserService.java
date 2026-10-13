package com.devhub.user.service;

import com.devhub.user.entity.User;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import com.devhub.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserProfileRepository userProfileRepository;

    /** Profiles whose username contains {@code username}; accounts pending deletion are left out. */
    public List<UserProfile> searchProfiles(String username) {
        if (username == null || username.isBlank()) return List.of();
        return userProfileRepository
                .find("lower(username) like lower(?1) and user.deletionRequestedAt is null", "%" + username + "%")
                .list();
    }

    /** Deletes the account and everything it owns, immediately. Only the purge job calls this. */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) {
            userRepository.delete(user);
            return;
        }

        // The GitHub link references the profile without a cascade: remove it first.
        userProfileRepository.getEntityManager()
                .createQuery("DELETE FROM GithubConnection g WHERE g.userProfile = :profile")
                .setParameter("profile", profile)
                .executeUpdate();

        // Nullify tasks assigned to this profile (tasks created by others)
        // so we don't violate FK constraints when cascade deletes createdBy tasks
        userProfileRepository.getEntityManager()
                .createQuery("UPDATE Task t SET t.assignedTo = NULL WHERE t.assignedTo = :profile")
                .setParameter("profile", profile)
                .executeUpdate();

        // Cascade ALL on User → UserProfile → Note, Command, Project, ProjectMember, Task(createdBy)
        userRepository.delete(user);
    }
}
