package com.example.Database.services.database;
import com.example.Database.file.DatabaseFileOperations;
import com.example.Database.model.ApiResponse;
import com.example.Database.model.InMemoryDatabase;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DatabaseService {

   public ApiResponse createDB(String dbName) {
       //soft addition
       InMemoryDatabase.getInstance().createDatabase(dbName);
       //hard addition
       return DatabaseFileOperations.createDatabase();
   }

    public ApiResponse deleteDB(String dbName) {
        //soft delete
        InMemoryDatabase.getInstance().deleteDatabase(dbName);
        //hard delete
        return DatabaseFileOperations.deleteDatabase(dbName);
    }

    public List<String> readDBs() {
        List<String> inMemoryDbs = InMemoryDatabase.getInstance().readDatabases();
        Set<String> uniqueDatabases = new HashSet<>(inMemoryDbs);
        List<String> inFileDBS = DatabaseFileOperations.readDatabases();
        uniqueDatabases.addAll(inFileDBS);
        return new ArrayList<>(uniqueDatabases);
    }
}