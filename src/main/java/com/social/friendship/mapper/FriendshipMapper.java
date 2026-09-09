package com.social.friendship.mapper;

import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface FriendshipMapper {

    default FriendResponse toFriendResponse(Friendship friendship, UUID currentUserId) {
        if(friendship.getAddresseeId().equals(currentUserId)){
            return new FriendResponse(friendship.getRequesterId(), friendship.getStatus());
        }
            return new FriendResponse(friendship.getAddresseeId(), friendship.getStatus());
    };

    default List<FriendResponse> toFriendResponseList(List<Friendship> friendshipList,  UUID currentUserId) {
        List<FriendResponse> friendResponseList = new ArrayList<>();
        for (Friendship friendship : friendshipList) {
            if(friendship.getAddresseeId().equals(currentUserId)){
                friendResponseList.add(new FriendResponse(friendship.getRequesterId(), friendship.getStatus()));
            } else
                friendResponseList.add(new FriendResponse(friendship.getAddresseeId(), friendship.getStatus()));
        }
        return friendResponseList;
    };
}
