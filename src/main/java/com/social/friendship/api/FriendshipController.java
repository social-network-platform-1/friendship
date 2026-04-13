package com.social.friendship.api;

import com.social.friendship.application.FriendshipService;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.mapper.FriendshipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friendship")
public class FriendshipController {
    private final FriendshipService  friendshipService;
    private FriendshipMapper friendshipMapper;

    @PostMapping("/{userId}")
    public void sendRequest(@PathVariable UUID userId) {
        friendshipService.sendRequest(userId);
    }

    @PostMapping("/{userId}/accept")
    public void accept(@PathVariable UUID userId) {
        friendshipService.acceptRequest(userId);
    }

    @PostMapping("/{userId}/reject")
    public void reject(@PathVariable UUID userId) {
        friendshipService.rejectRequest(userId);
    }

    @DeleteMapping("/{userId}")
    public void remove(@PathVariable UUID userId) {
        friendshipService.removeFriend(userId);
    }

    @GetMapping
    public List<FriendResponse> getFriends() {
        return friendshipService.getFriends();
    }

    @GetMapping("/{userId}/status")
    public FriendshipStatus getStatus(@PathVariable UUID userId) {
        return friendshipService.getStatus(userId);
    }
}
