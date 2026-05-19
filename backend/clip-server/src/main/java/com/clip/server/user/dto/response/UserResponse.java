package com.clip.server.user.dto.response;

import com.clip.server.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String profileImageUrl;
    private Integer level;
    private Integer exp;
    private Boolean isNewUser;

    public static UserResponse from(User user, boolean isNewUser) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .level(user.getLevel())
                .exp(user.getExp())
                .isNewUser(isNewUser)
                .build();
    }
}
