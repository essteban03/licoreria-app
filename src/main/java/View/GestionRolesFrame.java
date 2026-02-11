package View;

import Controler.MongoConnection;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GestionRolesFrame extends JFrame {

    // ==== DATOS DE SESIÓN (Para navegación circular) ====
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;

    // ==== MONGODB ====
    private MongoCollection<Document> rolesCol;
    
    // ==== COMPONENTES UI ====
    private JTable tablaRoles;
    private DefaultTableModel modeloTabla;

    private JTextField txtNombreRol;
    private JTextField txtDescripcion;

    // Checkboxes de permisos
    private JCheckBox chkCatalogo;
    private JCheckBox chkBodega;
    private JCheckBox chkGestProd;
    private JCheckBox chkGestPedidos;
    private JCheckBox chkGestUsuarios;
    private JCheckBox chkGestRoles;

    // Para saber cuál rol está seleccionado
    private String rolIdSeleccionado = null;

    // ==== CONSTRUCTOR COMPLETO (Usado desde el Menú) ====
    public GestionRolesFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos;

        initDb();
        initUI();
        cargarRoles();
    }

    // ==== CONSTRUCTOR DE COMPATIBILIDAD ====
    public GestionRolesFrame() {
        this("admin", "admin", null);
    }

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            rolesCol = db.getCollection("roles");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a MongoDB: " + e.getMessage(),
                    "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setTitle("Gestión de Roles y Permisos");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ==============================
        // 1. ENCABEZADO UNIFICADO
        // ==============================
        add(EstilosApp.crearHeader("Administración de Roles", usuarioActual, e -> volverAlMenu()), BorderLayout.NORTH);

        // ==============================
        // 2. TABLA DE ROLES (IZQUIERDA)
        // ==============================
        String[] columnas = {"ID", "Nombre rol", "Descripción", "Permisos"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // tabla solo lectura
            }
        };
        tablaRoles = new JTable(modeloTabla);
        tablaRoles.setRowHeight(25);
        tablaRoles.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaRoles.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JScrollPane scrollTabla = new JScrollPane(tablaRoles);

        // Cuando seleccionas un rol en la tabla → cargar en formulario
        tablaRoles.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = tablaRoles.getSelectedRow();
                if (fila >= 0) {
                    String id = (String) modeloTabla.getValueAt(fila, 0);
                    cargarRolEnFormulario(id);
                }
            }
        });

        // ==============================
        // 3. PANEL FORMULARIO (DERECHA)
        // ==============================
        JPanel panelDerecha = new JPanel();
        panelDerecha.setBackground(new Color(245, 245, 245)); // o EstilosApp.COLOR_FONDO
        panelDerecha.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelDerecha.setLayout(new BoxLayout(panelDerecha, BoxLayout.Y_AXIS));

        JLabel lblForm = new JLabel("Detalle del Rol");
        lblForm.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelDerecha.add(lblForm);
        panelDerecha.add(Box.createVerticalStrut(15));

        // Campos de nombre y descripción
        panelDerecha.add(crearLabelCampo("Nombre del rol:"));
        txtNombreRol = new JTextField();
        txtNombreRol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtNombreRol.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelDerecha.add(txtNombreRol);
        panelDerecha.add(Box.createVerticalStrut(10));

        panelDerecha.add(crearLabelCampo("Descripción:"));
        txtDescripcion = new JTextField();
        txtDescripcion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtDescripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelDerecha.add(txtDescripcion);
        panelDerecha.add(Box.createVerticalStrut(20));

        // Panel de permisos
        JLabel lblPerm = new JLabel("Permisos (Módulos accesibles):");
        lblPerm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPerm.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelDerecha.add(lblPerm);
        panelDerecha.add(Box.createVerticalStrut(5));

        chkCatalogo = new JCheckBox("Acceso a Catálogo (CATALOGO_CLIENTE)");
        chkBodega = new JCheckBox("Módulo de Bodega (BODEGA)");
        chkGestProd = new JCheckBox("Gestión de Productos (GESTION_PRODUCTOS)");
        chkGestPedidos = new JCheckBox("Gestión de Pedidos (GESTION_PEDIDOS)");
        chkGestUsuarios = new JCheckBox("Gestión de Usuarios (GESTION_USUARIOS)");
        chkGestRoles = new JCheckBox("Gestión de Roles (GESTION_ROLES)");

        // Añadir checkboxes y alinearlos
        for (JCheckBox chk : Arrays.asList(chkCatalogo, chkBodega, chkGestProd, chkGestPedidos, chkGestUsuarios, chkGestRoles)) {
            chk.setBackground(new Color(245, 245, 245));
            chk.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            chk.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelDerecha.add(chk);
        }

        panelDerecha.add(Box.createVerticalGlue()); // Empujar botones abajo
        panelDerecha.add(Box.createVerticalStrut(15));

        // Botones CRUD
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBotones.setBackground(new Color(245, 245, 245));
        panelBotones.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton btnNuevo = crearBoton("Nuevo / Limpiar");
        JButton btnGuardar = crearBoton("Guardar");
        JButton btnEliminar = crearBoton("Eliminar");

        btnNuevo.addActionListener(e -> limpiarFormulario());
        btnGuardar.addActionListener(e -> guardarActualizarRol());
        btnEliminar.addActionListener(e -> eliminarRolSeleccionado());

        panelBotones.add(btnNuevo);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnEliminar);

        panelDerecha.add(panelBotones);

        // ===== DIVISIÓN CENTRO: TABLA IZQUIERDA + FORM DERECHA =====
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                scrollTabla,
                panelDerecha
        );
        split.setDividerLocation(500); // Dar más espacio a la tabla
        add(split, BorderLayout.CENTER);
    }
    
    // ==== LÓGICA DE NAVEGACIÓN ====
    private void volverAlMenu() {
        dispose();
        new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
    }

    // ==== MÉTODOS UI HELPERS ====
    
    private JLabel crearLabelCampo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
    
    private JButton crearBoton(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(new Color(212, 175, 55)); // Dorado
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        return btn;
    }

    // ==== MÉTODOS CRUD ====

    private void cargarRoles() {
        modeloTabla.setRowCount(0);
        if (rolesCol == null) return;

        try (MongoCursor<Document> cur = rolesCol.find().iterator()) {
            while (cur.hasNext()) {
                Document d = cur.next();
                String id = d.getObjectId("_id").toHexString();
                String nombre = d.getString("nombre");
                String desc = d.getString("descripcion");
                List<String> permisos = (List<String>) d.get("permisos");
                String textoPermisos = (permisos != null)
                        ? String.join(", ", permisos)
                        : "";
                modeloTabla.addRow(new Object[]{id, nombre, desc, textoPermisos});
            }
        }
    }

    private void cargarRolEnFormulario(String id) {
        if (rolesCol == null) return;
        Document d = rolesCol.find(Filters.eq("_id", new ObjectId(id))).first();
        if (d == null) return;

        rolIdSeleccionado = id;
        txtNombreRol.setText(d.getString("nombre"));
        txtDescripcion.setText(d.getString("descripcion"));

        List<String> permisos = (List<String>) d.get("permisos");
        if (permisos == null) permisos = new ArrayList<>();

        chkCatalogo.setSelected(permisos.contains("CATALOGO_CLIENTE"));
        chkBodega.setSelected(permisos.contains("BODEGA"));
        chkGestProd.setSelected(permisos.contains("GESTION_PRODUCTOS"));
        chkGestPedidos.setSelected(permisos.contains("GESTION_PEDIDOS"));
        chkGestUsuarios.setSelected(permisos.contains("GESTION_USUARIOS"));
        chkGestRoles.setSelected(permisos.contains("GESTION_ROLES"));
    }

    private void limpiarFormulario() {
        rolIdSeleccionado = null;
        txtNombreRol.setText("");
        txtDescripcion.setText("");
        chkCatalogo.setSelected(false);
        chkBodega.setSelected(false);
        chkGestProd.setSelected(false);
        chkGestPedidos.setSelected(false);
        chkGestUsuarios.setSelected(false);
        chkGestRoles.setSelected(false);
        tablaRoles.clearSelection();
    }

    private void guardarActualizarRol() {
        if (rolesCol == null) {
            JOptionPane.showMessageDialog(this, "Colección roles no disponible");
            return;
        }

        String nombre = txtNombreRol.getText().trim();
        String desc = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del rol es obligatorio");
            return;
        }

        // Construir lista de permisos según checkboxes
        List<String> permisos = new ArrayList<>();
        if (chkCatalogo.isSelected()) permisos.add("CATALOGO_CLIENTE");
        if (chkBodega.isSelected()) permisos.add("BODEGA");
        if (chkGestProd.isSelected()) permisos.add("GESTION_PRODUCTOS");
        if (chkGestPedidos.isSelected()) permisos.add("GESTION_PEDIDOS");
        if (chkGestUsuarios.isSelected()) permisos.add("GESTION_USUARIOS");
        if (chkGestRoles.isSelected()) permisos.add("GESTION_ROLES");

        if (permisos.isEmpty()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Este rol no tiene ningún permiso. ¿Guardar de todos modos?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }

        // Verificar unicidad de nombre (evitar duplicados)
        Document existente = rolesCol.find(Filters.eq("nombre", nombre)).first();
        if (rolIdSeleccionado == null) {
            // Creando nuevo rol
            if (existente != null) {
                JOptionPane.showMessageDialog(this, "Ya existe un rol con ese nombre.");
                return;
            }
            Document nuevo = new Document("nombre", nombre)
                    .append("descripcion", desc)
                    .append("permisos", permisos);
            rolesCol.insertOne(nuevo);
            JOptionPane.showMessageDialog(this, "Rol creado correctamente ✅");
        } else {
            // Actualizando rol existente
            Document rolActual = rolesCol.find(Filters.eq("_id", new ObjectId(rolIdSeleccionado))).first();
            if (rolActual == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el rol seleccionado.");
                return;
            }

            // Si cambiaron el nombre, hay que verificar que no choque con otro rol
            String nombreActual = rolActual.getString("nombre");
            if (!nombre.equals(nombreActual) && existente != null) {
                JOptionPane.showMessageDialog(this, "Ya existe otro rol con ese nombre.");
                return;
            }

            rolesCol.updateOne(
                    Filters.eq("_id", new ObjectId(rolIdSeleccionado)),
                    new Document("$set", new Document("nombre", nombre)
                            .append("descripcion", desc)
                            .append("permisos", permisos))
            );
            JOptionPane.showMessageDialog(this, "Rol actualizado correctamente ✏️");
        }

        cargarRoles();
        limpiarFormulario();
    }

    private void eliminarRolSeleccionado() {
        if (rolesCol == null) return;
        int fila = tablaRoles.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un rol en la tabla");
            return;
        }
        String id = (String) modeloTabla.getValueAt(fila, 0);
        String nombreRol = (String) modeloTabla.getValueAt(fila, 1);

        // Por seguridad, evita que el admin se borre
        if ("admin".equalsIgnoreCase(nombreRol)) {
            JOptionPane.showMessageDialog(this, "No se puede eliminar el rol 'admin'.");
            return;
        }

        int op = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de eliminar el rol '" + nombreRol + "'?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

        if (op == JOptionPane.YES_OPTION) {
            rolesCol.deleteOne(Filters.eq("_id", new ObjectId(id)));
            JOptionPane.showMessageDialog(this, "Rol eliminado correctamente 🗑️");
            cargarRoles();
            limpiarFormulario();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestionRolesFrame().setVisible(true));
    }
}