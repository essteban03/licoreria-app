package View;

import org.bson.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class DetallePedidoFrame extends JFrame {

    private final Document pedido;

    private JTable tabla;
    private DefaultTableModel modelo;

    public DetallePedidoFrame(Document pedido) {
        this.pedido = pedido;

        initUI();
        cargarProductos();
    }

    private void initUI() {
        setTitle("Detalle del Pedido");
        setSize(750, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(EstilosApp.COLOR_FONDO);
        top.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Detalle del Pedido");
        titulo.setFont(EstilosApp.FUENTE_TITULO);

        JLabel info = new JLabel(
                "Fecha: " + pedido.getString("fechaHora") +
                        " | Cliente: " + pedido.getString("cliente") +
                        " | Total: $" + String.format("%.2f", pedido.getDouble("total"))
        );
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        top.add(titulo, BorderLayout.NORTH);
        top.add(info, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        // Tabla productos
        String[] cols = {"Producto", "Precio", "Cantidad", "Total Línea"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Sur
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.setBackground(EstilosApp.COLOR_FONDO);
        sur.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());

        sur.add(cerrar);
        add(sur, BorderLayout.SOUTH);
    }

    private void cargarProductos() {
        modelo.setRowCount(0);

        try {
            ArrayList<Document> items = (ArrayList<Document>) pedido.get("productos");

            for (Document item : items) {
                String nombre = item.getString("nombre");
                double precio = item.getDouble("precio");
                int cantidad = item.getInteger("cantidad", 0);

                double totalLinea = precio * cantidad;

                modelo.addRow(new Object[]{
                        nombre,
                        String.format("$%.2f", precio),
                        cantidad,
                        String.format("$%.2f", totalLinea)
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error cargando detalle: " + e.getMessage());
        }
    }
}