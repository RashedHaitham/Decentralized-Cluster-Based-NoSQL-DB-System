package com.example.Database.exceptions;

public class SchemaNotFoundException extends Exception{
    public SchemaNotFoundException(){
        super("Schema Not Found!");
    }
}