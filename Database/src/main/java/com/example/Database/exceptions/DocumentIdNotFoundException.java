package com.example.Database.exceptions;

public class DocumentIdNotFoundException extends Exception{
    public DocumentIdNotFoundException(){
        super("Document Id Not Found");
    }
}