package com.example.Bootstrapper.File;

import com.example.Bootstrapper.model.Admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public final class FileServices {
    private static final String usersDirectoryPath="src/main/resources/static";
    private FileServices(){}


    public static void saveUserToJson(String fileName, JSONObject document) {
        String documentPath = usersDirectoryPath + "/" + fileName + ".json";
        JSONArray jsonArray = new JSONArray();
        if (isFileExists(documentPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(documentPath))) {
                JSONParser parser = new JSONParser();
                jsonArray = (JSONArray) parser.parse(reader);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
                System.err.println("Error reading the file: " + e.getMessage());
                return;
            }
        }
        jsonArray.add(document);
        try {
            Files.createDirectories(Path.of(documentPath).getParent());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating directories: " + e.getMessage());
        }
       FileServices.writeJsonArrayFile(new File(documentPath), jsonArray);
    }

    public static void saveAdminToJson(JSONObject document) {
        String documentPath = usersDirectoryPath + "/admin.json";
        try {
            Files.createDirectories(Path.of(documentPath).getParent());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating directories: " + e.getMessage());
        }
        FileServices.writeJsonObjectFile(new File(documentPath), document);
    }

    public static void deleteUserFromJson(String fileName, String accountNumber) {
        String documentPath = getUserJsonPath(fileName);
        JSONArray jsonArray = readJsonArrayFile(new File(documentPath));
        if (jsonArray == null) {
            return;
        }

        // Iterate in reverse to avoid shifting indices when removing
        for (int i = jsonArray.size() - 1; i >= 0; i--) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String userAccountNumber = (String) jsonObject.get("accountNumber");
            if (userAccountNumber.equals(accountNumber)) {
                jsonArray.remove(i);
            }
        }
        FileServices.writeJsonArrayFile(new File(documentPath), jsonArray);
    }

    public static Optional<Admin> getAdminCredentials() {
        ObjectMapper mapper = new ObjectMapper();
        String path = FileServices.adminJsonFilePath();
        File file = new File(path);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            Admin credentials = mapper.readValue(file, Admin.class);
            return Optional.of(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static void writeJsonObjectFile(File file, JSONObject jsonObject) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(jsonObject.toJSONString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error writing to the file: " + e.getMessage());
        }
    }

    public static boolean isFileExists(String filePath){
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    public static JSONArray readJsonArrayFile(File file) {
        JSONParser parser = new JSONParser();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            if (file.length() == 0) {
                return new JSONArray();
            }
            Object obj = parser.parse(reader);
            return (JSONArray) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.err.println("Error while reading JSON file in bootstrapping: " + e.getMessage());
            return null;
        }
    }

    public static void writeJsonArrayFile(File file, JSONArray jsonArray) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(jsonArray.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while writing JSON file: " + e.getMessage());
        }
    }

    public static String getUserJsonPath(String fileName){
        return usersDirectoryPath + "/" + fileName + ".json";
    }

    public static String adminJsonFilePath(){
        return "src/main/resources/static/admin.json";
    }

    public static void updateUserFromJson(String fileName, String accountNumber, String password) {
        String documentPath = getUserJsonPath(fileName);
        JSONArray jsonArray = readJsonArrayFile(new File(documentPath));
        if (jsonArray == null) {
            return;
        }

        // Find the user to update
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String userAccountNumber = (String) jsonObject.get("accountNumber");
            if (userAccountNumber.equals(accountNumber)) {
                // Update the user's data
                jsonObject.put("accountNumber", accountNumber);
                jsonObject.put("password", password);
                // Write back the modified array
                FileServices.writeJsonArrayFile(new File(documentPath), jsonArray);
                return; // User updated
            }
        }

        // If we reach here, the user was not found
        System.out.println("User not found.");
    }
}