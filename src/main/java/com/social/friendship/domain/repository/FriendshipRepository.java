package com.social.friendship.domain.repository;

import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    Optional<Friendship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);
//    Optional<Friendship> findByRequesterId(UUID requesterId);
//    Optional<Friendship> findByAddresseeId(UUID addresseeId);
    @Query("""
    SELECT f FROM Friendship f
    WHERE (f.requesterId = :userId OR f.addresseeId = :userId)
    AND f.status = :status
""")
    List<Friendship> findByUserIdAndStatus( 
            @Param("userId") UUID userId,
            @Param("status") FriendshipStatus status);

    Optional<Friendship> findByRequesterIdAndAddresseeIdAndStatus(
            UUID requesterId,
            UUID addresseeId,
            FriendshipStatus status);

    @Query("""
    SELECT f
    FROM Friendship f
    WHERE
        (
            (f.requesterId = :user1 AND f.addresseeId = :user2)
            OR
            (f.requesterId = :user2 AND f.addresseeId = :user1)
        )
        AND f.status IN :statuses
    """)
    Optional<Friendship> findBetweenUsers(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2,
            @Param("statuses") Collection<FriendshipStatus> statuses
    );
}
