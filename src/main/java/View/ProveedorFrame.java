/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package View;

import Controler.MongoConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProveedorFrame extends JFrame {

    // ==== DATOS DE SESIÓN (Para navegación circular) ====
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;

    // ==== MONGODB ====
    private MongoCollection<Document> proveedoresCol;
    
    // ==== COMPONENTES UI ====
    private JTable tablaProveedores;
    private DefaultTableModel modeloTabla;

    // Formulario
    private JTextField txtNombre;
    private JTextField txtRuc;
    private JTextField txtContacto;
    private JTextField txtTelefono;
    private JTextField txtEmail;
    private JTextField txtDescuento;
    private JComboBox<String> cbCondiciones;
    private JTextArea txtDireccion;
    private JTextArea txtObservaciones;
    private JLabel lblEstado;

    // Búsqueda
    private JTextField txtBuscar;

    // ID proveedor seleccionado
    private String proveedorSeleccionadoId;

    // ==== CONSTRUCTOR COMPLETO (Usado desde el Menú) ====
    public ProveedorFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos;

        setTitle("Gestión de Proveedores");
        setSize(1150, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Cargar DB y UI
        initDb();
        initUI();
        cargarProveedores();
    }

    // ==== CONSTRUCTOR DE COMPATIBILIDAD (Por si se llama sin argumentos) ====
    public ProveedorFrame() {
        this("admin", "admin", null);
    }

    private void initUI() {
        // ==========================
        // COLORES CONSISTENTES
        // ==========================
        Color crema = new Color(250, 243, 224);
        Color dorado = new Color(212, 175, 55);

        // ==========================
        // 1. BARRA SUPERIOR (UNIFICADA)
        // ==========================
        // Usamos EstilosApp para mantener coherencia con el resto del sistema
        add(EstilosApp.crearHeader("Gestión de Proveedores", usuarioActual, e -> volverAlMenu()), BorderLayout.NORTH);

        // ==========================
        // 2. TABLA CENTRAL
        // ==========================
        String[] columnas = {
                "ID", "Nombre", "RUC", "Contacto",
                "Teléfono", "Email", "Condiciones pago", "Estado"
        };

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // solo lectura en tabla
            }
        };

        tablaProveedores = new JTable(modeloTabla);
        tablaProveedores.setRowHeight(26);
        tablaProveedores.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaProveedores.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JScrollPane scroll = new JScrollPane(tablaProveedores);
        add(scroll, BorderLayout.CENTER);

        // Cuando selecciono una fila → cargar datos en el formulario
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarProveedorSeleccionado();
            }
        });

        // ==========================
        // 3. PANEL DERECHO: FORM + BÚSQUEDA
        // ==========================
        JPanel panelDerecho = new JPanel();
        panelDerecho.setPreferredSize(new Dimension(360, 0));
        panelDerecho.setBackground(crema);
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblSubtitulo = new JLabel("Datos del proveedor");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelDerecho.add(lblSubtitulo);
        panelDerecho.add(Box.createVerticalStrut(10));

        // Campos
        txtNombre = crearCampoTexto(panelDerecho, "Nombre comercial:");
        txtRuc    = crearCampoTexto(panelDerecho, "RUC / Cédula:");
        txtContacto = crearCampoTexto(panelDerecho, "Persona de contacto:");
        txtTelefono = crearCampoTexto(panelDerecho, "Teléfono:");
        txtEmail    = crearCampoTexto(panelDerecho, "Email:");

        // Condiciones de pago
        JLabel lblCond = new JLabel("Condiciones de pago:");
        lblCond.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelDerecho.add(lblCond);
        cbCondiciones = new JComboBox<>(new String[]{
                "Contado", "15 días", "30 días", "45 días", "60 días"
        });
        cbCondiciones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        cbCondiciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panelDerecho.add(cbCondiciones);
        panelDerecho.add(Box.createVerticalStrut(8));

        // Descuento
        txtDescuento = crearCampoTexto(panelDerecho, "Descuento negociado (%):");

        // Dirección
        JLabel lblDir = new JLabel("Dirección:");
        lblDir.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelDerecho.add(lblDir);
        txtDireccion = new JTextArea(3, 20);
        txtDireccion.setLineWrap(true);
        txtDireccion.setWrapStyleWord(true);
        txtDireccion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scrollDir = new JScrollPane(txtDireccion);
        scrollDir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panelDerecho.add(scrollDir);
        panelDerecho.add(Box.createVerticalStrut(8));

        // Observaciones
        JLabel lblObs = new JLabel("Observaciones:");
        lblObs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelDerecho.add(lblObs);
        txtObservaciones = new JTextArea(3, 20);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);
        txtObservaciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scrollObs = new JScrollPane(txtObservaciones);
        scrollObs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panelDerecho.add(scrollObs);
        panelDerecho.add(Box.createVerticalStrut(8));

        // Estado
        lblEstado = new JLabel("Estado: (sin seleccionar)");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelDerecho.add(lblEstado);
        panelDerecho.add(Box.createVerticalStrut(10));

        // Botones CRUD
        JPanel panelBotones = new JPanel(new GridLayout(2, 2, 8, 8));
        panelBotones.setOpaque(false);

        JButton btnNuevo      = crearBoton(dorado, "Nuevo / Limpiar");
        JButton btnGuardar    = crearBoton(dorado, "Guardar");
        JButton btnActualizar = crearBoton(dorado, "Actualizar");
        JButton btnEstado     = crearBoton(dorado, "Activar/Inactivar");

        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnEstado);

        panelDerecho.add(panelBotones);
        panelDerecho.add(Box.createVerticalStrut(15));

        // Búsqueda
        JSeparator sep = new JSeparator();
        panelDerecho.add(sep);
        panelDerecho.add(Box.createVerticalStrut(10));

        JLabel lblBuscar = new JLabel("Buscar proveedor (nombre o RUC):");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelDerecho.add(lblBuscar);

        txtBuscar = new JTextField();
        txtBuscar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnBuscar = crearBoton(dorado, "Buscar");
        JButton btnMostrarTodos = crearBoton(dorado, "Mostrar todos");

        panelDerecho.add(txtBuscar);
        panelDerecho.add(Box.createVerticalStrut(5));
        panelDerecho.add(btnBuscar);
        panelDerecho.add(Box.createVerticalStrut(5));
        panelDerecho.add(btnMostrarTodos);

        panelDerecho.add(Box.createVerticalGlue());

        add(panelDerecho, BorderLayout.EAST);

        // ==========================
        // EVENTOS
        // ==========================
        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnGuardar.addActionListener(e -> guardarProveedor());
        btnActualizar.addActionListener(e -> actualizarProveedor());
        btnEstado.addActionListener(e -> cambiarEstadoProveedor());

        btnBuscar.addActionListener(e -> buscarProveedores());
        btnMostrarTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarProveedores();
        });
    }
    
    // ==== LÓGICA DE NAVEGACIÓN ====
    private void volverAlMenu() {
        dispose();
        // Abrimos el menú principal con los mismos datos de sesión
        new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
    }

    // ========= helpers UI =========

    private JTextField crearCampoTexto(JPanel contenedor, String etiqueta) {
        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        contenedor.add(lbl);

        JTextField txt = new JTextField();
        txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contenedor.add(txt);
        contenedor.add(Box.createVerticalStrut(8));
        return txt;
    }

    private JButton crearBoton(Color color, String texto) {
        JButton b = new JButton(texto);
        b.setBackground(color);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return b;
    }

    // ========= Mongo: conexión y carga =========

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            proveedoresCol = db.getCollection("proveedores");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a MongoDB:\n" + ex.getMessage(),
                    "Error MongoDB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarProveedores() {
        modeloTabla.setRowCount(0);
        proveedorSeleccionadoId = null;
        lblEstado.setText("Estado: (sin seleccionar)");

        if (proveedoresCol == null) return;

        try (MongoCursor<Document> cursor = proveedoresCol.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                modeloTabla.addRow(new Object[]{
                        d.getObjectId("_id") != null ? d.getObjectId("_id").toHexString() : "",
                        d.getString("nombre"),
                        d.getString("ruc"),
                        d.getString("contacto"),
                        d.getString("telefono"),
                        d.getString("email"),
                        d.getString("condicionesPago"),
                        d.getString("estado")
                });
            }
        }
    }

    private void buscarProveedores() {
        String texto = txtBuscar.getText().trim();
        modeloTabla.setRowCount(0);
        proveedorSeleccionadoId = null;
        lblEstado.setText("Estado: (sin seleccionar)");

        if (proveedoresCol == null) return;

        Document filtro;
        if (texto.isEmpty()) {
            filtro = new Document(); // todos
        } else {
            filtro = new Document("$or", java.util.Arrays.asList(
                    new Document("nombre",
                            new Document("$regex", texto).append("$options", "i")),
                    new Document("ruc",
                            new Document("$regex", texto).append("$options", "i"))
            ));
        }

        try (MongoCursor<Document> cursor = proveedoresCol.find(filtro).iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                modeloTabla.addRow(new Object[]{
                        d.getObjectId("_id") != null ? d.getObjectId("_id").toHexString() : "",
                        d.getString("nombre"),
                        d.getString("ruc"),
                        d.getString("contacto"),
                        d.getString("telefono"),
                        d.getString("email"),
                        d.getString("condicionesPago"),
                        d.getString("estado")
                });
            }
        }
    }

    // ========= Form / selección =========

    private void limpiarFormulario() {
        proveedorSeleccionadoId = null;
        txtNombre.setText("");
        txtRuc.setText("");
        txtContacto.setText("");
        txtTelefono.setText("");
        txtEmail.setText("");
        txtDescuento.setText("");
        cbCondiciones.setSelectedIndex(0);
        txtDireccion.setText("");
        txtObservaciones.setText("");
        lblEstado.setText("Estado: (sin seleccionar)");
        tablaProveedores.clearSelection();
    }

    private void cargarProveedorSeleccionado() {
        int fila = tablaProveedores.getSelectedRow();
        if (fila == -1) {
            limpiarFormulario();
            return;
        }

        String id = (String) modeloTabla.getValueAt(fila, 0);
        if (id == null || id.isEmpty() || proveedoresCol == null) {
            limpiarFormulario();
            return;
        }

        proveedorSeleccionadoId = id;

        try {
            Document d = proveedoresCol.find(Filters.eq("_id", new ObjectId(id))).first();
            if (d == null) {
                limpiarFormulario();
                return;
            }

            txtNombre.setText(d.getString("nombre"));
            txtRuc.setText(d.getString("ruc"));
            txtContacto.setText(d.getString("contacto"));
            txtTelefono.setText(d.getString("telefono"));
            txtEmail.setText(d.getString("email"));

            String cond = d.getString("condicionesPago");
            if (cond != null) cbCondiciones.setSelectedItem(cond);

            Double desc = d.getDouble("descuento");
            txtDescuento.setText(desc != null ? String.valueOf(desc) : "");

            txtDireccion.setText(d.getString("direccion"));
            txtObservaciones.setText(d.getString("observaciones"));

            String estado = d.getString("estado");
            if (estado == null) estado = "ACTIVO";
            lblEstado.setText("Estado: " + estado);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar proveedor: " + ex.getMessage());
        }
    }

    // ========= CRUD =========

    private boolean validarFormularioBasico() {
        String nombre = txtNombre.getText().trim();
        String ruc    = txtRuc.getText().trim();
        String tel    = txtTelefono.getText().trim();
        String email  = txtEmail.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del proveedor es obligatorio.");
            return false;
        }
        if (!ruc.matches("\\d{10,13}")) {
            JOptionPane.showMessageDialog(this,
                    "El RUC/Cédula debe tener entre 10 y 13 dígitos numéricos.");
            return false;
        }
        if (!tel.isEmpty() && !tel.matches("\\d{7,10}")) {
            JOptionPane.showMessageDialog(this,
                    "El teléfono debe contener solo números (7 a 10 dígitos) o dejarse vacío.");
            return false;
        }
        if (!email.isEmpty() && !email.matches("^.+@.+\\..+$")) {
            JOptionPane.showMessageDialog(this, "El email no tiene un formato válido.");
            return false;
        }
        return true;
    }

    private void guardarProveedor() {
        if (proveedoresCol == null) return;
        if (!validarFormularioBasico()) return;

        String nombre = txtNombre.getText().trim();
        String ruc    = txtRuc.getText().trim();

        // RUC único
        Document existe = proveedoresCol.find(Filters.eq("ruc", ruc)).first();
        if (existe != null) {
            JOptionPane.showMessageDialog(this,
                    "Ya existe un proveedor con ese RUC/Cédula.");
            return;
        }

        String contacto = txtContacto.getText().trim();
        String tel      = txtTelefono.getText().trim();
        String email    = txtEmail.getText().trim();
        String cond     = (String) cbCondiciones.getSelectedItem();
        String dir      = txtDireccion.getText().trim();
        String obs      = txtObservaciones.getText().trim();
        double desc     = 0.0;

        if (!txtDescuento.getText().trim().isEmpty()) {
            try {
                desc = Double.parseDouble(txtDescuento.getText().trim());
                if (desc < 0 || desc > 50) {
                    JOptionPane.showMessageDialog(this,
                            "El descuento debe estar entre 0 y 50%.");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "El descuento debe ser un número.");
                return;
            }
        }

        Document nuevo = new Document("nombre", nombre)
                .append("ruc", ruc)
                .append("contacto", contacto)
                .append("telefono", tel)
                .append("email", email)
                .append("condicionesPago", cond)
                .append("descuento", desc)
                .append("direccion", dir)
                .append("observaciones", obs)
                .append("estado", "ACTIVO");

        try {
            proveedoresCol.insertOne(nuevo);
            JOptionPane.showMessageDialog(this, "Proveedor registrado correctamente ✅");
            cargarProveedores();
            limpiarFormulario();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al guardar proveedor: " + ex.getMessage());
        }
    }

    private void actualizarProveedor() {
        if (proveedoresCol == null) return;
        if (proveedorSeleccionadoId == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecciona un proveedor en la tabla para actualizar.");
            return;
        }
        if (!validarFormularioBasico()) return;

        String nombre = txtNombre.getText().trim();
        String ruc    = txtRuc.getText().trim();
        String contacto = txtContacto.getText().trim();
        String tel      = txtTelefono.getText().trim();
        String email    = txtEmail.getText().trim();
        String cond     = (String) cbCondiciones.getSelectedItem();
        String dir      = txtDireccion.getText().trim();
        String obs      = txtObservaciones.getText().trim();
        double desc     = 0.0;

        if (!txtDescuento.getText().trim().isEmpty()) {
            try {
                desc = Double.parseDouble(txtDescuento.getText().trim());
                if (desc < 0 || desc > 50) {
                    JOptionPane.showMessageDialog(this,
                            "El descuento debe estar entre 0 y 50%.");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "El descuento debe ser un número.");
                return;
            }
        }

        try {
            UpdateResult res = proveedoresCol.updateOne(
                    Filters.eq("_id", new ObjectId(proveedorSeleccionadoId)),
                    Updates.combine(
                            Updates.set("nombre", nombre),
                            Updates.set("ruc", ruc),
                            Updates.set("contacto", contacto),
                            Updates.set("telefono", tel),
                            Updates.set("email", email),
                            Updates.set("condicionesPago", cond),
                            Updates.set("descuento", desc),
                            Updates.set("direccion", dir),
                            Updates.set("observaciones", obs)
                    )
            );

            if (res.getModifiedCount() > 0) {
                JOptionPane.showMessageDialog(this, "Proveedor actualizado correctamente ✏️");
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se realizaron cambios (verifica los datos).");
            }
            cargarProveedores();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al actualizar proveedor: " + ex.getMessage());
        }
    }

    private void cambiarEstadoProveedor() {
        if (proveedoresCol == null) return;
        if (proveedorSeleccionadoId == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecciona un proveedor en la tabla para cambiar su estado.");
            return;
        }

        try {
            Document d = proveedoresCol.find(Filters.eq("_id", new ObjectId(proveedorSeleccionadoId))).first();
            if (d == null) {
                JOptionPane.showMessageDialog(this,
                        "No se encontró el proveedor en la base de datos.");
                return;
            }

            String estadoActual = d.getString("estado");
            if (estadoActual == null) estadoActual = "ACTIVO";

            String nuevoEstado = "ACTIVO".equalsIgnoreCase(estadoActual) ? "INACTIVO" : "ACTIVO";

            proveedoresCol.updateOne(
                    Filters.eq("_id", new ObjectId(proveedorSeleccionadoId)),
                    Updates.set("estado", nuevoEstado)
            );

            JOptionPane.showMessageDialog(this,
                    "Estado cambiado a: " + nuevoEstado);
            cargarProveedores();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cambiar estado: " + ex.getMessage());
        }
    }

    // Para pruebas independientes
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProveedorFrame().setVisible(true));
    }
}