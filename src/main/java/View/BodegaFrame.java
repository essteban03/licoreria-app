package View;

import Controler.MongoConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class BodegaFrame extends JFrame {
    
    // ==== DATOS DE SESIÓN (Para navegación circular) ====
    private final String usuarioActual;
    private final String rolActual;
    private final List<String> permisosActuales;
    
    // ==== MONGODB ====
    private MongoCollection<Document> colBodega;

    // ==== COMPONENTES UI ====
    private JTextField txtProducto;
    private JTextField txtCategoria;
    private JTextField txtPresentacion;
    private JTextField txtCantidad;
    private JTextField txtObs;

    private JTable tabla;
    private DefaultTableModel modelo;
    private JCheckBox chkSoloPendientes;

    // Constructor Completo (Usado desde el Menú Principal)
    public BodegaFrame(String usuario, String rol, List<String> permisos) {
        this.usuarioActual = usuario;
        this.rolActual = rol;
        this.permisosActuales = permisos;

        initDb();
        initUI();
        cargarRegistros(true);
    }
    
    // Constructor de Compatibilidad (Por si se llama directo para pruebas)
    public BodegaFrame(String usuarioActual) {
        this(usuarioActual, "bodeguero", null);
    }

    private void initDb() {
        try {
            MongoDatabase db = MongoConnection.getDatabase();
            colBodega = db.getCollection("bodega_registros");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar a MongoDB: " + e.getMessage(),
                    "Error BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setTitle("Módulo de Bodega - Registro de entradas");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ==== 1. ENCABEZADO UNIFICADO ====
        // Usamos EstilosApp para el header con botón volver
        add(EstilosApp.crearHeader("Bodega - Recepción de Mercadería", usuarioActual, e -> volverAlMenu()), BorderLayout.NORTH);

        // ==== 2. FORMULARIO IZQUIERDA ====
        JPanel panelForm = new JPanel();
        panelForm.setBackground(new Color(245, 245, 245)); // O EstilosApp.COLOR_FONDO
        panelForm.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelForm.setLayout(new BoxLayout(panelForm, BoxLayout.Y_AXIS));
        panelForm.setPreferredSize(new Dimension(350, 0));

        JLabel lblSeccion = new JLabel("Registrar nueva entrada");
        lblSeccion.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panelForm.add(lblSeccion);
        panelForm.add(Box.createVerticalStrut(15));

        panelForm.add(crearCampo("Nombre del producto:", txtProducto = new JTextField()));
        panelForm.add(crearCampo("Categoría:", txtCategoria = new JTextField()));
        panelForm.add(crearCampo("Presentación:", txtPresentacion = new JTextField()));
        panelForm.add(crearCampo("Cantidad recibida:", txtCantidad = new JTextField()));
        panelForm.add(crearCampo("Observación:", txtObs = new JTextField()));

        JButton btnRegistrar = new JButton("Registrar entrada");
        btnRegistrar.setBackground(new Color(212, 175, 55)); // Dorado
        btnRegistrar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.addActionListener(e -> registrarEntrada());
        
        panelForm.add(Box.createVerticalStrut(15));
        panelForm.add(btnRegistrar);

        // ==== 3. TABLA DERECHA ====
        String[] columnas = {"ID", "Fecha", "Producto", "Cant.", "Estado", "Usuario"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scrollTabla = new JScrollPane(tabla);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBackground(new Color(245, 245, 245));
        panelTabla.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel de filtros superior a la tabla
        JPanel panelOpcionesTabla = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelOpcionesTabla.setBackground(new Color(245, 245, 245));
        
        chkSoloPendientes = new JCheckBox("Mostrar solo pendientes");
        chkSoloPendientes.setSelected(true);
        chkSoloPendientes.setBackground(new Color(245, 245, 245));

        JButton btnRefrescar = new JButton("Actualizar lista");
        btnRefrescar.addActionListener(e -> cargarRegistros(chkSoloPendientes.isSelected()));

        panelOpcionesTabla.add(chkSoloPendientes);
        panelOpcionesTabla.add(btnRefrescar);

        panelTabla.add(panelOpcionesTabla, BorderLayout.NORTH);
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        // ==== SPLIT ====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelForm, panelTabla);
        split.setDividerLocation(360); // Ancho fijo para el formulario
        add(split, BorderLayout.CENTER);
    }
    
    // ==== LÓGICA DE NAVEGACIÓN ====
    private void volverAlMenu() {
        dispose();
        new MenuPrincipalFrame(usuarioActual, rolActual, permisosActuales).setVisible(true);
    }

    private JPanel crearCampo(String etiqueta, JTextField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(245, 245, 245));
        p.setAlignmentX(Component.LEFT_ALIGNMENT); // Alinear a la izquierda

        JLabel lbl = new JLabel(etiqueta);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        p.add(lbl);
        p.add(field);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    private void registrarEntrada() {
        if (colBodega == null) {
            JOptionPane.showMessageDialog(this, "Colección de bodega no disponible");
            return;
        }

        String producto = txtProducto.getText().trim();
        String categoria = txtCategoria.getText().trim();
        String presentacion = txtPresentacion.getText().trim();
        String cantStr = txtCantidad.getText().trim();
        String obs = txtObs.getText().trim();

        if (producto.isEmpty() || cantStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Producto y cantidad son obligatorios.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantStr);
            if (cantidad <= 0) {
                throw new NumberFormatException("Cantidad no válida");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cantidad debe ser un número entero positivo.");
            return;
        }

        Document reg = new Document("fecha", LocalDate.now().toString())
                .append("producto", producto)
                .append("categoria", categoria)
                .append("presentacion", presentacion)
                .append("cantidad", cantidad)
                .append("observacion", obs)
                .append("estado", "pendiente")
                .append("usuario", usuarioActual != null ? usuarioActual : "desconocido");

        try {
            colBodega.insertOne(reg);
            JOptionPane.showMessageDialog(this, "Entrada registrada correctamente ✅");
            limpiarCampos();
            cargarRegistros(chkSoloPendientes.isSelected());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al registrar: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        txtProducto.setText("");
        txtCategoria.setText("");
        txtPresentacion.setText("");
        txtCantidad.setText("");
        txtObs.setText("");
    }

    private void cargarRegistros(boolean soloPendientes) {
        modelo.setRowCount(0);
        if (colBodega == null) return;

        Document filtro = new Document();
        if (soloPendientes) {
            filtro.append("estado", "pendiente");
        }

        try (MongoCursor<Document> cur = colBodega
                .find(filtro)
                .sort(Sorts.descending("fecha"))
                .iterator()) {

            while (cur.hasNext()) {
                Document d = cur.next();
                String id = d.getObjectId("_id").toHexString();
                String fecha = d.getString("fecha");
                String prod = d.getString("producto");
                Integer cant = d.getInteger("cantidad", 0);
                // String presentacion = d.getString("presentacion"); // (Opcional si quieres mostrarlo en tabla)
                String estado = d.getString("estado");
                String usuario = d.getString("usuario");

                modelo.addRow(new Object[]{
                        id,
                        fecha,
                        prod,
                        cant,
                        estado,
                        usuario
                });
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BodegaFrame("bodegueroDemo").setVisible(true));
    }   
}