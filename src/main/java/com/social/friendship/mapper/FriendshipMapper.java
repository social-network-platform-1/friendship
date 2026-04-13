package com.social.friendship.mapper;

import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FriendshipMapper {

    @Mapping(source = "requesterId", target = "userId")
    FriendResponse toFriendResponse(Friendship friendship);

    @Mapping(source = "requesterId", target = "userId")
    List<FriendResponse> toFriendResponseList(List<Friendship> friendshipList);
}
