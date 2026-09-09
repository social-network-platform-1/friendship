package com.social.friendship.controller;

import com.social.friendship.api.FriendshipController;
import com.social.friendship.application.FriendshipService;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.exception.FriendshipAlreadyExistsException;
import com.social.friendship.exception.FriendshipNotFoundException;
import com.social.friendship.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendshipController.class)
@AutoConfigureMockMvc(addFilters = false)
class FriendshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendshipService friendshipService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;


    @Test
    void sendRequest_shouldCallService() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        mockMvc.perform(
                        post("/api/v1/friendship/{userId}", targetUserId)
                )
                .andExpect(status().isOk());

        verify(friendshipService)
                .sendRequest(
                        currentUserId,
                        targetUserId
                );
    }


    @Test
    void accept_shouldCallService() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        mockMvc.perform(
                        post(
                                "/api/v1/friendship/{userId}/accept",
                                requesterId
                        )
                )
                .andExpect(status().isOk());

        verify(friendshipService)
                .acceptRequest(
                        currentUserId,
                        requesterId
                );
    }


    @Test
    void reject_shouldCallService() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        mockMvc.perform(
                        post(
                                "/api/v1/friendship/{userId}/reject",
                                requesterId
                        )
                )
                .andExpect(status().isOk());

        verify(friendshipService)
                .rejectRequest(
                        currentUserId,
                        requesterId
                );
    }


    @Test
    void remove_shouldCallService() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        mockMvc.perform(
                        delete(
                                "/api/v1/friendship/{userId}",
                                targetUserId
                        )
                )
                .andExpect(status().isOk());

        verify(friendshipService)
                .removeFriend(
                        currentUserId,
                        targetUserId
                );
    }


    @Test
    void getFriends_shouldReturnFriends() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        List<FriendResponse> friends = List.of(
                new FriendResponse(
                        friendId,
                        FriendshipStatus.ACCEPTED
                )
        );

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        when(friendshipService.getFriends(currentUserId))
                .thenReturn(friends);

        mockMvc.perform(
                        get("/api/v1/friendship")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId")
                        .value(friendId.toString()))
                .andExpect(jsonPath("$[0].status")
                        .value("ACCEPTED"));

        verify(friendshipService)
                .getFriends(currentUserId);
    }


    @Test
    void getStatus_shouldReturnStatus() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        when(friendshipService.getStatus(
                currentUserId,
                targetUserId
        )).thenReturn(FriendshipStatus.ACCEPTED);

        mockMvc.perform(
                        get(
                                "/api/v1/friendship/{userId}/status",
                                targetUserId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(content().string("\"ACCEPTED\""));

        verify(friendshipService)
                .getStatus(
                        currentUserId,
                        targetUserId
                );
    }

    @Test
    void sendRequest_shouldReturnConflictWhenFriendshipAlreadyExists()
            throws Exception {

        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        doThrow(new FriendshipAlreadyExistsException(
                "Friendship request already exists"
        )).when(friendshipService)
                .sendRequest(
                        currentUserId,
                        targetUserId
                );

        mockMvc.perform(
                        post(
                                "/api/v1/friendship/{userId}",
                                targetUserId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        content().string(
                                "Friendship request already exists"
                        )
                );
    }

    @Test
    void sendRequest_shouldReturnNotFoundWhenFriendshipNotFound()
            throws Exception {

        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        doThrow(new FriendshipNotFoundException(
                "Friendship not found"
        )).when(friendshipService)
                .sendRequest(
                        currentUserId,
                        targetUserId
                );

        mockMvc.perform(
                        post(
                                "/api/v1/friendship/{userId}",
                                targetUserId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().string("Friendship not found")
                );
    }

    @Test
    void sendRequest_shouldReturnBadRequestWhenUserIdIsInvalid()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/friendship/not-a-uuid")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(friendshipService);
    }

    @Test
    void accept_shouldReturnNotFoundWhenFriendshipNotFound()
            throws Exception {

        UUID currentUserId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        doThrow(new FriendshipNotFoundException(
                "Friendship not found"
        )).when(friendshipService)
                .acceptRequest(
                        currentUserId,
                        requestUserId
                );

        mockMvc.perform(
                        post(
                                "/api/v1/friendship/{userId}/accept",
                                requestUserId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().string("Friendship not found")
                );
    }

    @Test
    void reject_shouldReturnNotFoundWhenFriendshipNotFound()
            throws Exception {

        UUID currentUserId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        doThrow(new FriendshipNotFoundException(
                "Friendship not found"
        )).when(friendshipService)
                .rejectRequest(
                        currentUserId,
                        requestUserId
                );

        mockMvc.perform(
                        post(
                                "/api/v1/friendship/{userId}/reject",
                                requestUserId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().string("Friendship not found")
                );
    }

    @Test
    void remove_shouldReturnNotFoundWhenFriendshipNotFound()
            throws Exception {

        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(currentUserId);

        doThrow(new FriendshipNotFoundException(
                "Friendship not found"
        )).when(friendshipService)
                .removeFriend(
                        currentUserId,
                        targetUserId
                );

        mockMvc.perform(
                        delete(
                                "/api/v1/friendship/{userId}",
                                targetUserId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().string("Friendship not found")
                );
    }


}
