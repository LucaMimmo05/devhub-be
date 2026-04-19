package com.devhub.command.service;

import com.devhub.command.dto.CommandRequest;
import com.devhub.command.dto.CommandResponse;
import com.devhub.command.entity.Command;
import com.devhub.command.repository.CommandRepository;
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
public class CommandService {

    @Inject CommandRepository commandRepository;
    @Inject UserProfileRepository userProfileRepository;

    public List<CommandResponse> getUserCommands(UUID userId, String category, String search) {
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");
        return commandRepository.findByUser(profile.id, category, search)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CommandResponse createCommand(CommandRequest request, UUID userId) {
        UserProfile profile = userProfileRepository.find("user.id", userId).firstResult();
        if (profile == null) throw new NotFoundException("Profile not found");

        Command cmd = new Command();
        cmd.title = request.title;
        cmd.command = request.command;
        cmd.description = request.description;
        cmd.category = request.category;
        cmd.userProfile = profile;

        commandRepository.persist(cmd);
        return toResponse(cmd);
    }

    @Transactional
    public CommandResponse updateCommand(UUID commandId, CommandRequest request, UUID userId) {
        Command cmd = commandRepository.findById(commandId);
        if (cmd == null) throw new NotFoundException("Command not found");
        if (!cmd.userProfile.user.id.equals(userId)) throw new ForbiddenException("Access denied");

        if (request.title != null) cmd.title = request.title;
        if (request.command != null) cmd.command = request.command;
        if (request.description != null) cmd.description = request.description;
        if (request.category != null) cmd.category = request.category;

        commandRepository.persist(cmd);
        return toResponse(cmd);
    }

    @Transactional
    public void deleteCommand(UUID commandId, UUID userId) {
        Command cmd = commandRepository.findById(commandId);
        if (cmd == null) throw new NotFoundException("Command not found");
        if (!cmd.userProfile.user.id.equals(userId)) throw new ForbiddenException("Access denied");
        commandRepository.delete(cmd);
    }

    private CommandResponse toResponse(Command cmd) {
        CommandResponse dto = new CommandResponse();
        dto.id = cmd.id;
        dto.title = cmd.title;
        dto.command = cmd.command;
        dto.description = cmd.description;
        dto.category = cmd.category;
        dto.createdAt = cmd.getCreatedAt();
        dto.updatedAt = cmd.getUpdatedAt();
        return dto;
    }
}
