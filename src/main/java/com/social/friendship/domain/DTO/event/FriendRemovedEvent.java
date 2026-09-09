package com.social.friendship.domain.DTO.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FriendRemovedEvent(
        @NotNull UUID userId,
        @NotNull UUID targetId
) {}
