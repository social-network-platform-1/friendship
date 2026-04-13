package com.social.friendship.exception;

public class FriendshipNotFoundException extends RuntimeException{
    public FriendshipNotFoundException(String message) {
        super(message);
    }

    public FriendshipNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
