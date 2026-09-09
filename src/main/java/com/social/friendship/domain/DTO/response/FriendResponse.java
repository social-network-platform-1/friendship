package com.social.friendship.domain.DTO.response;

import com.social.friendship.domain.model.FriendshipStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
public record FriendResponse(
        @NotNull UUID userId,
        @NotNull FriendshipStatus status
) {
}
