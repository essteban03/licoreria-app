package View;

import org.bson.Document;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class CarritoFrame extends JFrame {

    private final ArrayList<Document> carrito;
    private final String usuarioActual;

    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblSubtotal, lblIva, lblTotal;
    private JTextField txtCupon;

    private double subtotal = 0.0;
    private double descuento = 0.0;
    private double iva = 0.0;
    private double total = 0.0;

    // NUEVO: guardamos el cupón aplicado (para guardarlo en el pedido)
    private String cuponAplicado = "";

    public CarritoFrame(ArrayList<Document> carrito, String usuario) {
        this.carrito = carrito;
        this.usuarioActual = usuario;

        initUI();
        calcularTotales();
    }

    private void initUI() {
        setTitle("Carrito de Compras");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        add(EstilosApp.crearHeader("Mi Carrito", usuarioActual, e -> dispose()), BorderLayout.NORTH);

        // TABLA
        String[] col = {"Producto", "Precio Unit.", "Cant.", "Total"};
        modelo = new DefaultTableModel(col, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        for (Document d : carrito) {
            double p = d.getDouble("precio");
            int c = d.getInteger("cantidad");
            modelo.addRow(new Object[]{ d.getString("nombre"), p, c, p * c });
        }

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // PANEL INFERIOR
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setBackground(new Color(250, 248, 245));
        panelSur.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Izquierda
        JPanel pIzq = new JPanel(new GridLayout(3, 1, 5, 5));
        pIzq.setOpaque(false);

        JButton btnEliminar = new JButton("Eliminar Item Seleccionado");
        btnEliminar.setBackground(new Color(255, 200, 200));
        btnEliminar.addActionListener(e -> eliminarItem());

        JPanel pCupon = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pCupon.setOpaque(false);

        txtCupon = new JTextField(10);
        JButton btnAplicar = new JButton("Aplicar Cupón");
        btnAplicar.setBackground(Color.LIGHT_GRAY);
        btnAplicar.addActionListener(e -> aplicarCupon());

        pCupon.add(new JLabel("Cupón:"));
        pCupon.add(txtCupon);
        pCupon.add(btnAplicar);

        pIzq.add(btnEliminar);
        pIzq.add(pCupon);

        // Derecha
        JPanel pDer = new JPanel(new GridLayout(4, 1, 5, 5));
        pDer.setOpaque(false);

        lblSubtotal = new JLabel("Subtotal: $0.00", SwingConstants.RIGHT);
        lblIva = new JLabel("IVA (15%): $0.00", SwingConstants.RIGHT);
        lblTotal = new JLabel("TOTAL: $0.00", SwingConstants.RIGHT);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton btnPagar = new JButton("PROCEDER AL PAGO");
        btnPagar.setBackground(new Color(212, 175, 55));
        btnPagar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPagar.setForeground(Color.BLACK);
        btnPagar.addActionListener(e -> irAPagar());

        pDer.add(lblSubtotal);
        pDer.add(lblIva);
        pDer.add(lblTotal);
        pDer.add(btnPagar);

        panelSur.add(pIzq, BorderLayout.WEST);
        panelSur.add(pDer, BorderLayout.EAST);

        add(panelSur, BorderLayout.SOUTH);
    }

    private void calcularTotales() {
        subtotal = 0;
        for (Document d : carrito) {
            subtotal += d.getDouble("precio") * d.getInteger("cantidad");
        }
        iva = subtotal * 0.15;
        total = subtotal + iva - descuento;
        if (total < 0) total = 0;

        lblSubtotal.setText(String.format("Subtotal: $%.2f", subtotal));
        lblIva.setText(String.format("IVA (15%%): $%.2f", iva));
        lblTotal.setText(String.format("TOTAL: $%.2f", total));
    }

    private void eliminarItem() {
        int row = tabla.getSelectedRow();
        if (row < 0) return;

        carrito.remove(row);
        modelo.removeRow(row);

        // Si eliminan algo, el cupón se invalida para evitar errores de lógica
        descuento = 0;
        cuponAplicado = "";
        txtCupon.setText("");

        calcularTotales();
    }

    private void aplicarCupon() {
        String c = txtCupon.getText().trim().toUpperCase();

        if (c.equals("LICOR10")) {
            descuento = subtotal * 0.10;
            cuponAplicado = c;
            JOptionPane.showMessageDialog(this, "¡Descuento 10% aplicado!");
        } else if (c.equals("VIP20")) {
            descuento = subtotal * 0.20;
            cuponAplicado = c;
            JOptionPane.showMessageDialog(this, "¡Descuento VIP 20% aplicado!");
        } else {
            descuento = 0;
            cuponAplicado = "";
            JOptionPane.showMessageDialog(this, "Cupón no válido");
        }

        calcularTotales();
    }

    private void irAPagar() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío");
            return;
        }

        // ✅ PASAMOS usuarioActual + cuponAplicado
        new PagoFrame(carrito, subtotal, descuento, iva, cuponAplicado, usuarioActual).setVisible(true);

        dispose();
    }
}