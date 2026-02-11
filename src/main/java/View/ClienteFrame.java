package View;

import Controler.MongoConnection;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteFrame extends JFrame {

    // Datos de sesión
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;

    private JPanel panelProductos;
    private final ArrayList<Document> carrito = new ArrayList<>();
    private MongoCollection<Document> coleccion;

    // Filtros UI
    private JComboBox<String> cbTipoFiltro;
    private JTextField txtPrecioMin, txtPrecioMax;

    // Botón carrito para actualizarlo en tiempo real
    private JButton btnCarrito;

    public ClienteFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos != null ? permisos : new ArrayList<>();

        initDb();
        initUI();
    }

    public ClienteFrame(String usuario) {
        this(usuario, "cliente", null);
    }

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            coleccion = db.getCollection("productos");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Mongo: " + e.getMessage());
        }
    }

    private void initUI() {
        setTitle("Catálogo de Licores - Licorería");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header unificado
        JPanel header = EstilosApp.crearHeader("Catálogo de Productos", usuarioActual, e -> volverAlMenu());

        // ==== PANEL DE FILTROS Y CARRITO ====
        JPanel panelControl = new JPanel(new BorderLayout());
        panelControl.setBackground(EstilosApp.COLOR_FONDO);
        panelControl.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Filtros (Izquierda)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.setOpaque(false);

        filtros.add(new JLabel("Tipo:"));
        cbTipoFiltro = new JComboBox<>(new String[]{
                "Todos", "Vino tinto", "Vino blanco", "Cerveza", "Whisky", "Ron", "Vodka", "Tequila", "Otros"
        });
        filtros.add(cbTipoFiltro);

        filtros.add(new JLabel("  Precio:"));
        txtPrecioMin = new JTextField(5);
        txtPrecioMax = new JTextField(5);
        filtros.add(txtPrecioMin);
        filtros.add(new JLabel("-"));
        filtros.add(txtPrecioMax);

        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.setBackground(Color.WHITE);
        btnFiltrar.addActionListener(e -> aplicarFiltros());
        filtros.add(btnFiltrar);

        // Botón Refrescar
        JButton btnRefrescar = new JButton("🔄");
        btnRefrescar.setToolTipText("Actualizar Stock");
        btnRefrescar.addActionListener(e -> cargarProductosDesdeMongo(null, null, null));
        filtros.add(btnRefrescar);

        // Botón Ver Carrito (Derecha)
        btnCarrito = new JButton();
        btnCarrito.setBackground(EstilosApp.COLOR_DORADO);
        btnCarrito.setFont(EstilosApp.FUENTE_BOLD);
        btnCarrito.setFocusPainted(false);
        actualizarTextoBotonCarrito();
        btnCarrito.addActionListener(e -> abrirCarrito());

        panelControl.add(filtros, BorderLayout.CENTER);
        panelControl.add(btnCarrito, BorderLayout.EAST);

        // Norte combinado (header + controles)
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(header, BorderLayout.NORTH);
        norte.add(panelControl, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        // ==== GRID DE PRODUCTOS ====
        panelProductos = new JPanel(new GridLayout(0, 3, 20, 20));
        panelProductos.setBackground(Color.WHITE);
        panelProductos.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scroll = new JScrollPane(panelProductos);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        cargarProductosDesdeMongo(null, null, null);
    }

    private void volverAlMenu() {
        dispose();
        new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
    }

    private void abrirCarrito() {
        CarritoFrame frameCarrito = new CarritoFrame(carrito, usuarioActual);

        frameCarrito.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                actualizarTextoBotonCarrito();
                cargarProductosDesdeMongo(null, null, null);
            }
        });

        frameCarrito.setVisible(true);
    }

    // ✔ Profesional: muestra total real de productos (sumatoria cantidades)
    private void actualizarTextoBotonCarrito() {
        int totalItems = 0;
        for (Document d : carrito) {
            totalItems += d.getInteger("cantidad", 0);
        }
        btnCarrito.setText("🛒 Ver Carrito (" + totalItems + ")");
    }

    private void cargarProductosDesdeMongo(String tipo, Double min, Double max) {
        panelProductos.removeAll();
        if (coleccion == null) return;

        List<Bson> filtros = new ArrayList<>();
        filtros.add(Filters.eq("activo", true));

        if (tipo != null && !tipo.equals("Todos")) filtros.add(Filters.eq("tipo", tipo));
        if (min != null) filtros.add(Filters.gte("precio", min));
        if (max != null) filtros.add(Filters.lte("precio", max));

        Bson query = filtros.isEmpty() ? new Document() : Filters.and(filtros);

        for (Document doc : coleccion.find(query)) {
            panelProductos.add(crearTarjetaProducto(doc));
        }

        panelProductos.revalidate();
        panelProductos.repaint();
    }

    private void aplicarFiltros() {
        String tipo = (String) cbTipoFiltro.getSelectedItem();
        Double min = null, max = null;

        try {
            if (!txtPrecioMin.getText().isEmpty()) min = Double.parseDouble(txtPrecioMin.getText());
            if (!txtPrecioMax.getText().isEmpty()) max = Double.parseDouble(txtPrecioMax.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Precios inválidos");
            return;
        }

        cargarProductosDesdeMongo(tipo, min, max);
    }

    private JPanel crearTarjetaProducto(Document prod) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(15), BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setPreferredSize(new Dimension(200, 280));

        // Datos
        String nombre = prod.getString("nombre");
        double precio = prod.getDouble("precio");
        int stock = prod.getInteger("stock");
        String imgPath = prod.getString("imagen");

        // Imagen
        JLabel lblImg = new JLabel();
        lblImg.setHorizontalAlignment(SwingConstants.CENTER);
        if (imgPath != null && !imgPath.isEmpty()) {
            ImageIcon icon = new ImageIcon(new ImageIcon(imgPath).getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH));
            lblImg.setIcon(icon);
        } else {
            lblImg.setText("🍾");
            lblImg.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        }
        card.add(lblImg, BorderLayout.CENTER);

        // Info inferior
        JPanel info = new JPanel(new GridLayout(0, 1));
        info.setOpaque(false);

        JLabel lblNom = new JLabel(nombre, JLabel.CENTER);
        lblNom.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel lblPrecio = new JLabel(String.format("$%.2f", precio), JLabel.CENTER);
        lblPrecio.setForeground(EstilosApp.COLOR_VINO);

        JLabel lblStock = new JLabel("Stock: " + stock, JLabel.CENTER);
        lblStock.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        if (stock < 5) lblStock.setForeground(Color.RED);

        JButton btnAdd = new JButton("Agregar");
        btnAdd.setBackground(EstilosApp.COLOR_DORADO);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> agregarAlCarrito(prod));

        info.add(lblNom);
        info.add(lblPrecio);
        info.add(lblStock);
        info.add(Box.createVerticalStrut(5));
        info.add(btnAdd);

        card.add(info, BorderLayout.SOUTH);
        return card;
    }

    private void agregarAlCarrito(Document prod) {
        if (coleccion == null) return;

        // Re-consulta a BD para asegurar precio y stock actuales
        Document prodActual = coleccion.find(Filters.eq("_id", prod.getObjectId("_id"))).first();

        if (prodActual == null) {
            JOptionPane.showMessageDialog(this, "Este producto ya no existe.");
            return;
        }

        String id = prodActual.getObjectId("_id").toHexString();
        String nombre = prodActual.getString("nombre");

        // ✔ Precio actual desde BD
        double precioActual = prodActual.getDouble("precio");

        int stockReal = prodActual.getInteger("stock", 0);

        if (stockReal <= 0) {
            JOptionPane.showMessageDialog(this, "Producto sin stock disponible.");
            return;
        }

        // Buscar si ya está en carrito
        Document itemEnCarrito = null;
        for (Document d : carrito) {
            if (id.equals(d.getString("id"))) {
                itemEnCarrito = d;
                break;
            }
        }

        int cantidadActual = (itemEnCarrito == null) ? 0 : itemEnCarrito.getInteger("cantidad", 0);

        if (cantidadActual + 1 > stockReal) {
            JOptionPane.showMessageDialog(this,
                    "¡No hay suficiente stock disponible!",
                    "Stock insuficiente",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (itemEnCarrito == null) {
            // Carrito estándar
            Document item = new Document("id", id)
                    .append("nombre", nombre)
                    .append("precio", precioActual)
                    .append("cantidad", 1);
            carrito.add(item);
        } else {
            itemEnCarrito.put("cantidad", cantidadActual + 1);
            // Actualiza precio en caso de cambio (opcional, pero profesional)
            itemEnCarrito.put("precio", precioActual);
        }

        actualizarTextoBotonCarrito();
        JOptionPane.showMessageDialog(this, "Agregado: " + nombre);
    }

    // Bordes redondeados
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        RoundedBorder(int radius) { this.radius = radius; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
        }
    }
}