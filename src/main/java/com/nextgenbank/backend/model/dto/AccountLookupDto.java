package com.nextgenbank.backend.model.dto;

import java.util.List;

public class AccountLookupDto {
    private String firstName;
    private String lastName;
    private List<String> ibans;

    // ✅ No-argument constructor required for Jackson
    public AccountLookupDto() {
    }

    public AccountLookupDto(String firstName, String lastName, List<String> ibans) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.ibans = ibans;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<String> getIbans() {
        return ibans;
    }

    // Optional but useful: Setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setIbans(List<String> ibans) {
        this.ibans = ibans;
    }
}
