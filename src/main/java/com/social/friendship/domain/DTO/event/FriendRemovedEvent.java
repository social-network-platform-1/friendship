package com.social.friendship.domain.DTO.event;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record FriendRemovedEvent(
        @NotBlank UUID userId,
        @NotBlank UUID targetId
) {}
