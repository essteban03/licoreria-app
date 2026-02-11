/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

public class Producto {
    private String id;        // _id como String (ObjectId.toHexString())
    private String nombre;
    private double precio;
    private int stock;
    private String imagenPath; // ruta relativa o URL

    public Producto() {}

    public Producto(String id, String nombre, double precio, int stock, String imagenPath) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.imagenPath = imagenPath;
    }

    // getters y setters
    public String getId() { 
        return id; 
    }
    public void setId(String id) { 
        this.id = id; 
    }

    public String getNombre() {
        return nombre; 
    }
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
    }

    public double getPrecio() {
        return precio; 
    }
    public void setPrecio(double precio) { 
        this.precio = precio; 
    }

    public int getStock() { 
        return stock; 
    }
    public void setStock(int stock) { 
        this.stock = stock; 
    }

    public String getImagenPath() { 
        return imagenPath; 
    }
    public void setImagenPath(String imagenPath) { 
        this.imagenPath = imagenPath;
    }
}