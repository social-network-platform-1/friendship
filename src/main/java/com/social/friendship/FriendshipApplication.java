package com.social.friendship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FriendshipApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendshipApplication.class, args);
    }

}
