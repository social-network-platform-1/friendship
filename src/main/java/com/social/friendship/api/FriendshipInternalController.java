package com.social.friendship.api;

import com.social.friendship.application.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/friendships")
public class FriendshipInternalController {
    private  final FriendshipService friendshipService;

    @GetMapping("/check")
    public boolean areFriends(@RequestParam UUID firstUserId, @RequestParam UUID secondUserId) {
        return friendshipService.areFriends(firstUserId, secondUserId);
    }
}
