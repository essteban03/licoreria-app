/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package View;

import javax.swing.*;
import java.awt.*;

public class MenuGestorFrame extends JFrame{
    private final String usuarioActual;

    public MenuGestorFrame(String usuarioActual) {
        this.usuarioActual = (usuarioActual != null && !usuarioActual.isEmpty())
                ? usuarioActual
                : "gestor";

        setTitle("Panel del Gestor de Productos y Pedidos");
        setSize(500, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        Color vinoOscuro = new Color(60, 0, 0);
        Color crema = new Color(250, 243, 224);
        Color dorado = new Color(212, 175, 55);

        // ===== BARRA SUPERIOR =====
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(vinoOscuro);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblTitulo = new JLabel("Panel del Gestor", JLabel.CENTER);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel lblUser = new JLabel("Gestor: " + this.usuarioActual);
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.setBackground(dorado);
        btnCerrarSesion.setForeground(Color.BLACK);
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCerrarSesion.addActionListener(e -> {
            dispose();
            new Login().setVisible(true);
        });

        barra.add(lblUser, BorderLayout.WEST);
        barra.add(lblTitulo, BorderLayout.CENTER);
        barra.add(btnCerrarSesion, BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);

        // ===== PANEL CENTRAL (2 OPCIONES) =====
        JPanel centro = new JPanel();
        centro.setBackground(crema);
        centro.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        centro.setLayout(new GridLayout(2, 1, 15, 15));

        // Botón para gestionar pedidos
        JButton btnPedidos = new JButton("Gestionar pedidos");
        btnPedidos.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnPedidos.setBackground(dorado);
        btnPedidos.setForeground(Color.BLACK);
        btnPedidos.setFocusPainted(false);
        btnPedidos.addActionListener(e -> {
        dispose();
        new GestorStockFrame(usuarioActual).setVisible(true);
        });

        // Botón para ir al panel de productos (AdminFrame)
        JButton btnInventario = new JButton("Actualizar inventario (productos)");
        btnInventario.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnInventario.setBackground(dorado);
        btnInventario.setForeground(Color.BLACK);
        btnInventario.setFocusPainted(false);
        btnInventario.addActionListener(e -> {
        dispose();
   
        new AdminFrame("gestor_demo", "gestor", null).setVisible(true);
        });

        centro.add(btnPedidos);
        centro.add(btnInventario);

        add(centro, BorderLayout.CENTER);
    }

    // Para probar solito si quieres
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuGestorFrame("gestor_demo").setVisible(true));
    }
}
