package View;

import Controler.MongoConnection;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class GestorStockFrame extends JFrame {

    // ==== DATOS DE SESIÓN (Para navegación circular) ====
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;

    // ==== MONGODB ====
    private MongoCollection<Document> colBodega;
    private MongoCollection<Document> colProductos;

    // ==== COMPONENTES UI ====
    private JTable tabla;
    private DefaultTableModel modelo;

    // Constructor Completo (Usado desde el Menú Principal)
    public GestorStockFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos;

        initDb();
        initUI();
        cargarPendientes();
    }
    
    // Constructor de Compatibilidad (Por si se llama directo)
    public GestorStockFrame(String usuario) {
        this(usuario, "gestor", null);
    }

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            colBodega = db.getCollection("bodega_registros");
            colProductos = db.getCollection("productos");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a MongoDB: " + e.getMessage(),
                    "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setTitle("Gestor de Productos y Pedidos");
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ==== 1. ENCABEZADO UNIFICADO ====
        add(EstilosApp.crearHeader("Gestión de Stock y Pedidos", usuarioActual, e -> volverAlMenu()), BorderLayout.NORTH);

        // ==== 2. TABLA DE ENTRADAS (CENTRO) ====
        String[] columnas = { "ID", "Fecha", "Producto", "Cantidad", "Presentación", "Estado", "Usuario Bodega" };
        modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        
        tabla = new JTable(modelo);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        // Panel para la tabla con título
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBackground(new Color(245, 245, 245));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel lblSub = new JLabel("Entradas de bodega pendientes de procesar");
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSub.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        panelCentral.add(lblSub, BorderLayout.NORTH);
        panelCentral.add(new JScrollPane(tabla), BorderLayout.CENTER);

        // ==== 3. PANEL ACCIONES (DERECHA) ====
        JPanel panelAcciones = new JPanel();
        panelAcciones.setBackground(new Color(245, 245, 245)); // o EstilosApp.COLOR_FONDO
        panelAcciones.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelAcciones.setLayout(new BoxLayout(panelAcciones, BoxLayout.Y_AXIS));
        panelAcciones.setPreferredSize(new Dimension(300, 0));

        JLabel lblMod = new JLabel("Acciones de Stock");
        lblMod.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMod.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panelAcciones.add(lblMod);
        panelAcciones.add(Box.createVerticalStrut(15));

        JButton btnRefrescar = crearBoton("🔄 Actualizar Lista");
        btnRefrescar.addActionListener(e -> cargarPendientes());
        panelAcciones.add(btnRefrescar);
        panelAcciones.add(Box.createVerticalStrut(10));

        JButton btnProcesar = crearBoton("⚡ Procesar Entrada");
        btnProcesar.addActionListener(e -> procesarEntradaSeleccionada());
        btnProcesar.setToolTipText("Suma el stock al inventario y marca como procesado");
        panelAcciones.add(btnProcesar);
        
        panelAcciones.add(Box.createVerticalStrut(30));
        
        JLabel lblNav = new JLabel("Navegación Rápida");
        lblNav.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNav.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelAcciones.add(lblNav);
        panelAcciones.add(Box.createVerticalStrut(10));

        JButton btnInventario = crearBoton("📦 Ver Inventario Global");
        btnInventario.addActionListener(e -> {
            dispose();
            // Abre AdminFrame en modo "Solo lectura" (true) pasando la sesión
            new AdminFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
        });
        panelAcciones.add(btnInventario);
        panelAcciones.add(Box.createVerticalStrut(10));

        JButton btnVerPedidos = crearBoton("📃 Ver Pedidos Clientes");
        btnVerPedidos.addActionListener(e -> new PedidosFrame().setVisible(true));
        panelAcciones.add(btnVerPedidos);

        // ==== SPLIT ====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelCentral, panelAcciones);
        split.setDividerLocation(750);
        split.setResizeWeight(0.8); // La tabla toma más espacio al redimensionar
        add(split, BorderLayout.CENTER);
    }
    
    // ==== LÓGICA DE NAVEGACIÓN ====
    private void volverAlMenu() {
        dispose();
        new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
    }
    
    // Método auxiliar para estilo de botones
    private JButton crearBoton(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(new Color(212, 175, 55)); // Dorado
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(280, 40));
        return btn;
    }

    private void cargarPendientes() {
        modelo.setRowCount(0);
        if (colBodega == null) return;

        try (MongoCursor<Document> cur = colBodega
                .find(Filters.eq("estado", "pendiente"))
                .sort(Sorts.descending("fecha"))
                .iterator()) {

            while (cur.hasNext()) {
                Document d = cur.next();
                modelo.addRow(new Object[]{
                        d.getObjectId("_id").toHexString(),
                        d.getString("fecha"),
                        d.getString("producto"),
                        d.getInteger("cantidad", 0),
                        d.getString("presentacion"),
                        d.getString("estado"),
                        d.getString("usuario")
                });
            }
        }
    }

    private void procesarEntradaSeleccionada() {
        if (colBodega == null || colProductos == null) {
            JOptionPane.showMessageDialog(this, "Colecciones no disponibles");
            return;
        }
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una entrada en la tabla.");
            return;
        }

        String id = (String) modelo.getValueAt(fila, 0);
        Document reg = colBodega.find(Filters.eq("_id", new ObjectId(id))).first();
        
        if (reg == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el registro en BD.");
            return;
        }

        if ("procesado".equalsIgnoreCase(reg.getString("estado"))) {
            JOptionPane.showMessageDialog(this, "Esta entrada ya fue procesada.");
            return;
        }

        String nombreProd = reg.getString("producto");
        int cantidad = reg.getInteger("cantidad", 0);
        
        if (cantidad <= 0) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida en el registro.");
            return;
        }

        // Buscar si ya existe un producto con ese nombre
        Document prod = colProductos.find(Filters.eq("nombre", nombreProd)).first();

        try {
            if (prod == null) {
                // PRODUCTO NUEVO
                int r = JOptionPane.showConfirmDialog(this,
                        "No existe el producto '" + nombreProd + "'.\n" +
                        "¿Deseas crearlo automáticamente con este stock?",
                        "Crear nuevo producto", JOptionPane.YES_NO_OPTION);
                
                if (r != JOptionPane.YES_OPTION) return;

                String precioStr = JOptionPane.showInputDialog(this,
                        "Ingresa el precio de venta (PVP) para " + nombreProd + ":", "0.00");
                
                double precio = 0.0;
                try {
                    if (precioStr != null) precio = Double.parseDouble(precioStr.trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Precio inválido, se asignará $0.00");
                }

                Document nuevoProd = new Document("nombre", nombreProd)
                        .append("precio", precio)
                        .append("stock", cantidad)
                        .append("tipo", "Otros") // Tipo por defecto
                        .append("imagen", null)
                        .append("activo", true);

                colProductos.insertOne(nuevoProd);
                JOptionPane.showMessageDialog(this, "Producto creado y stock asignado ✅");

            } else {
                // PRODUCTO EXISTENTE - Sumar stock
                colProductos.updateOne(
                        Filters.eq("_id", prod.getObjectId("_id")),
                        Updates.inc("stock", cantidad)
                );
                JOptionPane.showMessageDialog(this, "Stock actualizado: +" + cantidad + " unidades para " + nombreProd + " ✅");
            }

            // Marcar registro de bodega como PROCESADO
            colBodega.updateOne(
                    Filters.eq("_id", reg.getObjectId("_id")),
                    Updates.set("estado", "procesado")
            );

            cargarPendientes(); // Recargar tabla

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al procesar: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestorStockFrame("gestorDemo").setVisible(true));
    }
}