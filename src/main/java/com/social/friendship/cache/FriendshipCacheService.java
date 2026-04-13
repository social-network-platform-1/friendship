package com.social.friendship.cache;

import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipCacheService {

    private  final RedisTemplate<String, Object> friendshipRedisTemplate;

    public Boolean isFriend(UUID userId, UUID otherId) {
        String key = "friendship:" + userId.toString() + ":" + otherId.toString();
        String isFriend = String.valueOf(friendshipRedisTemplate.opsForValue().get(key));
        if(isFriend.equals(FriendshipStatus.ACCEPTED.toString())) {
            return true;
        } else if (isFriend.equals(FriendshipStatus.PENDING.toString())) {
            return false;
        }
        return false;
    }

    public void cacheFriendship(UUID userA, UUID userB, FriendshipStatus status) {
        String key1 = "friendship:" + userA.toString() + ":" + userB.toString();
        String key2 = "friendship:" + userB.toString() + ":" + userA.toString();
        friendshipRedisTemplate.opsForValue().set(key1, status);
        friendshipRedisTemplate.opsForValue().set(key2, status);
        log.info("Friendship cache is set");
    }

    public void removeFriendship(UUID userA, UUID userB) {
        String key1 = "friendship:" + userA.toString() + ":" + userB.toString();
        String key2 = "friendship:" + userB.toString() + ":" + userA.toString();
        friendshipRedisTemplate.delete(key1);
        friendshipRedisTemplate.delete(key2);
    }

    public List<FriendResponse> getFriends(UUID userId) {
        String key = "friendships:" + userId.toString();
        List<FriendResponse> friendships = (List<FriendResponse>) friendshipRedisTemplate.opsForValue().get(key);
        return friendships;
    }

    public void cacheFriendsList(UUID userId, List<FriendResponse> friends) {
        String key = "friendships:" + userId.toString();
        friendshipRedisTemplate.opsForValue().set(key, friends);
    }

}
