 package com.example.Database.file;

import com.example.Database.index.Index;
import com.example.Database.index.PropertyIndex;
import com.example.Database.model.AccountReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class FileService {
    private static final String FILE_PATH = "src/main/resources/databases";
    private static String DB_DIRECTORY;

    private FileService() {
    }

    public static JSONObject readSchema(String collectionName) {
        try {
            File schemaFile = getSchemaPath(collectionName);
            String schemaString = Files.readString(Paths.get(schemaFile.getAbsolutePath()));
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(schemaString);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }


    public static JsonNode readAdminCredentialsFromJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File jsonFile = new File(getUserJsonPath("admin"));
            return objectMapper.readTree(jsonFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading admin credentials from JSON", e);
        }
    }


    public static void saveAccountDirectory(Map<String, AccountReference> accountDirectory) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FileService.getRootFile() + "/accountDirectory.json"))) {
            for (Map.Entry<String, AccountReference> entry : accountDirectory.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue().getDatabaseName() + "," +
                        entry.getValue().getCollectionName() + "," + entry.getValue().getDocumentId());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving the account directory: " + e.getMessage());
        }
    }


    public static Map<String, AccountReference> loadAccountDirectory() {
        Map<String, AccountReference> directory = new ConcurrentHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FileService.getRootFile() + "/accountDirectory.json"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                directory.put(parts[0], new AccountReference(parts[1], parts[2], parts[3]));
            }
        } catch (FileNotFoundException e) {
            System.err.println("Account directory not found. This is normal if the system is just set up.");
        } catch (IOException e) {
            System.err.println("Error loading the account directory: " + e.getMessage());
        }
        return directory;
    }


    public static Map<String, String> readIndexFile(String path) {
        File file = new File(path);
        Map<String, String> indexData = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int separatorPosition = line.indexOf(",");
                if (separatorPosition == -1) continue;
                String key = line.substring(0, separatorPosition).trim();
                String value = line.substring(separatorPosition + 1).trim();
                indexData.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("Error reading the file.");
            e.printStackTrace();
        }
        return indexData;
    }


    public static synchronized void createDirectoryIfNotExist(Path path){
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to create " + path.getFileName());
            e.printStackTrace();
        }
    }

    public static int getJSONArrayLength(File collectionFile) {
        JSONArray jsonArray = FileService.readJsonArrayFile(collectionFile);
        return jsonArray != null ? jsonArray.size() : 0;
    }


    public static File getRootFile(){
        return new File(FILE_PATH);
    }

    public static File getCollectionFile(String collectionName) {
        return new File(DB_DIRECTORY + "/" + collectionName + ".json");
    }

    public static File getSchemaPath(String collectionName) {
        return new File(DB_DIRECTORY + "/schemas/" + collectionName + ".json");
    }

    public static File getDatabasePath() {
        return new File( DB_DIRECTORY);
    }

    public static String getIndexFilePath(String collectionName) {
        String dirPath = DB_DIRECTORY + "/indexes/" + collectionName + "_indexes";
        createDirectoryIfNotExist(new File(dirPath).toPath());
        return dirPath + "/" + collectionName + "_index.txt";
    }

    public static String getPropertyIndexFilePath(String collectionName, String propertyName) {
        String dirPath = DB_DIRECTORY + "/indexes/" + collectionName + "_indexes";
        createDirectoryIfNotExist(new File(dirPath).toPath());
        return dirPath + "/" + collectionName + "_" + propertyName + "_property_index.txt";
    }

    public static void setDatabaseDirectory(String dbName) {
        DB_DIRECTORY = FILE_PATH + "/" + dbName;
    }

    public static boolean isIndexFile(String fileName) {
        return fileName.endsWith("_index.txt");
    }

    public static boolean isPropertyIndexFile(String fileName) {
        return fileName.endsWith("_property_index.txt");
    }


    public static boolean isFileExists(String filePath){
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    public static List<String> getAllKnownDatabases() {
        List<String> databases = new ArrayList<>();
        File databaseRoot = new File(getRootFile().toURI());
        File[] dbDirectories = databaseRoot.listFiles(File::isDirectory);
        System.out.println(Arrays.toString(dbDirectories) + " database directories");
        if (dbDirectories != null) {
            for (File dbDir : dbDirectories) {
                databases.add(dbDir.getName());
            }
        }
        return databases;
    }

    public static JSONObject readJsonObjectFile(File file) {
        JSONParser parser = new JSONParser();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            if (file.length() == 0) {
                return new JSONObject();
            }
            Object obj = parser.parse(reader);
            return (JSONObject) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.err.println("Error while reading JSON file: " + e.getMessage());
            return null;
        }
    }

    public static void writeJsonObjectFile(File file, JSONObject jsonObject) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error while creating the file: " + e.getMessage());
                return;
            }
        }
        try (RandomAccessFile stream = new RandomAccessFile(file, "rw");
             FileChannel channel = stream.getChannel()) {
            channel.truncate(0);
            byte[] bytes = jsonObject.toJSONString().getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.clear();
            buffer.put(bytes);
            buffer.flip();
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while writing JSON file: " + e.getMessage());
        }
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
            System.err.println("Error while reading JSON file: " + e.getMessage());
            return null;
        }
    }

    public static boolean writeJsonArrayFile(Path filePath, JSONArray jsonArray) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, jsonArray.toJSONString());
            return true;
        } catch (IOException e) {
            System.err.println("Error while writing JSON file: " + e.getMessage());
            return false;
        }
    }

    public static void appendToIndexFile(String path, Object key, String value) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            List<String> lines = Files.readAllLines(filePath);
            boolean keyExists = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",");
                if (parts[0].equals(key.toString())) {
                    lines.set(i, key + "," + value);
                    keyExists = true;
                    break;
                }
            }
            if (!keyExists) {
                lines.add(key + "," + value);
            }
            Files.write(filePath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                try (Stream<Path> entries = Files.list(path)) {
                    entries.forEach(entry -> {
                        try {
                            deleteDirectoryRecursively(entry);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
            Files.delete(path);
        }
    }

    public static void rewriteIndexFile(String collectionName, Index index) {
        File file = new File(getIndexFilePath(collectionName));
        List<Map.Entry<String, String>> allEntries = index.getBPlusTree().getAllEntries();
        if (allEntries.isEmpty()) {
            if (FileService.isFileExists(file.getPath())) {
                file.delete();
            }
            return;
        }
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Map.Entry<String, String> entry : allEntries) {
                    writer.write(entry.getKey() + "," + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void rewritePropertyIndexFile(String path, PropertyIndex propertyIndex) {
        try {
            File file = new File(path);
            List<Map.Entry<String, String>> allEntries = propertyIndex.getBPlusTree().getAllEntries();
            if (allEntries.isEmpty()) {
                if (FileService.isFileExists(file.getPath())) {
                    file.delete();
                    File parentDir = file.getParentFile();
                    if (isDirectoryExists(parentDir) && Objects.requireNonNull(parentDir.list()).length == 0) {
                        System.out.println("indexes directory is empty deleting it..........");
                        deleteDirectoryRecursively(file.toPath());
                    }
                }
                return;
            }
            file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Map.Entry<String, String> entry : allEntries) {
                    writer.write(entry.getKey() + "," + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject addIdToDocument(JSONObject inputJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(inputJson.toString());
            if (!node.has("_id")) {  // Check if the _id field already exists
                System.out.println("generating a new id..................");
                UUID uuid = UUID.randomUUID();      // Generate a new id if it doesn't exist
                ((ObjectNode) node).put("_id", uuid.toString());
            }
            Map<String, Object> resultMap = mapper.convertValue(node, Map.class);
            return new JSONObject(resultMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUserJsonPath(String fileName){
        String usersDirectoryPath="src/main/resources/static";
        return usersDirectoryPath + "/" + fileName + ".json";
    }

    public static boolean isDirectoryExists(File file) {
        return !file.exists() || !file.isDirectory();
    }
}