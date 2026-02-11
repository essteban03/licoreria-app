package View;

import Controler.MongoConnection;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class MisPedidosFrame extends JFrame {

    private final String usuarioActual;

    private MongoCollection<Document> pedidosCol;

    private JTable tabla;
    private DefaultTableModel modelo;

    // Guardamos los pedidos cargados (para ver detalle)
    private ArrayList<Document> pedidosCargados = new ArrayList<>();

    public MisPedidosFrame(String usuarioActual) {
        this.usuarioActual = (usuarioActual != null && !usuarioActual.trim().isEmpty())
                ? usuarioActual
                : "anonimo";

        try {
            MongoDatabase db = MongoConnection.getDatabase();
            pedidosCol = db.getCollection("pedidos");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error BD: " + e.getMessage());
        }

        initUI();
        cargarPedidos();
    }

    private void initUI() {
        setTitle("Mis Pedidos - Historial");
        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // HEADER
        JPanel header = EstilosApp.crearHeader("Mis Pedidos", usuarioActual, e -> dispose());
        add(header, BorderLayout.NORTH);

        // CENTRO: tabla
        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(EstilosApp.COLOR_FONDO);
        centro.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Fecha", "Cliente", "Total", "Método Pago", "Estado"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scroll = new JScrollPane(tabla);
        centro.add(scroll, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);

        // SUR: botones
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.setBackground(EstilosApp.COLOR_FONDO);
        sur.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnVer = new JButton("Ver Detalle");
        btnVer.setBackground(EstilosApp.COLOR_DORADO);
        btnVer.setFont(EstilosApp.FUENTE_BOLD);
        btnVer.addActionListener(e -> verDetalle());

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.setBackground(Color.LIGHT_GRAY);
        btnRefrescar.addActionListener(e -> cargarPedidos());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setBackground(new Color(230, 230, 230));
        btnCerrar.addActionListener(e -> dispose());

        sur.add(btnRefrescar);
        sur.add(btnVer);
        sur.add(btnCerrar);

        add(sur, BorderLayout.SOUTH);
    }

    private void cargarPedidos() {
        if (pedidosCol == null) return;

        modelo.setRowCount(0);
        pedidosCargados.clear();

        try (MongoCursor<Document> cursor = pedidosCol
                .find(Filters.eq("usuario", usuarioActual))
                .iterator()) {

            while (cursor.hasNext()) {
                Document p = cursor.next();
                pedidosCargados.add(p);

                String fecha = p.getString("fechaHora");
                String cliente = p.getString("cliente");
                String metodo = p.getString("metodoPago");
                String estado = p.getString("estado");

                double total = 0;
                if (p.get("total") instanceof Double) total = p.getDouble("total");
                if (p.get("total") instanceof Integer) total = p.getInteger("total");

                modelo.addRow(new Object[]{
                        fecha,
                        cliente,
                        String.format("$%.2f", total),
                        metodo,
                        estado
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando pedidos: " + e.getMessage());
        }

        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No tienes pedidos registrados todavía.",
                    "Historial",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void verDetalle() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un pedido primero.");
            return;
        }

        Document pedido = pedidosCargados.get(row);
        new DetallePedidoFrame(pedido).setVisible(true);
    }
}