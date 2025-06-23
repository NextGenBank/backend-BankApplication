package com.nextgenbank.backend.model.dto;

public class LoginResponseDto {

    private String token;
    private UserDto user;

    public LoginResponseDto() {
    }

    public LoginResponseDto(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}