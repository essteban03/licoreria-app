/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;
import Controler.MongoConnection;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {
    private final MongoCollection<Document> col;

    public ProductoDAO() {
        MongoDatabase db = MongoConnection.getDatabase();
        col = db.getCollection("productos");
    }

    // Crear
    public Producto crearProducto(Producto p) {
        Document doc = new Document()
                .append("nombre", p.getNombre())
                .append("precio", p.getPrecio())
                .append("stock", p.getStock())
                .append("imagenPath", p.getImagenPath());
        col.insertOne(doc);
        ObjectId id = doc.getObjectId("_id");
        p.setId(id.toHexString());
        return p;
    }

    // Leer todos
    public List<Producto> listarProductos() {
        List<Producto> lista = new ArrayList<>();
        FindIterable<Document> docs = col.find();
        for (Document d : docs) {
            lista.add(documentToProducto(d));
        }
        return lista;
    }

    // Buscar por nombre (contiene)
    public List<Producto> buscarPorNombre(String texto) {
        List<Producto> lista = new ArrayList<>();
        FindIterable<Document> docs = col.find(Filters.regex("nombre", ".*" + texto + ".*", "i"));
        for (Document d : docs) {
            lista.add(documentToProducto(d));
        }
        return lista;
    }

    // Actualizar por id
    public boolean actualizarProducto(Producto p) {
        if (p.getId() == null) return false;
        ObjectId oid = new ObjectId(p.getId());
        Document update = new Document()
                .append("nombre", p.getNombre())
                .append("precio", p.getPrecio())
                .append("stock", p.getStock())
                .append("imagenPath", p.getImagenPath());
        var result = col.replaceOne(Filters.eq("_id", oid), new Document(update).append("_id", oid));
        return result.getModifiedCount() > 0 || result.getMatchedCount() > 0;
    }

    // Eliminar por id
    public boolean eliminarProducto(String id) {
        ObjectId oid = new ObjectId(id);
        var result = col.deleteOne(Filters.eq("_id", oid));
        return result.getDeletedCount() > 0;
    }

    private Producto documentToProducto(Document d) {
        Producto p = new Producto();
        ObjectId oid = d.getObjectId("_id");
        p.setId(oid != null ? oid.toHexString() : null);
        p.setNombre(d.getString("nombre"));
        p.setPrecio(d.getDouble("precio") != null ? d.getDouble("precio") : d.getInteger("precio", 0));
        p.setStock(d.getInteger("stock", 0));
        p.setImagenPath(d.getString("imagenPath"));
        return p;
    }
}
