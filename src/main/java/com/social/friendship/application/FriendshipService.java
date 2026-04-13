package com.social.friendship.application;

import com.social.friendship.cache.FriendshipCacheService;
import com.social.friendship.domain.DTO.event.FriendAcceptedEvent;
import com.social.friendship.domain.DTO.event.FriendRemovedEvent;
import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.domain.repository.FriendshipRepository;
import com.social.friendship.exception.FriendshipNotFoundException;
import com.social.friendship.mapper.FriendshipMapper;
import com.social.friendship.messaging.KafkaProducer;
import io.micrometer.core.aop.CountedAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final FriendshipCacheService friendshipCacheService;
    private final KafkaProducer kafkaProducer;
    private final FriendshipMapper  friendshipMapper;

    private UUID CurrentAddressedId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }

    public Friendship findFriendship(UUID addressedId) {
        UUID requesterId = CurrentAddressedId();
        Friendship friendship = friendshipRepository.findByRequesterIdAndAddresseeId(requesterId, addressedId)
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        if(!friendship.getAddresseeId().equals(addressedId))
            throw new RuntimeException("Friendship is already friend");
        return friendship;
    }

    public void sendRequest(UUID targetUserId) {
        UUID userId = CurrentAddressedId();
        Boolean isFriend = friendshipCacheService.isFriend(userId, targetUserId);
        if (isFriend) {
            throw new RuntimeException("It is your friend");
        }
        Friendship friendship = friendshipRepository.findByRequesterIdAndAddresseeId(userId, targetUserId)
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        if(friendship.getStatus().equals(FriendshipStatus.PENDING)) throw new RuntimeException("Friendship is already pending");
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipCacheService.cacheFriendship(userId, targetUserId, FriendshipStatus.PENDING);
        kafkaProducer.sendFriendRequestEvent(new FriendRequestSentEvent(userId, targetUserId));
    }

    public void acceptRequest(UUID requesterId) {
        Friendship friendship = findFriendship(requesterId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        friendshipCacheService.cacheFriendship(
                friendship.getRequesterId(),
                friendship.getAddresseeId(),
                FriendshipStatus.ACCEPTED);
        kafkaProducer.sendFriendAcceptedEvent(
                new FriendAcceptedEvent(requesterId,
                        friendship.getAddresseeId()));
    }

    public void rejectRequest(UUID requesterId) {
        Friendship friendship = findFriendship(requesterId);
        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    public void removeFriend(UUID userId) {
        UUID id = CurrentAddressedId();
        Friendship friendship = friendshipRepository.findByRequesterIdAndAddresseeId(id, userId)
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        friendship.setStatus(FriendshipStatus.REMOVED);
        friendshipRepository.save(friendship);
        friendshipCacheService.removeFriendship(id, userId);
        kafkaProducer.sendFriendRemovedEvent(new FriendRemovedEvent(id, userId));
    }

    public List<FriendResponse> getFriends() {
        UUID userId = CurrentAddressedId();
        List<FriendResponse> friendResponseList = (List<FriendResponse>) friendshipCacheService.getFriends(userId);
        if(!friendResponseList.isEmpty()) return friendResponseList;
        friendResponseList = friendshipMapper
                .toFriendResponseList(friendshipRepository.findFriends(userId, FriendshipStatus.ACCEPTED));
        friendshipCacheService.cacheFriendsList(userId, friendResponseList);
        return friendResponseList;
    }

    public FriendshipStatus getStatus(UUID addresseeId) {
        UUID userId =  CurrentAddressedId();
        Friendship friendship = friendshipRepository.findByRequesterIdAndAddresseeId(userId, addresseeId)
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        return friendship.getStatus();
    }
}
