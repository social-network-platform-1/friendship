package com.social.friendship.domain.repository;

import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    Optional<Friendship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);
    Optional<Friendship> findByRequesterId(UUID requesterId);
    Optional<Friendship> findByAddresseeId(UUID addresseeId);
    @Query("""
    SELECT f FROM Friendship f
    WHERE (f.requesterId = :userId OR f.addresseeId = :userId)
    AND f.status = :status
""")
    List<Friendship> findFriends(UUID userId, FriendshipStatus status);

}
