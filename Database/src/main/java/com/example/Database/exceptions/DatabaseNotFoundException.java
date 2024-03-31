package com.example.Database.exceptions;

public class DatabaseNotFoundException extends Exception{
    public DatabaseNotFoundException(){
        super("Database Not Found!");
    }
}