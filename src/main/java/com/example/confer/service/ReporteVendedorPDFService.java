package com.example.confer.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.example.confer.model.Producto;
import com.example.confer.model.Usuario;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class ReporteVendedorPDFService {

    public ByteArrayInputStream generarReporteVendedor(Usuario vendedor, List<Producto> productos) {
        Document documento = new Document(PageSize.A4.rotate(), 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();

            // Logo
            try {
                Image logo = Image.getInstance("src/main/resources/static/img/logo.png");
                logo.scaleToFit(100, 100);
                logo.setAlignment(Image.ALIGN_CENTER);
                documento.add(logo);
            } catch (Exception e) {
                System.out.println("Advertencia: No se pudo cargar el logo");
            }

            // Título principal
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph encabezado = new Paragraph("Reporte de Productos - Vendedor", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            documento.add(new Paragraph(" "));

            // Información del vendedor
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.DARK_GRAY);
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph infoVendedor = new Paragraph();
            infoVendedor.add(new Chunk("Vendedor: ", labelFont));
            infoVendedor.add(new Chunk(vendedor.getNombre(), dataFont));
            documento.add(infoVendedor);

            Paragraph infoEmail = new Paragraph();
            infoEmail.add(new Chunk("Correo: ", labelFont));
            infoEmail.add(new Chunk(vendedor.getCorreo(), dataFont));
            documento.add(infoEmail);

            // Fecha de generación
            Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            Paragraph fecha = new Paragraph("Generado el: " + fechaActual, fechaFont);
            documento.add(fecha);

            documento.add(new Paragraph(" "));

            // Resumen
            Font resumenFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY);
            Paragraph resumen = new Paragraph("Resumen", resumenFont);
            documento.add(resumen);

            PdfPTable tablaResumen = new PdfPTable(2);
            tablaResumen.setWidthPercentage(50);

            agregarCeldaResumen(tablaResumen, "Total de productos:", String.valueOf(productos.size()));
            
            BigDecimal precioTotal = productos.stream()
                .map(Producto::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            agregarCeldaResumen(tablaResumen, "Valor total inventario:", "$" + precioTotal.toString());

            long productosDestacados = productos.stream()
                .filter(Producto::isDestacado)
                .count();
            agregarCeldaResumen(tablaResumen, "Productos destacados:", String.valueOf(productosDestacados));

            BigDecimal valorDestacado = productos.stream()
                .filter(Producto::isDestacado)
                .map(Producto::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            agregarCeldaResumen(tablaResumen, "Valor productos destacados:", "$" + valorDestacado.toString());

            documento.add(tablaResumen);
            documento.add(new Paragraph(" "));

            // Tabla detallada de productos
            if (productos.size() > 0) {
                Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY);
                Paragraph subtitulo = new Paragraph("Detalle de Productos", subtituloFont);
                documento.add(subtitulo);
                documento.add(new Paragraph(" "));

                PdfPTable tabla = new PdfPTable(7);
                tabla.setWidthPercentage(100);
                tabla.setWidths(new float[]{1.5f, 3, 4, 2, 2, 1.5f, 1.5f});

                agregarCeldaEncabezado(tabla, "ID");
                agregarCeldaEncabezado(tabla, "Nombre");
                agregarCeldaEncabezado(tabla, "Descripción");
                agregarCeldaEncabezado(tabla, "Categoría");
                agregarCeldaEncabezado(tabla, "Precio");
                agregarCeldaEncabezado(tabla, "Destacado");
                agregarCeldaEncabezado(tabla, "Descuento");

                for (Producto p : productos) {
                    tabla.addCell(p.getId().toString());
                    tabla.addCell(p.getNombre());
                    tabla.addCell(p.getDescripcion() != null ? p.getDescripcion() : "-");
                    tabla.addCell(p.getCategoria() != null ? p.getCategoria() : "-");
                    tabla.addCell("$" + p.getPrecio().toString());
                    tabla.addCell(p.isDestacado() ? "Sí" : "No");
                    
                    if (p.getPorcentajeDescuento() != null && p.getPorcentajeDescuento().compareTo(BigDecimal.ZERO) > 0) {
                        tabla.addCell(p.getPorcentajeDescuento().toString() + "%");
                    } else {
                        tabla.addCell("-");
                    }
                }

                documento.add(tabla);
            } else {
                Paragraph sinProductos = new Paragraph("No hay productos registrados para este vendedor.", 
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, BaseColor.GRAY));
                documento.add(sinProductos);
            }

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(" "));

            // Código QR de verificación
            Font textoQRFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY);
            Paragraph textoQR = new Paragraph("Código QR de verificación:", textoQRFont);
            documento.add(textoQR);

            String datosQR = "Reporte Vendedor: " + vendedor.getNombre() + " | " + 
                           "Productos: " + productos.size() + " | " + fechaActual;
            Image qr = generarQR(datosQR);
            qr.scaleToFit(80, 80);
            qr.setAlignment(Image.ALIGN_CENTER);
            documento.add(qr);

            documento.add(new Paragraph(" "));
            Paragraph pie = new Paragraph("Este documento fue generado automáticamente por el sistema Confer", 
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY));
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);

            documento.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE)));
        celda.setBackgroundColor(new BaseColor(102, 126, 234)); // Color morado/azul
        celda.setPadding(8);
        tabla.addCell(celda);
    }

    private void agregarCeldaResumen(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        celdaEtiqueta.setBackgroundColor(new BaseColor(240, 240, 240));
        celdaEtiqueta.setPadding(8);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, 
            FontFactory.getFont(FontFactory.HELVETICA, 11)));
        celdaValor.setPadding(8);
        tabla.addCell(celdaValor);
    }

    private Image generarQR(String texto) throws WriterException, IOException, BadElementException {
        int size = 150;
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(texto, BarcodeFormat.QR_CODE, size, size);

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int grayValue = bitMatrix.get(x, y) ? 0 : 255;
                img.setRGB(x, y, (grayValue == 0 ? java.awt.Color.BLACK : java.awt.Color.WHITE).getRGB());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Image.getInstance(baos.toByteArray());
    }
}