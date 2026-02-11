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

public class PedidosFrame extends JFrame {

    private MongoCollection<Document> colPedidos;

    // Componentes UI
    private JTable tabla;
    private DefaultTableModel modelo;
    private JComboBox<String> cbEstado;
    private JTextField txtBuscarCliente;

    public PedidosFrame() {
        initDb();
        initUI();
        cargarPedidos();
    }

    private void initDb() {
    try {
        MongoDatabase db = MongoConnection.getDatabase();
        colPedidos = db.getCollection("pedidos");
        coleccionProductos = db.getCollection("productos"); // conexión productos
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this,
                "No se pudo conectar a MongoDB: " + e.getMessage(),
                "Error BD", JOptionPane.ERROR_MESSAGE);
    }
}

    private void initUI() {
        setTitle("Historial de Pedidos - Licorería");
        setSize(1050, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ==============================
        // 1. PANEL NORTE (HEADER + FILTROS)
        // ==============================
        JPanel panelNorte = new JPanel(new BorderLayout());

        // Header (botón volver solo cierra)
        panelNorte.add(EstilosApp.crearHeader("Historial de Pedidos de Clientes", null, e -> dispose()),
                BorderLayout.NORTH);

        // Filtros
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelFiltros.setBackground(new Color(245, 245, 245));
        panelFiltros.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        panelFiltros.add(new JLabel("Estado:"));
        cbEstado = new JComboBox<>(new String[]{"Todos", "Pagado", "preparando", "enviado", "entregado", "cancelado"});
        cbEstado.setBackground(Color.WHITE);
        panelFiltros.add(cbEstado);

        panelFiltros.add(new JLabel("Buscar Cliente:"));
        txtBuscarCliente = new JTextField(15);
        panelFiltros.add(txtBuscarCliente);

        JButton btnFiltrar = crearBoton("🔍 Filtrar", new Color(212, 175, 55));
        btnFiltrar.addActionListener(e -> cargarPedidos());
        panelFiltros.add(btnFiltrar);

        panelNorte.add(panelFiltros, BorderLayout.SOUTH);

        add(panelNorte, BorderLayout.NORTH);

        // ==============================
        // 2. TABLA (CENTRO)
        // ==============================
        String[] columnas = {"ID", "Fecha", "Cliente", "Teléfono", "Total ($)", "Método", "Estado"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setSelectionBackground(new Color(255, 248, 220));
        tabla.setSelectionForeground(Color.BLACK);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scroll.getViewport().setBackground(Color.WHITE);

        add(scroll, BorderLayout.CENTER);

        // ==============================
        // 3. PANEL ACCIONES (SUR)
        // ==============================
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        panelAcciones.setBackground(new Color(245, 245, 245));
        panelAcciones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JButton btnDetalle = crearBoton("📄 Ver Detalle", new Color(60, 60, 60));
        btnDetalle.setForeground(Color.WHITE);
        btnDetalle.addActionListener(e -> verDetalle());

        // NUEVO BOTÓN PDF
        JButton btnPDF = crearBoton("🧾 Generar PDF", new Color(40, 120, 40));
        btnPDF.setForeground(Color.WHITE);
        btnPDF.addActionListener(e -> generarPDFPedido());

        // Botones estado
        JButton btnPreparando = crearBoton("👨‍🍳 Preparando", Color.WHITE);
        JButton btnEnviado = crearBoton("🚚 Enviado", Color.WHITE);
        JButton btnEntregado = crearBoton("✅ Entregado", Color.WHITE);
        JButton btnCancelar = crearBoton("❌ Cancelar", new Color(255, 200, 200));

        btnPreparando.addActionListener(e -> cambiarEstado("preparando"));
        btnEnviado.addActionListener(e -> cambiarEstado("enviado"));
        btnEntregado.addActionListener(e -> cambiarEstado("entregado"));
        btnCancelar.addActionListener(e -> cambiarEstado("cancelado"));

        panelAcciones.add(btnDetalle);
        panelAcciones.add(btnPDF);

        panelAcciones.add(Box.createHorizontalStrut(20));
        panelAcciones.add(new JLabel("Cambiar Estado:"));
        panelAcciones.add(btnPreparando);
        panelAcciones.add(btnEnviado);
        panelAcciones.add(btnEntregado);
        panelAcciones.add(btnCancelar);

        add(panelAcciones, BorderLayout.SOUTH);
    }

    // ==== UI HELPERS ====
    private JButton crearBoton(String texto, Color bg) {
        JButton btn = new JButton(texto);
        btn.setBackground(bg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        if (bg.equals(Color.WHITE)) {
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
        return btn;
    }

    // ==== LÓGICA DE DATOS ====

    private void cargarPedidos() {
        modelo.setRowCount(0);
        if (colPedidos == null) return;

        String estadoSel = (String) cbEstado.getSelectedItem();
        String clienteFiltro = txtBuscarCliente.getText().trim();

        List<org.bson.conversions.Bson> filtros = new java.util.ArrayList<>();

        if (estadoSel != null && !"Todos".equals(estadoSel)) {
            filtros.add(Filters.eq("estado", estadoSel));
        }
        if (!clienteFiltro.isEmpty()) {
            filtros.add(Filters.regex("cliente", ".*" + clienteFiltro + ".*", "i"));
        }

        org.bson.conversions.Bson query = filtros.isEmpty() ? new Document() : Filters.and(filtros);

        try (MongoCursor<Document> cur = colPedidos.find(query).sort(Sorts.descending("fechaHora")).iterator()) {
            while (cur.hasNext()) {
                Document d = cur.next();

                modelo.addRow(new Object[]{
                        d.getObjectId("_id").toHexString(),
                        d.getString("fechaHora"),
                        d.getString("cliente"),
                        d.getString("telefono"),
                        d.getDouble("total"),
                        d.getString("metodoPago"),
                        d.getString("estado")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando pedidos: " + e.getMessage());
        }
    }

    private void verDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido de la lista.");
            return;
        }

        String id = (String) modelo.getValueAt(fila, 0);
        Document d = colPedidos.find(Filters.eq("_id", new ObjectId(id))).first();

        if (d == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("          DETALLE DEL PEDIDO            \n");
        sb.append("========================================\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("Fecha: ").append(d.getString("fechaHora")).append("\n");
        sb.append("Estado: ").append(d.getString("estado")).append("\n\n");

        sb.append("--- DATOS DEL CLIENTE ---\n");
        sb.append("Cliente: ").append(d.getString("cliente")).append("\n");
        sb.append("Teléfono: ").append(d.getString("telefono")).append("\n");
        sb.append("Dirección: ").append(d.getString("direccion")).append("\n\n");

        sb.append("--- PRODUCTOS ---\n");
        List<Document> productos = (List<Document>) d.get("productos");
        if (productos != null) {
            for (Document p : productos) {
                String nombre = p.getString("nombre");
                int cant = p.getInteger("cantidad", 1);
                Double precio = p.getDouble("precio");
                double sub = (precio != null ? precio : 0.0) * cant;

                sb.append(String.format("- %-20s x%d  ($%.2f)\n", nombre, cant, sub));
            }
        }

        sb.append("\n--- TOTALES ---\n");
        sb.append(String.format("Subtotal:   $%.2f\n", d.getDouble("subtotal")));
        sb.append(String.format("Descuento: -$%.2f\n", d.getDouble("descuento")));
        sb.append(String.format("IVA:        $%.2f\n", d.getDouble("iva")));
        sb.append("----------------------------\n");
        sb.append(String.format("TOTAL:      $%.2f\n", d.getDouble("total")));
        sb.append("Método Pago: ").append(d.getString("metodoPago")).append("\n");

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);

        JScrollPane pScroll = new JScrollPane(area);
        pScroll.setPreferredSize(new Dimension(450, 520));

        JOptionPane.showMessageDialog(this, pScroll, "Detalle de Pedido", JOptionPane.PLAIN_MESSAGE);
    }

    // ==========================================
    // NUEVO: GENERAR PDF DESDE PEDIDO SELECCIONADO
    // ==========================================
    private void generarPDFPedido() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido primero.");
            return;
        }

        String id = (String) modelo.getValueAt(fila, 0);

        try {
            Document pedido = colPedidos.find(Filters.eq("_id", new ObjectId(id))).first();

            if (pedido == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el pedido en la BD.");
                return;
            }

            FacturaPDF.generar(pedido);
            JOptionPane.showMessageDialog(this, "PDF generado correctamente.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error generando PDF: " + e.getMessage());
        }
    }
    private MongoCollection<Document> coleccionProductos;

    private void cambiarEstado(String nuevoEstado) {
    int fila = tabla.getSelectedRow();
    if (fila < 0) {
        JOptionPane.showMessageDialog(this, "Selecciona un pedido primero.");
        return;
    }

    String id = (String) modelo.getValueAt(fila, 0);
    String estadoActual = (String) modelo.getValueAt(fila, 6);

    if (estadoActual.equalsIgnoreCase(nuevoEstado)) return;

    // Validación de stock si se intenta preparar o pagar
    if ("preparando".equalsIgnoreCase(nuevoEstado) || "pagado".equalsIgnoreCase(nuevoEstado)) {
        Document pedido = colPedidos.find(Filters.eq("_id", new ObjectId(id))).first();
        if (pedido == null) return;

        List<Document> productos = (List<Document>) pedido.get("productos");
        StringBuilder faltantes = new StringBuilder();

        for (Document p : productos) {
            String nombre = p.getString("nombre");
            int cantPedido = p.getInteger("cantidad", 1);

            Document prodBD = coleccionProductos.find(Filters.eq("nombre", nombre)).first();
            int stockActual = (prodBD != null && prodBD.getInteger("stock") != null) ? prodBD.getInteger("stock") : 0;

            if (stockActual < cantPedido) {
                faltantes.append(String.format("- %s: pedido %d, disponible %d\n", nombre, cantPedido, stockActual));
            }
        }

        if (faltantes.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay suficiente stock para los siguientes productos:\n" + faltantes,
                    "Stock insuficiente", JOptionPane.WARNING_MESSAGE);
            return; // bloqueamos cambio de estado
        }
    }

    // Confirmación
    int confirm = JOptionPane.showConfirmDialog(this,
            "¿Cambiar estado de '" + estadoActual + "' a '" + nuevoEstado + "'?",
            "Confirmar Cambio", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
        try {
            colPedidos.updateOne(Filters.eq("_id", new ObjectId(id)), Updates.set("estado", nuevoEstado));

            // Si se está preparando, descontamos stock
            if ("preparando".equalsIgnoreCase(nuevoEstado)) {
                Document pedido = colPedidos.find(Filters.eq("_id", new ObjectId(id))).first();
                if (pedido != null) {
                    List<Document> productos = (List<Document>) pedido.get("productos");
                    for (Document p : productos) {
                        String nombre = p.getString("nombre");
                        int cantPedido = p.getInteger("cantidad", 1);
                        coleccionProductos.updateOne(Filters.eq("nombre", nombre),
                                Updates.inc("stock", -cantPedido));
                    }
                }
            }

            cargarPedidos();
            JOptionPane.showMessageDialog(this, "Estado actualizado correctamente.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PedidosFrame().setVisible(true));
    }
}