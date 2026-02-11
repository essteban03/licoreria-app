package View;

import Controler.MongoConnection;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ListaUsuariosFrame extends JFrame {

    // ==== DATOS DE SESIÓN (Para navegación circular) ====
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;

    // ==== MONGODB ====
    private MongoCollection<Document> coleccionUsuarios;
    private MongoCollection<Document> coleccionRoles;
    
    // ==== COMPONENTES UI ====
    private DefaultTableModel modeloTabla;
    private JTable tablaUsuarios;

    // Constructor Completo (Usado desde el Menú Principal)
    public ListaUsuariosFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos;

        // Configuración de la ventana
        setTitle("Gestión de Usuarios - Licorería");
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 240, 240));

        initDb();
        initUI();
        cargarUsuarios();
    }

    // Constructor vacío (Para pruebas o compatibilidad)
    public ListaUsuariosFrame() {
        this("admin", "admin", null);
    }

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            coleccionUsuarios = db.getCollection("usuarios");
            coleccionRoles = db.getCollection("roles");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al conectar con MongoDB", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        // ==== 1. ENCABEZADO UNIFICADO ====
        // Usamos EstilosApp para tener el botón "Volver" estandarizado arriba
        add(EstilosApp.crearHeader("Administración de Usuarios", usuarioActual, e -> volverAlMenu()), BorderLayout.NORTH);

        // ==== 2. TABLA CENTRAL ====
        String[] columnas = {"ID", "Nombre", "Apellido", "Cédula", "Usuario", "Rol"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override // Hacemos que la tabla no sea editable directamente
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        tablaUsuarios = new JTable(modeloTabla);
        tablaUsuarios.setRowHeight(30);
        tablaUsuarios.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaUsuarios.setSelectionBackground(new Color(212, 175, 55)); // Dorado al seleccionar
        tablaUsuarios.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JScrollPane scroll = new JScrollPane(tablaUsuarios);
        add(scroll, BorderLayout.CENTER);

        // ==== 3. PANEL DE BOTONES (CRUD) ====
        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(new Color(240, 240, 240));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JButton btnAgregar = crearBoton("Agregar");
        JButton btnEditar = crearBoton("Editar");
        JButton btnEliminar = crearBoton("Eliminar");
        JButton btnActualizar = crearBoton("Refrescar Lista");

        // Asignar eventos
        btnAgregar.addActionListener(this::agregarUsuario);
        btnEditar.addActionListener(this::editarUsuario);
        btnEliminar.addActionListener(this::eliminarUsuario);
        btnActualizar.addActionListener(e -> cargarUsuarios());

        panelBotones.add(btnAgregar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        panelBotones.add(Box.createHorizontalStrut(20)); // Separador
        panelBotones.add(btnActualizar);

        add(panelBotones, BorderLayout.SOUTH);
    }

    // ==== LÓGICA DE NAVEGACIÓN ====
    private void volverAlMenu() {
        dispose(); // Cerramos esta ventana
        // Volvemos al menú con los datos de sesión intactos
        new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
    }

    // ==== MÉTODOS AUXILIARES ====
    
    private JButton crearBoton(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(new Color(212, 175, 55)); // Dorado
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return btn;
    }

    private DefaultComboBoxModel<String> obtenerModeloRoles() {
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        modelo.addElement("usuario"); // Default

        if (coleccionRoles == null) return modelo;

        try (MongoCursor<Document> cursor = coleccionRoles.find().iterator()) {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                String nombreRol = d.getString("nombre");
                if (nombreRol != null && !nombreRol.isEmpty()) {
                    // Evitar duplicados si "usuario" ya viene de la BD
                    boolean yaExiste = false;
                    for (int i = 0; i < modelo.getSize(); i++) {
                        if (nombreRol.equalsIgnoreCase(modelo.getElementAt(i))) {
                            yaExiste = true;
                            break;
                        }
                    }
                    if (!yaExiste) modelo.addElement(nombreRol);
                }
            }
        }
        return modelo;
    }

    private void cargarUsuarios() {
        modeloTabla.setRowCount(0);
        if (coleccionUsuarios == null) return;
        
        try (MongoCursor<Document> cursor = coleccionUsuarios.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                modeloTabla.addRow(new Object[]{
                        doc.getObjectId("_id").toString(),
                        doc.getString("nombre"),
                        doc.getString("apellido"),
                        doc.getString("cedula"),
                        doc.getString("usuario"),
                        doc.getString("rol")
                });
            }
        }
    }

    // ==== MÉTODOS CRUD ====

    private void agregarUsuario(ActionEvent e) {
        JTextField txtNombre = new JTextField();
        JTextField txtApellido = new JTextField();
        JTextField txtCedula = new JTextField();
        JTextField txtUsuario = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JComboBox<String> cbRol = new JComboBox<>(obtenerModeloRoles());

        Object[] campos = {
                "Nombre:", txtNombre,
                "Apellido:", txtApellido,
                "Cédula:", txtCedula,
                "Usuario:", txtUsuario,
                "Contraseña:", txtPass,
                "Rol:", cbRol
        };

        int opcion = JOptionPane.showConfirmDialog(this, campos, "Agregar Usuario", JOptionPane.OK_CANCEL_OPTION);
        
        if (opcion == JOptionPane.OK_OPTION) {
            if (txtNombre.getText().isEmpty() || txtUsuario.getText().isEmpty() || txtPass.getPassword().length == 0) {
                JOptionPane.showMessageDialog(this, "Nombre, Usuario y Contraseña son obligatorios");
                return;
            }

            Document nuevo = new Document("nombre", txtNombre.getText())
                    .append("apellido", txtApellido.getText())
                    .append("cedula", txtCedula.getText())
                    .append("usuario", txtUsuario.getText())
                    .append("password", new String(txtPass.getPassword())) // NOTA: Idealmente hashear esto
                    .append("rol", cbRol.getSelectedItem().toString());
            
            coleccionUsuarios.insertOne(nuevo);
            JOptionPane.showMessageDialog(this, "Usuario agregado correctamente ✅");
            cargarUsuarios();
        }
    }

    private void editarUsuario(ActionEvent e) {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para editar");
            return;
        }

        String id = (String) modeloTabla.getValueAt(fila, 0);
        String nombreActual = (String) modeloTabla.getValueAt(fila, 1);
        String apellidoActual = (String) modeloTabla.getValueAt(fila, 2);
        String cedulaActual = (String) modeloTabla.getValueAt(fila, 3);
        String usuarioActualT = (String) modeloTabla.getValueAt(fila, 4);
        String rolActualT = (String) modeloTabla.getValueAt(fila, 5);

        JTextField txtNombre = new JTextField(nombreActual);
        JTextField txtApellido = new JTextField(apellidoActual);
        JTextField txtCedula = new JTextField(cedulaActual);
        JTextField txtUsuario = new JTextField(usuarioActualT);
        JComboBox<String> cbRol = new JComboBox<>(obtenerModeloRoles());
        cbRol.setSelectedItem(rolActualT);

        Object[] campos = {
                "Nombre:", txtNombre,
                "Apellido:", txtApellido,
                "Cédula:", txtCedula,
                "Usuario:", txtUsuario,
                "Rol:", cbRol
        };

        int opcion = JOptionPane.showConfirmDialog(this, campos, "Editar Usuario", JOptionPane.OK_CANCEL_OPTION);
        
        if (opcion == JOptionPane.OK_OPTION) {
            coleccionUsuarios.updateOne(
                    new Document("_id", new ObjectId(id)),
                    new Document("$set", new Document("nombre", txtNombre.getText())
                            .append("apellido", txtApellido.getText())
                            .append("cedula", txtCedula.getText())
                            .append("usuario", txtUsuario.getText())
                            .append("rol", cbRol.getSelectedItem().toString()))
            );
            JOptionPane.showMessageDialog(this, "Usuario actualizado correctamente ✏️");
            cargarUsuarios();
        }
    }

    private void eliminarUsuario(ActionEvent e) {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar");
            return;
        }

        String id = (String) modeloTabla.getValueAt(fila, 0);
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas eliminar este usuario?", "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            coleccionUsuarios.deleteOne(new Document("_id", new ObjectId(id)));
            JOptionPane.showMessageDialog(this, "Usuario eliminado correctamente 🗑️");
            cargarUsuarios();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ListaUsuariosFrame().setVisible(true));
    }
}