package com.example.Database.exceptions;

public class DocumentNotFoundException extends Exception{
    public DocumentNotFoundException(){
        super("Document Not Found!");
    }
}