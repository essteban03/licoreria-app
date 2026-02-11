package View;

import Controler.MongoConnection;
import Controler.ProveedorController;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AdminFrame extends JFrame {

    // ==== DATOS DE SESIÓN ====
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;

    // ==== MONGODB ====
    private MongoCollection<Document> coleccion;

    // ==== CONTROLADORES ====
    private ProveedorController proveedorController;

    // ==== COMPONENTES UI ====
    private JTable tablaProductos;
    private DefaultTableModel modelo;
    private JTextField txtNombre, txtPrecio, txtStock, txtBuscar;
    private JLabel lblImagen;
    private String rutaImagen = "";
    private JComboBox<String> cbTipo;

    // NUEVO: PROVEEDORES
    private JComboBox<String> cbProveedor;
    private List<Document> listaProveedores = new ArrayList<>();

    // Botones
    private JButton btnUsuarios;
    private JButton btnProveedores;
    private JButton btnAgregar, btnEditar, btnEliminar, btnLimpiar, btnBuscar;

    // Estado
    private boolean modoGestor = false;

    // ==== CONSTRUCTOR PRINCIPAL ====
    public AdminFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos;

        // Determinar modo: Si NO tiene permiso de usuarios, es un gestor restringido
        this.modoGestor = (permisos == null || !permisos.contains("GESTION_USUARIOS"));

        initDb();
        initControllers();
        initUI();
        cargarProveedores();
        cargarProductos();
    }

    public AdminFrame() {
        this("admin", "admin", null);
    }

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            coleccion = db.getCollection("productos");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error conectando a MongoDB: " + e.getMessage());
        }
    }

    private void initControllers() {
        proveedorController = new ProveedorController();
    }

    private void initUI() {
        setTitle("Panel de Administración - Licorería");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== HEADER =====
        add(EstilosApp.crearHeader("Gestión de Inventario", usuarioActual, e -> volverAlMenu()), BorderLayout.NORTH);

        // ===== TABLA =====
        String[] columnas = {"ID", "Nombre", "Tipo", "Proveedor", "Precio", "Stock"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaProductos = new JTable(modelo);
        tablaProductos.setRowHeight(30);
        tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tablaProductos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        tablaProductos.getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) cargarProductoEnFormulario();
        });

        JScrollPane scroll = new JScrollPane(tablaProductos);
        add(scroll, BorderLayout.CENTER);

        // ===== PANEL FORMULARIO =====
        JPanel panelForm = new JPanel();
        panelForm.setPreferredSize(new Dimension(420, 0));
        panelForm.setBackground(new Color(250, 243, 224));
        panelForm.setLayout(new BoxLayout(panelForm, BoxLayout.Y_AXIS));
        panelForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblSub = new JLabel("Detalle del Producto");
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelForm.add(lblSub);
        panelForm.add(Box.createVerticalStrut(15));

        panelForm.add(crearCampo("Nombre:", txtNombre = new JTextField()));

        cbTipo = new JComboBox<>(new String[]{
                "Seleccionar...", "Vino tinto", "Vino blanco", "Cerveza", "Whisky", "Ron",
                "Vodka", "Tequila", "Espumante", "Otros"
        });
        panelForm.add(crearCampoCombo("Tipo:", cbTipo));

        // ===== COMBO PROVEEDOR =====
        cbProveedor = new JComboBox<>(new String[]{"Cargando proveedores..."});
        panelForm.add(crearCampoCombo("Proveedor:", cbProveedor));

        panelForm.add(crearCampo("Precio (USD):", txtPrecio = new JTextField()));
        panelForm.add(crearCampo("Stock:", txtStock = new JTextField()));

        JButton btnImg = new JButton("Seleccionar Imagen");
        btnImg.setBackground(new Color(212, 175, 55));
        btnImg.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnImg.addActionListener(e -> seleccionarImagen());

        lblImagen = new JLabel("Sin imagen", JLabel.CENTER);
        lblImagen.setPreferredSize(new Dimension(150, 120));
        lblImagen.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel pImg = new JPanel();
        pImg.setOpaque(false);
        pImg.add(lblImagen);

        panelForm.add(btnImg);
        panelForm.add(pImg);
        panelForm.add(Box.createVerticalStrut(10));

        // Botones CRUD
        JPanel pBotones = new JPanel(new GridLayout(2, 2, 10, 10));
        pBotones.setOpaque(false);

        btnAgregar = crearBoton("Agregar");
        btnEditar = crearBoton("Actualizar");
        btnEliminar = crearBoton("Dar Baja");
        btnLimpiar = crearBoton("Limpiar");

        btnAgregar.addActionListener(e -> agregarProducto());
        btnEditar.addActionListener(e -> editarProducto());
        btnEliminar.addActionListener(e -> eliminarProducto());
        btnLimpiar.addActionListener(e -> limpiarCampos());

        pBotones.add(btnAgregar); pBotones.add(btnEditar);
        pBotones.add(btnEliminar); pBotones.add(btnLimpiar);
        panelForm.add(pBotones);

        // Buscador
        panelForm.add(Box.createVerticalStrut(15));
        panelForm.add(new JLabel("Buscar producto:"));
        txtBuscar = new JTextField();
        btnBuscar = crearBoton("Buscar");
        btnBuscar.addActionListener(e -> buscarProducto());

        panelForm.add(txtBuscar);
        panelForm.add(Box.createVerticalStrut(5));
        panelForm.add(btnBuscar);

        // Botones extra (solo si no es gestor)
        if (!modoGestor) {
            panelForm.add(Box.createVerticalStrut(20));
            JSeparator sep = new JSeparator();
            panelForm.add(sep);
            panelForm.add(Box.createVerticalStrut(10));

            btnUsuarios = new JButton("Gestionar Usuarios");
            btnUsuarios.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnUsuarios.addActionListener(e -> {
                dispose();
                new ListaUsuariosFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
            });

            btnProveedores = new JButton("Gestionar Proveedores");
            btnProveedores.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnProveedores.addActionListener(e -> {
                dispose();
                new ProveedorFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
            });

            panelForm.add(btnUsuarios);
            panelForm.add(Box.createVerticalStrut(5));
            panelForm.add(btnProveedores);
        } else {
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
            txtStock.setEditable(false);
            txtStock.setBackground(new Color(230, 230, 230));
        }

        add(panelForm, BorderLayout.EAST);
    }

    // ==== NAVEGACIÓN ====
    private void volverAlMenu() {
        dispose();
        if (permisosActuales != null && !permisosActuales.isEmpty()) {
            new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
        } else {
            new Login().setVisible(true);
        }
    }

    // ==== PROVEEDORES ====
    private void cargarProveedores() {
        if (proveedorController == null) return;

        // Traemos todos los proveedores
        listaProveedores = proveedorController.getTodosProveedores();
        cbProveedor.removeAllItems();

        cbProveedor.addItem("Seleccionar...");

        for (Document p : listaProveedores) {
            String nombre = p.getString("nombre");
            String estado = p.getString("estado"); // ACTIVO o INACTIVO
            if ("INACTIVO".equalsIgnoreCase(estado)) {
                nombre += " (Proveedor inactivo)";
            }
            cbProveedor.addItem(nombre);
        }
    }

    private Document getProveedorSeleccionado() {
        int index = cbProveedor.getSelectedIndex();
        if (index <= 0) return null;
        return listaProveedores.get(index - 1);
    }

    // ==== CRUD PRODUCTOS ====
    private void cargarProductos() {
        modelo.setRowCount(0);
        if (coleccion == null) return;

        for (Document doc : coleccion.find()) {
            if (doc.getBoolean("activo", true)) {
                modelo.addRow(new Object[]{
                        doc.getObjectId("_id"),
                        doc.getString("nombre"),
                        doc.getString("tipo"),
                        doc.getString("proveedorNombre"),
                        doc.getDouble("precio"),
                        doc.getInteger("stock")
                });
            }
        }
    }

    private void cargarProductoEnFormulario() {
        int fila = tablaProductos.getSelectedRow();
        if (fila < 0) return;

        ObjectId id = (ObjectId) modelo.getValueAt(fila, 0);
        Document d = coleccion.find(Filters.eq("_id", id)).first();

        if (d != null) {
            txtNombre.setText(d.getString("nombre"));
            txtPrecio.setText(String.valueOf(d.getDouble("precio")));
            txtStock.setText(String.valueOf(d.getInteger("stock")));
            cbTipo.setSelectedItem(d.getString("tipo"));

            String provNombre = d.getString("proveedorNombre");
            if (provNombre != null && !provNombre.isEmpty()) {
                cbProveedor.setSelectedItem(provNombre);
            } else {
                cbProveedor.setSelectedIndex(0);
            }

            rutaImagen = d.getString("imagen");

            if (rutaImagen != null && !rutaImagen.isEmpty()) {
                ImageIcon icon = new ImageIcon(new ImageIcon(rutaImagen).getImage().getScaledInstance(150, 120, Image.SCALE_SMOOTH));
                lblImagen.setIcon(icon);
                lblImagen.setText("");
            } else {
                lblImagen.setIcon(null);
                lblImagen.setText("Sin imagen");
            }
        }
    }

    private void agregarProducto() {
        try {
            if (cbTipo.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un tipo.");
                return;
            }

            Document proveedor = getProveedorSeleccionado();
            if (proveedor == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un proveedor.");
                return;
            }

            String nombreProveedor = proveedor.getString("nombre");
            String proveedorId = proveedor.getObjectId("_id").toHexString();

            Document doc = new Document("nombre", txtNombre.getText())
                    .append("tipo", cbTipo.getSelectedItem().toString())
                    .append("proveedorId", proveedorId)
                    .append("proveedorNombre", nombreProveedor)
                    .append("precio", Double.parseDouble(txtPrecio.getText()))
                    .append("stock", Integer.parseInt(txtStock.getText()))
                    .append("imagen", rutaImagen)
                    .append("activo", true);

            coleccion.insertOne(doc);
            JOptionPane.showMessageDialog(this, "Producto agregado");

            cargarProductos();
            limpiarCampos();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void editarProducto() {
        int fila = tablaProductos.getSelectedRow();
        if (fila < 0) return;

        ObjectId id = (ObjectId) modelo.getValueAt(fila, 0);

        try {
            Document proveedor = getProveedorSeleccionado();
            if (proveedor == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un proveedor.");
                return;
            }

            String nombreProveedor = proveedor.getString("nombre");
            String proveedorId = proveedor.getObjectId("_id").toHexString();

            coleccion.updateOne(Filters.eq("_id", id), new Document("$set", new Document()
                    .append("nombre", txtNombre.getText())
                    .append("tipo", cbTipo.getSelectedItem().toString())
                    .append("proveedorId", proveedorId)
                    .append("proveedorNombre", nombreProveedor)
                    .append("precio", Double.parseDouble(txtPrecio.getText()))
                    .append("stock", Integer.parseInt(txtStock.getText()))
                    .append("imagen", rutaImagen)
            ));

            JOptionPane.showMessageDialog(this, "Producto actualizado");
            cargarProductos();

        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void eliminarProducto() {
        int fila = tablaProductos.getSelectedRow();
        if (fila < 0) return;

        ObjectId id = (ObjectId) modelo.getValueAt(fila, 0);

        if (JOptionPane.showConfirmDialog(this, "¿Dar de baja?") == JOptionPane.YES_OPTION) {
            coleccion.updateOne(Filters.eq("_id", id),
                    new Document("$set", new Document("activo", false)));

            cargarProductos();
            limpiarCampos();
        }
    }

    private void buscarProducto() {
        String txt = txtBuscar.getText().trim();
        if (txt.isEmpty()) {
            cargarProductos();
            return;
        }

        modelo.setRowCount(0);

        for (Document d : coleccion.find(Filters.regex("nombre", ".*" + txt + ".*", "i"))) {
            if (d.getBoolean("activo", true)) {
                modelo.addRow(new Object[]{
                        d.getObjectId("_id"),
                        d.getString("nombre"),
                        d.getString("tipo"),
                        d.getString("proveedorNombre"),
                        d.getDouble("precio"),
                        d.getInteger("stock")
                });
            }
        }
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtPrecio.setText("");
        txtStock.setText("");
        txtBuscar.setText("");

        cbTipo.setSelectedIndex(0);
        cbProveedor.setSelectedIndex(0);

        lblImagen.setIcon(null);
        lblImagen.setText("Sin imagen");

        rutaImagen = "";
        tablaProductos.clearSelection();
    }

    private void seleccionarImagen() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rutaImagen = fc.getSelectedFile().getAbsolutePath();
            lblImagen.setIcon(new ImageIcon(new ImageIcon(rutaImagen).getImage().getScaledInstance(150, 120, Image.SCALE_SMOOTH)));
            lblImagen.setText("");
        }
    }

    private JPanel crearCampo(String titulo, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(new JLabel(titulo), BorderLayout.NORTH);
        p.add(txt, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        return p;
    }

    private JPanel crearCampoCombo(String titulo, JComboBox<String> combo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(new JLabel(titulo), BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        return p;
    }

    private JButton crearBoton(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(212, 175, 55));
        b.setFocusPainted(false);
        return b;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminFrame().setVisible(true));
    }
}