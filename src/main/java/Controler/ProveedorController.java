/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
public class ProveedorController {
       private final MongoCollection<Document> colProveedores;

    public ProveedorController() {
        MongoDatabase db = MongoConnection.getDatabase();
        colProveedores = db.getCollection("proveedores");
    }

    public List<Document> getProveedoresActivos() {
        List<Document> lista = new ArrayList<>();

        try (MongoCursor<Document> cur = colProveedores
                .find(Filters.eq("activo", true))
                .iterator()) {

            while (cur.hasNext()) {
                lista.add(cur.next());
            }
        } catch (Exception e) {
            System.out.println("Error obteniendo proveedores: " + e.getMessage());
        }

        return lista;
    }

    public Document getProveedorPorId(String id) {
        try {
            return colProveedores.find(Filters.eq("_id", new ObjectId(id))).first();
        } catch (Exception e) {
            return null;
        }
    }
    public List<Document> getTodosProveedores() {
    MongoCollection<Document> col = MongoConnection.getDatabase().getCollection("proveedores");
    List<Document> lista = new ArrayList<>();
    try (MongoCursor<Document> cursor = col.find().iterator()) {
        while (cursor.hasNext()) {
            lista.add(cursor.next());
        }
    }
    return lista;
}
    
    
}
