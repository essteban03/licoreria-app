package View;

import Controler.MongoConnection;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class PagoFrame extends JFrame {

    private JComboBox<String> metodoPagoCombo;
    private JTextField nombreField, direccionField, telefonoField;
    private JTextField numeroTarjetaField, cvvField, fechaExpField;
    private JButton confirmarBtn;
    private JPanel panelDatos, panelTarjeta;

    private final ArrayList<Document> carrito;
    private final double subtotal;
    private final double descuento;
    private final double iva;
    private final String cuponAplicado;

    private final String usuarioActual;

    private MongoCollection<Document> productosCol;
    private MongoCollection<Document> pedidosCol;

    public PagoFrame(ArrayList<Document> carrito, double subtotal, double descuento, double iva, String cuponAplicado, String usuarioActual) {
        this.carrito = carrito != null ? carrito : new ArrayList<>();
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.iva = iva;
        this.cuponAplicado = (cuponAplicado != null) ? cuponAplicado : "";
        this.usuarioActual = (usuarioActual != null && !usuarioActual.trim().isEmpty()) ? usuarioActual : "anonimo";

        try {
            MongoDatabase db = MongoConnection.getDatabase();
            productosCol = db.getCollection("productos");
            pedidosCol = db.getCollection("pedidos");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error BD: " + e.getMessage());
        }

        initUI();
    }

    private void initUI() {
        setTitle("Pasarela de Pago");
        setSize(550, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBackground(EstilosApp.COLOR_FONDO);
        contenido.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Confirmar Pedido");
        titulo.setFont(EstilosApp.FUENTE_TITULO);
        titulo.setAlignmentX(CENTER_ALIGNMENT);
        contenido.add(titulo);

        double total = subtotal + iva - descuento;

        JLabel lblTotal = new JLabel(String.format("TOTAL A PAGAR: $%.2f", total));
        lblTotal.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        lblTotal.setForeground(EstilosApp.COLOR_VINO);
        lblTotal.setAlignmentX(CENTER_ALIGNMENT);

        contenido.add(Box.createVerticalStrut(10));
        contenido.add(lblTotal);
        contenido.add(Box.createVerticalStrut(20));

        metodoPagoCombo = new JComboBox<>(new String[]{"Efectivo", "Tarjeta de Crédito"});
        metodoPagoCombo.addActionListener(e -> toggleTarjeta());
        contenido.add(crearInput("Método de Pago:", metodoPagoCombo));

        panelDatos = new JPanel();
        panelDatos.setLayout(new BoxLayout(panelDatos, BoxLayout.Y_AXIS));
        panelDatos.setOpaque(false);

        nombreField = new JTextField();
        direccionField = new JTextField();
        telefonoField = new JTextField();

        panelDatos.add(crearInput("Nombre Cliente:", nombreField));
        panelDatos.add(crearInput("Dirección:", direccionField));
        panelDatos.add(crearInput("Teléfono:", telefonoField));
        contenido.add(panelDatos);

        panelTarjeta = new JPanel();
        panelTarjeta.setLayout(new BoxLayout(panelTarjeta, BoxLayout.Y_AXIS));
        panelTarjeta.setOpaque(false);
        panelTarjeta.setVisible(false);

        numeroTarjetaField = new JTextField();
        cvvField = new JTextField();
        fechaExpField = new JTextField();

        panelTarjeta.add(crearInput("Num Tarjeta:", numeroTarjetaField));
        panelTarjeta.add(crearInput("CVV:", cvvField));
        panelTarjeta.add(crearInput("Fecha Exp (MM/AA):", fechaExpField));
        contenido.add(panelTarjeta);

        contenido.add(Box.createVerticalGlue());

        confirmarBtn = new JButton("PAGAR Y FINALIZAR");
        confirmarBtn.setBackground(EstilosApp.COLOR_DORADO);
        confirmarBtn.setFont(EstilosApp.FUENTE_BOLD);
        confirmarBtn.setAlignmentX(CENTER_ALIGNMENT);
        confirmarBtn.setMaximumSize(new Dimension(200, 40));
        confirmarBtn.addActionListener(e -> procesarPago());

        contenido.add(confirmarBtn);

        add(new JScrollPane(contenido));
    }

    private JPanel crearInput(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(500, 50));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return p;
    }

    private void toggleTarjeta() {
        panelTarjeta.setVisible(metodoPagoCombo.getSelectedItem().equals("Tarjeta de Crédito"));
        revalidate();
    }

    private boolean validarDatos() {
        if (nombreField.getText().trim().isEmpty() || direccionField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Faltan datos de envío");
            return false;
        }

        if (metodoPagoCombo.getSelectedItem().equals("Tarjeta de Crédito")) {
            if (numeroTarjetaField.getText().trim().isEmpty()
                    || cvvField.getText().trim().isEmpty()
                    || fechaExpField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Faltan datos de tarjeta");
                return false;
            }
        }

        return true;
    }

    private void procesarPago() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío");
            return;
        }

        if (!validarDatos()) return;

        double total = subtotal + iva - descuento;

        // 1) Validación + descuento de stock profesional
        if (!descontarStockSeguro()) return;

        // 2) Documento pedido
        Document pedido = new Document();

        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        pedido.append("fechaHora", fechaHora);

        // usuario del sistema
        pedido.append("usuario", usuarioActual);

        // Datos cliente
        pedido.append("cliente", nombreField.getText().trim());
        pedido.append("direccion", direccionField.getText().trim());
        pedido.append("telefono", telefonoField.getText().trim());

        String metodoPago = metodoPagoCombo.getSelectedItem().toString();
        pedido.append("metodoPago", metodoPago);

        if (metodoPago.equals("Tarjeta de Crédito")) {
            String num = numeroTarjetaField.getText().trim();
            String ultimos4 = (num.length() >= 4) ? num.substring(num.length() - 4) : num;
            pedido.append("tarjetaUltimos4", ultimos4);
        }

        pedido.append("subtotal", subtotal);
        pedido.append("iva", iva);
        pedido.append("descuento", descuento);
        pedido.append("total", total);

        pedido.append("cupon", cuponAplicado);
        pedido.append("estado", "Pagado");

        // Guardamos copia limpia de productos
        ArrayList<Document> productosPedido = new ArrayList<>();
        for (Document item : carrito) {
            Document nuevo = new Document();
            nuevo.append("id", item.getString("id"));
            nuevo.append("nombre", item.getString("nombre"));
            nuevo.append("precio", item.getDouble("precio"));
            nuevo.append("cantidad", item.getInteger("cantidad", 0));
            productosPedido.add(nuevo);
        }

        pedido.append("productos", productosPedido);

        pedidosCol.insertOne(pedido);

        // ==========================================
        // NUEVO: PDF desde clase reutilizable
        // ==========================================
        FacturaPDF.generar(pedido);

        JOptionPane.showMessageDialog(this, "¡Compra Exitosa! Factura generada.");
        carrito.clear();
        dispose();
    }

    // =============================
    // STOCK PROFESIONAL: 2 PASOS
    // =============================
    private boolean descontarStockSeguro() {
        if (productosCol == null) {
            JOptionPane.showMessageDialog(this, "No hay conexión a productos.");
            return false;
        }

        // 1) Validar stock completo
        for (Document item : carrito) {
            String id = item.getString("id");
            int cantidad = item.getInteger("cantidad", 0);

            try {
                Document prod = productosCol.find(Filters.eq("_id", new ObjectId(id))).first();

                if (prod == null) {
                    JOptionPane.showMessageDialog(this,
                            "Un producto ya no existe en inventario.\nCompra cancelada.");
                    return false;
                }

                int stockActual = prod.getInteger("stock", 0);

                if (cantidad > stockActual) {
                    JOptionPane.showMessageDialog(this,
                            "Stock insuficiente para: " + item.getString("nombre") +
                                    "\nSolicitado: " + cantidad +
                                    "\nDisponible: " + stockActual +
                                    "\n\nCompra cancelada.");
                    return false;
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error validando stock.\nCompra cancelada.");
                return false;
            }
        }

        // 2) Descontar todo
        for (Document item : carrito) {
            String id = item.getString("id");
            int cantidad = item.getInteger("cantidad", 0);

            try {
                productosCol.updateOne(
                        Filters.eq("_id", new ObjectId(id)),
                        Updates.inc("stock", -cantidad)
                );
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error descontando stock.\nCompra cancelada.");
                return false;
            }
        }

        return true;
    }
}