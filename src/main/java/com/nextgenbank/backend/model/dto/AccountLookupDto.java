package com.nextgenbank.backend.model.dto;

import java.util.List;

public class AccountLookupDto {
    private String firstName;
    private String lastName;
    private List<String> ibans;

    public AccountLookupDto(String firstName, String lastName, List<String> ibans) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.ibans = ibans;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public List<String> getIbans() { return ibans; }
}
