package com.social.friendship.controller;

import com.social.friendship.api.FriendshipInternalController;
import com.social.friendship.application.FriendshipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(FriendshipInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
class FriendshipInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendshipService friendshipService;


    @Test
    void areFriends_shouldReturnTrueWhenUsersAreFriends() throws Exception {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        when(friendshipService.areFriends(
                firstUserId,
                secondUserId
        )).thenReturn(true);

        mockMvc.perform(
                        get("/internal/v1/friendships/check")
                                .param("firstUserId", firstUserId.toString())
                                .param("secondUserId", secondUserId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(friendshipService)
                .areFriends(
                        firstUserId,
                        secondUserId
                );
    }


    @Test
    void areFriends_shouldReturnFalseWhenUsersAreNotFriends() throws Exception {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        when(friendshipService.areFriends(
                firstUserId,
                secondUserId
        )).thenReturn(false);

        mockMvc.perform(
                        get("/internal/v1/friendships/check")
                                .param("firstUserId", firstUserId.toString())
                                .param("secondUserId", secondUserId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(friendshipService)
                .areFriends(
                        firstUserId,
                        secondUserId
                );
    }
}
