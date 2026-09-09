package com.social.friendship.application;

import com.social.friendship.cache.FriendshipCacheService;
import com.social.friendship.domain.DTO.event.FriendAcceptedEvent;
import com.social.friendship.domain.DTO.event.FriendRemovedEvent;
import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.domain.repository.FriendshipRepository;
import com.social.friendship.exception.FriendshipAlreadyExistsException;
import com.social.friendship.exception.FriendshipNotFoundException;
import com.social.friendship.mapper.FriendshipMapper;
import com.social.friendship.messaging.KafkaProducer;
import com.social.friendship.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final FriendshipCacheService friendshipCacheService;
    private final OutboxEventService outboxEventService;
    private final FriendshipMapper  friendshipMapper;


    public Friendship findFriendship(UUID requesterId, UUID addressedId) {
        return friendshipRepository
                .findByRequesterIdAndAddresseeId(
                        requesterId,
                        addressedId
                )
                .orElseThrow(()->
                        new FriendshipNotFoundException(
                                "Friendship not found"
                        ));
    }

    @Transactional
    public void sendRequest(UUID currentUserId, UUID targetUserId) {
        if(currentUserId.equals(targetUserId)) {
            throw new  IllegalStateException("Current user id is the same as the target user id");
        }

        if(friendshipCacheService.isFriend(currentUserId, targetUserId)) {
            throw new FriendshipAlreadyExistsException("Users are already friends");
        }

        Optional<Friendship> existingFriendship =
                friendshipRepository
                        .findBetweenUsers(
                                currentUserId,
                                targetUserId,
                                List.of(FriendshipStatus.PENDING,
                                        FriendshipStatus.ACCEPTED,
                                        FriendshipStatus.REJECTED,
                                        FriendshipStatus.REMOVED));

        Friendship friendship;

        if (existingFriendship.isEmpty()) {
            friendship = Friendship.builder()
                    .requesterId(currentUserId)
                    .addresseeId(targetUserId)
                    .status(FriendshipStatus.PENDING)
                    .build();
        } else {
            friendship = existingFriendship.get();

            switch (friendship.getStatus()) {
                case PENDING:
                    throw new FriendshipAlreadyExistsException("Friendship request already exists");
                case ACCEPTED:
                    throw new FriendshipAlreadyExistsException("Friendship request accepted");
                case REJECTED:
                case REMOVED:
                    friendship.setStatus(FriendshipStatus.PENDING);
                    break;
            }
        }

        friendshipRepository.save(friendship);

        friendshipCacheService.cacheFriendship(currentUserId, targetUserId, FriendshipStatus.PENDING);

        outboxEventService.saveEvent(
                "friend.requested",
                new FriendRequestSentEvent(
                        currentUserId,
                        targetUserId
                )
        );
    }

    @Transactional
    public void acceptRequest(UUID currentUserId, UUID requesterId) {
        Friendship friendship = findByRequesterIdAndAddresseeIdAndStatus(
                requesterId,
                currentUserId,
                FriendshipStatus.PENDING);

        friendship.setStatus(FriendshipStatus.ACCEPTED);

        friendshipRepository.save(friendship);

        friendshipCacheService.deleteFriendsCache(requesterId);
        friendshipCacheService.deleteFriendsCache(currentUserId);

        friendshipCacheService.cacheFriendship(
                friendship.getRequesterId(),
                friendship.getAddresseeId(),
                FriendshipStatus.ACCEPTED);

        outboxEventService.saveEvent(
                "friend.accepted",
                new FriendAcceptedEvent(
                        requesterId,
                        friendship.getAddresseeId()
                )
        );
    }

    public void rejectRequest(UUID currentUserId, UUID requesterId) {
        Friendship friendship = findByRequesterIdAndAddresseeIdAndStatus(
                requesterId,
                currentUserId,
                FriendshipStatus.PENDING);

        friendship.setStatus(FriendshipStatus.REJECTED);

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void removeFriend(UUID currentUserId, UUID targetUserId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(
                targetUserId,
                currentUserId,
                List.of(FriendshipStatus.ACCEPTED)
                ).orElseThrow(() ->
                        new FriendshipNotFoundException("Friendship not found"));

        friendship.setStatus(FriendshipStatus.REMOVED);

        friendshipRepository.save(friendship);

        friendshipCacheService.deleteFriendsCache(currentUserId);
        friendshipCacheService.deleteFriendsCache(targetUserId);

        friendshipCacheService.removeFriendship(currentUserId, targetUserId);

        outboxEventService.saveEvent(
                "friend.removed",
                new FriendRemovedEvent(
                        currentUserId,
                        targetUserId
                )
        );
    }

    public List<FriendResponse> getFriends(UUID currentUserId) {
        List<FriendResponse> friendResponseList = friendshipCacheService.getFriends(currentUserId);

        if(!(friendResponseList == null)) return friendResponseList;

        friendResponseList = friendshipMapper
                .toFriendResponseList(
                        friendshipRepository.
                                findByUserIdAndStatus
                                        (currentUserId,
                                        FriendshipStatus.ACCEPTED),
                        currentUserId);

        friendshipCacheService.cacheFriendsList(currentUserId, friendResponseList);

        return friendResponseList;
    }

    public FriendshipStatus getStatus(UUID currentUserId, UUID targetUserId) {
        Friendship friendship = friendshipRepository.
                findBetweenUsers(
                        currentUserId,
                        targetUserId,
                        List.of(FriendshipStatus.ACCEPTED,
                                FriendshipStatus.REJECTED,
                                FriendshipStatus.REMOVED,
                                FriendshipStatus.PENDING))
                .orElseThrow(() ->
                        new FriendshipNotFoundException("Friendship not found"));

        return friendship.getStatus();
    }

    public boolean areFriends(UUID currentUserId, UUID targetUserId) {
        Boolean isFriend = friendshipCacheService.isFriend(currentUserId, targetUserId);
        if (isFriend) {
            return true;
        }

        Optional<Friendship> friendship = friendshipRepository
                .findBetweenUsers(
                        currentUserId,
                        targetUserId,
                        List.of(FriendshipStatus.ACCEPTED));

        if(friendship.isEmpty()) {
            return false;
        }

        friendshipCacheService.cacheFriendship(currentUserId, targetUserId,  FriendshipStatus.ACCEPTED);

        return true;
    }

    private Friendship findByRequesterIdAndAddresseeIdAndStatus(UUID requesterId, UUID currentUserId, FriendshipStatus status) {
         return friendshipRepository
                .findByRequesterIdAndAddresseeIdAndStatus(requesterId, currentUserId, status)
                .orElseThrow(() ->
                        new FriendshipNotFoundException("Friendship not found"));
    }
}
