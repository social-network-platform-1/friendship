package com.social.friendship.api;

import com.social.friendship.application.FriendshipService;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.mapper.FriendshipMapper;
import com.social.friendship.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friendship")
public class FriendshipController {
    private final FriendshipService  friendshipService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{userId}")
    public void sendRequest(@PathVariable("userId") UUID targetUserId) {
        friendshipService.sendRequest(currentUserProvider.getCurrentUserId(), targetUserId);
    }

    @PostMapping("/{userId}/accept")
    public void accept(@PathVariable("userId") UUID requestUserId) {
        friendshipService.acceptRequest(currentUserProvider.getCurrentUserId(), requestUserId);
    }

    @PostMapping("/{userId}/reject")
    public void reject(@PathVariable("userId") UUID requestUserId) {
        friendshipService.rejectRequest(currentUserProvider.getCurrentUserId(), requestUserId);
    }

    @DeleteMapping("/{userId}")
    public void remove(@PathVariable("userId") UUID targetUserId) {
        friendshipService.removeFriend(currentUserProvider.getCurrentUserId(), targetUserId);
    }

    @GetMapping
    public List<FriendResponse> getFriends() {
        return friendshipService.getFriends(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/{userId}/status")
    public FriendshipStatus getStatus(@PathVariable("userId") UUID targetUserId) {
        return friendshipService.getStatus(currentUserProvider.getCurrentUserId(), targetUserId);
    }
}
