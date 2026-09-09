package com.social.friendship;

import com.social.friendship.exception.FriendshipAlreadyExistsException;
import com.social.friendship.exception.FriendshipNotFoundException;
import com.social.friendship.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleFriendshipAlreadyExists_shouldReturnConflict() {
        FriendshipAlreadyExistsException exception =
                new FriendshipAlreadyExistsException(
                        "Friendship request already exists"
                );

        ResponseEntity<String> response =
                handler.handleFriendshipAlreadyExists(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertEquals(
                "Friendship request already exists",
                response.getBody()
        );
    }

    @Test
    void handleFriendshipNotFound_shouldReturnNotFound() {
        FriendshipNotFoundException exception =
                new FriendshipNotFoundException(
                        "Friendship not found"
                );

        ResponseEntity<String> response =
                handler.handleFriendshipNotFound(exception);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertEquals(
                "Friendship not found",
                response.getBody()
        );
    }
}
