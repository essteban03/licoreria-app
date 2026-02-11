package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;

public class MenuPrincipalFrame extends JFrame {
    
    // ==== DATOS DE SESIÓN ====
    private final String usuario;
    private final String rol;
    private final List<String> permisos;

    public MenuPrincipalFrame(String usuario, String rol, List<String> permisos) {
        this.usuario = usuario;
        this.rol = rol;
        // Si la lista es null, la inicializamos vacía para evitar errores
        this.permisos = (permisos != null) ? permisos : new ArrayList<>();
        
        initUI();
    }

    private void initUI() {
        setTitle("Panel Principal - Inkia Vinos y Licores");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Al cerrar el menú, se cierra la app
        setLayout(new BorderLayout());

        // Colores corporativos locales (o puedes usar EstilosApp)
        Color vino   = new Color(60, 0, 0);
        Color dorado = new Color(212, 175, 55);
        Color fondo  = new Color(245, 245, 245);

        // ==========================================
        // 1. ENCABEZADO PERSONALIZADO
        // ==========================================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(vino);
        topPanel.setPreferredSize(new Dimension(0, 80));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Info Usuario (Izquierda)
        JPanel pInfo = new JPanel(new GridLayout(2, 1));
        pInfo.setOpaque(false);
        
        JLabel lblUser = new JLabel("👤 " + usuario);
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JLabel lblRol = new JLabel("Rol: " + rol.toUpperCase());
        lblRol.setForeground(new Color(220, 220, 220));
        lblRol.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        pInfo.add(lblUser);
        pInfo.add(lblRol);

        // Título Central
        JLabel lblTitulo = new JLabel("PANEL DE CONTROL", JLabel.CENTER);
        lblTitulo.setForeground(dorado);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));

        // Botón Salir (Derecha)
        JButton btnSalir = new JButton("Cerrar Sesión");
        btnSalir.setBackground(dorado);
        btnSalir.setForeground(Color.BLACK);
        btnSalir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSalir.setFocusPainted(false);
        btnSalir.addActionListener(e -> cerrarSesion());

        topPanel.add(pInfo, BorderLayout.WEST);
        topPanel.add(lblTitulo, BorderLayout.CENTER);
        topPanel.add(btnSalir, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. GRID DE MÓDULOS (CENTRO)
        // ==========================================
        JPanel gridPanel = new JPanel(new GridBagLayout()); // Usamos GridBag para centrar los botones
        gridPanel.setBackground(fondo);
        
        JPanel containerBotones = new JPanel(new GridLayout(0, 3, 20, 20)); // 3 columnas, filas dinámicas
        containerBotones.setOpaque(false);

        // --- GENERACIÓN DINÁMICA DE BOTONES SEGÚN PERMISOS ---

        // 1. Catálogo (Clientes)
        if (tienePermiso("CATALOGO_CLIENTE") || esRol("cliente") || esRol("admin")) {
            containerBotones.add(crearTarjetaModulo("Catálogo", "🛒", "Comprar productos", e -> {
                navegarA(new ClienteFrame(usuario, rol, permisos));
            }));
        }

        // 2. Bodega (Bodegueros)
        if (tienePermiso("BODEGA") || esRol("bodeguero") || esRol("admin")) {
            containerBotones.add(crearTarjetaModulo("Bodega", "📦", "Recepción de mercadería", e -> {
                navegarA(new BodegaFrame(usuario, rol, permisos));
            }));
        }

        // 3. Gestión Productos (Admin / Gestor)
        if (tienePermiso("GESTION_PRODUCTOS") || esRol("gestor") || esRol("admin")) {
            containerBotones.add(crearTarjetaModulo("Inventario", "🍾", "Gestionar productos", e -> {
                navegarA(new AdminFrame(usuario, rol, permisos));
            }));
        }

        // 4. Gestión Pedidos (Gestor)
        if (tienePermiso("GESTION_PEDIDOS") || esRol("gestor") || esRol("admin")) {
            containerBotones.add(crearTarjetaModulo("Pedidos", "📑", "Procesar ventas", e -> {
                navegarA(new GestorStockFrame(usuario, rol, permisos));
            }));
        }

        // 5. Gestión Usuarios (Admin)
        if (tienePermiso("GESTION_USUARIOS") || esRol("admin")) {
            containerBotones.add(crearTarjetaModulo("Usuarios", "👥", "Control de personal", e -> {
                navegarA(new ListaUsuariosFrame(usuario, rol, permisos));
            }));
        }

        // 6. Gestión Roles (Admin)
        if (tienePermiso("GESTION_ROLES") || esRol("admin")) {
            containerBotones.add(crearTarjetaModulo("Roles", "🔐", "Permisos de acceso", e -> {
                navegarA(new GestionRolesFrame(usuario, rol, permisos));
            }));
        }

        // Si no hay módulos (caso raro)
        if (containerBotones.getComponentCount() == 0) {
            JLabel lblVacio = new JLabel("No tienes accesos asignados.");
            lblVacio.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            gridPanel.add(lblVacio);
        } else {
            gridPanel.add(containerBotones);
        }

        add(gridPanel, BorderLayout.CENTER);
        
        // ==========================================
        // 3. FOOTER (ESTADO)
        // ==========================================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        JLabel lblVer = new JLabel("Sistema Licorería v2.0  ");
        lblVer.setForeground(Color.GRAY);
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footer.add(lblVer);
        add(footer, BorderLayout.SOUTH);
    }

    // ==== LÓGICA AUXILIAR ====

    private boolean tienePermiso(String permisoRequerido) {
        // Si es admin, tiene acceso a todo por defecto (opcional)
        if (rol.equalsIgnoreCase("admin")) return true;
        return permisos.contains(permisoRequerido);
    }
    
    private boolean esRol(String rolRequerido) {
        return rol.equalsIgnoreCase(rolRequerido);
    }

    private void navegarA(JFrame ventanaDestino) {
        this.dispose(); // Cerramos el menú
        ventanaDestino.setVisible(true); // Abrimos el módulo
    }

    private void cerrarSesion() {
        int r = JOptionPane.showConfirmDialog(this, "¿Cerrar sesión?", "Salir", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            dispose();
            new Login().setVisible(true);
        }
    }

    // ==== DISEÑO DE BOTONES (TARJETAS) ====
    private JButton crearTarjetaModulo(String titulo, String icono, String subtitulo, ActionListener accion) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout());
        btn.setBackground(Color.WHITE);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 120));
        
        // Icono grande
        JLabel lblIcono = new JLabel(icono, JLabel.CENTER);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        
        // Textos
        JPanel pTextos = new JPanel(new GridLayout(2, 1));
        pTextos.setOpaque(false);
        
        JLabel lblTit = new JLabel(titulo, JLabel.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTit.setForeground(new Color(60, 0, 0)); // Vino
        
        JLabel lblSub = new JLabel(subtitulo, JLabel.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(Color.GRAY);
        
        pTextos.add(lblTit);
        pTextos.add(lblSub);
        
        btn.add(lblIcono, BorderLayout.CENTER);
        btn.add(pTextos, BorderLayout.SOUTH);
        
        // Borde elegante
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Efecto Hover simple
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(250, 243, 224)); // Crema
                btn.setBorder(BorderFactory.createLineBorder(new Color(212, 175, 55), 2)); // Dorado
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(Color.WHITE);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }
        });

        btn.addActionListener(accion);
        return btn;
    }
}