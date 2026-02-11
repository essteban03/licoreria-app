package Controler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class PedidoController {

    private final MongoCollection<Document> collection;

    public PedidoController() {
        MongoDatabase db = MongoConnection.getDatabase();
        collection = db.getCollection("pedidos");
    }

    public boolean guardarPedido(Document pedido) {
        try {
            collection.insertOne(pedido);
            return true;
        } catch (Exception e) {
            System.out.println("Error al guardar pedido: " + e.getMessage());
            return false;
        }
    }
}