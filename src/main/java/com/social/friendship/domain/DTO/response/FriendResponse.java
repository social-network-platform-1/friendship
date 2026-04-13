package com.social.friendship.domain.DTO.response;

import com.social.friendship.domain.model.FriendshipStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
public record FriendResponse(
        @NotBlank UUID userId,
        @NotBlank FriendshipStatus status
) {
}
