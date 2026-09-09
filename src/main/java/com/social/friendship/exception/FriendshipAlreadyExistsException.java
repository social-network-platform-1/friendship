package com.social.friendship.exception;

public class FriendshipAlreadyExistsException extends RuntimeException{
    public FriendshipAlreadyExistsException(String message) {
        super(message);
    }
}
