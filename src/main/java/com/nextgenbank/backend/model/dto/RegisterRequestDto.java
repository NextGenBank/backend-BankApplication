package com.nextgenbank.backend.model.dto;

public class RegisterRequestDto {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String bsn;
    private String phone;

    // Constructors
    public RegisterRequestDto() {
    }

    public RegisterRequestDto(String firstName, String lastName, String email, String password, String bsn, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.bsn = bsn;
        this.phone = phone;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getBsn() {
        return bsn;
    }

    public String getPhone() {
        return phone;
    }

    // Setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBsn(String bsn) {
        this.bsn = bsn;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
