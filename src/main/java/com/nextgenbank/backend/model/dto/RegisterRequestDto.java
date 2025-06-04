package com.nextgenbank.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public class RegisterRequestDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "BSN is required")
    private String bsnNumber;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    private String transferLimit;

    public RegisterRequestDto() {}

    public RegisterRequestDto(String firstName, String lastName, String email, String password, 
                             String bsnNumber, String phoneNumber, String transferLimit) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.bsnNumber = bsnNumber;
        this.phoneNumber = phoneNumber;
        this.transferLimit = transferLimit;
    }

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBsnNumber() { return bsnNumber; }
    public void setBsnNumber(String bsnNumber) { this.bsnNumber = bsnNumber; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getTransferLimit() { return transferLimit; }
    public void setTransferLimit(String transferLimit) { this.transferLimit = transferLimit; }
}
