/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package View;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;

import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.bson.Document;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
public class FacturaPDF {
    // ==========================================
    // MÉTODO PÚBLICO PARA GENERAR FACTURA
    // ==========================================
    public static void generar(Document pedido) {
        try {
            String numeroFactura = generarNumeroFactura();
            String ruta = System.getProperty("user.home") + "/Factura_" + numeroFactura + ".pdf";

            com.itextpdf.text.Document pdf = new com.itextpdf.text.Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(pdf, new FileOutputStream(ruta));
            pdf.open();

            com.itextpdf.text.Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.itextpdf.text.Font fSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font fTexto = FontFactory.getFont(FontFactory.HELVETICA, 11);
            com.itextpdf.text.Font fNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            Paragraph empresa = new Paragraph("INKIA VINOS Y LICORES", fTitulo);
            empresa.setAlignment(Element.ALIGN_CENTER);

            Paragraph datosEmpresa = new Paragraph(
                    "Dirección: Quito - Ecuador\nTel: 0999999999 | Email: ventas@inkia.com\n",
                    fTexto
            );
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);

            pdf.add(empresa);
            pdf.add(datosEmpresa);
            pdf.add(new Paragraph("------------------------------------------------------------"));

            // ==========================================
            // DATOS FACTURA (TABLA 2 COLUMNAS)
            // ==========================================
            PdfPTable tablaFactura = new PdfPTable(2);
            tablaFactura.setWidthPercentage(100);
            tablaFactura.setSpacingBefore(10);
            tablaFactura.setSpacingAfter(10);
            tablaFactura.setWidths(new float[]{1.5f, 1.5f});

            String fecha = pedido.containsKey("fechaHora") ? pedido.getString("fechaHora") : "(sin fecha)";
            String cliente = pedido.containsKey("cliente") ? pedido.getString("cliente") : "(sin cliente)";
            String telefono = pedido.containsKey("telefono") ? pedido.getString("telefono") : "(sin teléfono)";
            String direccion = pedido.containsKey("direccion") ? pedido.getString("direccion") : "(sin dirección)";
            String metodoPago = pedido.containsKey("metodoPago") ? pedido.getString("metodoPago") : "(sin método)";
            String usuarioSistema = pedido.containsKey("usuario") ? pedido.getString("usuario") : "(no registrado)";

            tablaFactura.addCell(celdaSinBorde("Factura N°: " + numeroFactura, fNegrita));
            tablaFactura.addCell(celdaSinBorde("Fecha: " + fecha, fTexto));

            tablaFactura.addCell(celdaSinBorde("Cliente: " + cliente, fTexto));
            tablaFactura.addCell(celdaSinBorde("Teléfono: " + telefono, fTexto));

            tablaFactura.addCell(celdaSinBorde("Dirección: " + direccion, fTexto));
            tablaFactura.addCell(celdaSinBorde("Método Pago: " + metodoPago, fTexto));

            tablaFactura.addCell(celdaSinBorde("Usuario sistema: " + usuarioSistema, fTexto));
            tablaFactura.addCell(celdaSinBorde("", fTexto));

            if (pedido.containsKey("tarjetaUltimos4")) {
                tablaFactura.addCell(celdaSinBorde("Tarjeta: **** " + pedido.getString("tarjetaUltimos4"), fTexto));
                tablaFactura.addCell(celdaSinBorde("", fTexto));
            }

            if (pedido.containsKey("cupon")) {
                String cupon = pedido.getString("cupon");
                if (cupon != null && !cupon.trim().isEmpty()) {
                    tablaFactura.addCell(celdaSinBorde("Cupón aplicado: " + cupon, fTexto));
                    tablaFactura.addCell(celdaSinBorde("", fTexto));
                }
            }

            pdf.add(tablaFactura);

            // ==========================================
            // TABLA PRODUCTOS
            // ==========================================
            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(10);
            tabla.setSpacingAfter(10);
            tabla.setWidths(new float[]{3.0f, 1.2f, 1.0f, 1.2f, 1.4f});

            tabla.addCell(celdaHeader("Producto", fSub));
            tabla.addCell(celdaHeader("Precio", fSub));
            tabla.addCell(celdaHeader("Cant.", fSub));
            tabla.addCell(celdaHeader("IVA", fSub));
            tabla.addCell(celdaHeader("Total", fSub));

            ArrayList<Document> items = (ArrayList<Document>) pedido.get("productos");

            if (items != null) {
                for (Document item : items) {
                    String nombre = item.containsKey("nombre") ? item.getString("nombre") : "Producto";
                    double precio = item.getDouble("precio") != null ? item.getDouble("precio") : 0.0;
                    int cantidad = item.getInteger("cantidad", 0);

                    double totalLinea = precio * cantidad;
                    double ivaLinea = totalLinea * 0.15;

                    tabla.addCell(celdaNormal(nombre, fTexto));
                    tabla.addCell(celdaNormal(String.format("$%.2f", precio), fTexto));
                    tabla.addCell(celdaNormal(String.valueOf(cantidad), fTexto));
                    tabla.addCell(celdaNormal(String.format("$%.2f", ivaLinea), fTexto));
                    tabla.addCell(celdaNormal(String.format("$%.2f", totalLinea + ivaLinea), fTexto));
                }
            }

            pdf.add(tabla);

            // ==========================================
            // TOTALES
            // ==========================================
            PdfPTable totales = new PdfPTable(2);
            totales.setWidthPercentage(45);
            totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totales.setSpacingBefore(10);
            totales.setWidths(new float[]{2.0f, 1.0f});

            double subtotal = pedido.getDouble("subtotal") != null ? pedido.getDouble("subtotal") : 0.0;
            double iva = pedido.getDouble("iva") != null ? pedido.getDouble("iva") : 0.0;
            double descuento = pedido.getDouble("descuento") != null ? pedido.getDouble("descuento") : 0.0;
            double total = pedido.getDouble("total") != null ? pedido.getDouble("total") : (subtotal + iva - descuento);

            totales.addCell(celdaTotalLabel("Subtotal:", fTexto));
            totales.addCell(celdaTotalValor(String.format("$%.2f", subtotal), fTexto));

            totales.addCell(celdaTotalLabel("IVA:", fTexto));
            totales.addCell(celdaTotalValor(String.format("$%.2f", iva), fTexto));

            totales.addCell(celdaTotalLabel("Descuento:", fTexto));
            totales.addCell(celdaTotalValor(String.format("-$%.2f", descuento), fTexto));

            totales.addCell(celdaTotalLabel("TOTAL:", fNegrita));
            totales.addCell(celdaTotalValor(String.format("$%.2f", total), fNegrita));

            pdf.add(totales);

            pdf.add(new Paragraph("\n"));
            Paragraph gracias = new Paragraph("Gracias por su compra. ¡Vuelva pronto!", fNegrita);
            gracias.setAlignment(Element.ALIGN_CENTER);
            pdf.add(gracias);

            pdf.close();

            // Abrir automáticamente (opcional)
            try {
                Desktop.getDesktop().open(new java.io.File(ruta));
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.out.println("Error PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // NUMERO FACTURA
    // ==========================================
    private static String generarNumeroFactura() {
        String anio = String.valueOf(LocalDateTime.now().getYear());
        String random = String.valueOf(System.currentTimeMillis());
        String ultimos = random.substring(random.length() - 6);
        return anio + "-" + ultimos;
    }

    // ==========================================
    // HELPERS PDF
    // ==========================================
    private static PdfPCell celdaHeader(String texto, com.itextpdf.text.Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(8);
        c.setBackgroundColor(new BaseColor(230, 230, 230));
        return c;
    }

    private static PdfPCell celdaNormal(String texto, com.itextpdf.text.Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setPadding(7);
        return c;
    }

    private static PdfPCell celdaSinBorde(String texto, com.itextpdf.text.Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
    }

    private static PdfPCell celdaTotalLabel(String texto, com.itextpdf.text.Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(5);
        return c;
    }

    private static PdfPCell celdaTotalValor(String texto, com.itextpdf.text.Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(5);
        return c;
    }
}
