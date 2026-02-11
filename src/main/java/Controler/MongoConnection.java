/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controler;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    // URI local (puedes cambiar a Atlas si luego quieres)
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";

    // BASE DE DATOS UNICA DEL SISTEMA
    private static final String DB_NAME = "licoreria_db";

    private MongoConnection() {
        // Evita que se instancie
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DB_NAME);
            System.out.println(" Conectado a MongoDB - DB: " + DB_NAME);
        }
        return database;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            System.out.println(" Conexion MongoDB cerrada.");
        }
    }
}
