package com.example.BankingSystem.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Customer {
    private long accountNumber;
    private String password;
}
