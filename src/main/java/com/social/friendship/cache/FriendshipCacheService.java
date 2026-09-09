package com.social.friendship.cache;

import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipCacheService {

    @Qualifier("statusRedis")
    private  final RedisTemplate<String, FriendshipStatus> statusRedisTemplate;
    @Qualifier("responseRedis")
    private final RedisTemplate<String, List<FriendResponse>> responseRedisTemplate;

    public Boolean isFriend(UUID userId, UUID otherId) {

        String key = "friendship:" + userId.toString() + ":" + otherId.toString();
        FriendshipStatus isFriend = statusRedisTemplate.opsForValue().get(key);

        if(isFriend == FriendshipStatus.ACCEPTED) {
            return true;
        }

        return false;
    }

    public void cacheFriendship(UUID userA, UUID userB, FriendshipStatus status) {
        String key1 = "friendship:" + userA.toString() + ":" + userB.toString();
        String key2 = "friendship:" + userB.toString() + ":" + userA.toString();

        statusRedisTemplate.opsForValue().set(key1, status);
        statusRedisTemplate.opsForValue().set(key2, status);

        log.info("Friendship cache is set");
    }

    public void removeFriendship(UUID userA, UUID userB) {
        String key1 = "friendship:" + userA.toString() + ":" + userB.toString();
        String key2 = "friendship:" + userB.toString() + ":" + userA.toString();

        statusRedisTemplate.delete(key1);
        statusRedisTemplate.delete(key2);
    }

    public List<FriendResponse> getFriends(UUID userId) {
        String key = "friendships:" + userId.toString();

        List<FriendResponse> friendships = responseRedisTemplate.opsForValue().get(key);

        return friendships;
    }

    public void deleteFriendsCache(UUID userId) {
        String key = "friendships:" + userId.toString();
        responseRedisTemplate.delete(key);
    }

    public void cacheFriendsList(UUID userId, List<FriendResponse> friends) {
        String key = "friendships:" + userId.toString();
        responseRedisTemplate.opsForValue().set(key, friends);
    }

}
