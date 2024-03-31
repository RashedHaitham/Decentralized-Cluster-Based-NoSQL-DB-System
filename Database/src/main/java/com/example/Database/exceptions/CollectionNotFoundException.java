package com.example.Database.exceptions;

public class CollectionNotFoundException extends Exception{
    public CollectionNotFoundException(){
        super("Collection Not Found!");
    }
}