package com.example.confer.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.example.confer.model.Producto;
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
public class ReporteProductoPDFService {

    public ByteArrayInputStream generarReporteProductos(List<Producto> productos) {
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

            // Título
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph encabezado = new Paragraph("Reporte de Productos", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            // Fecha
            Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph fecha = new Paragraph("Generado el: " + fechaActual, fechaFont);
            fecha.setAlignment(Element.ALIGN_LEFT);
            documento.add(fecha);

            documento.add(Chunk.NEWLINE);

            // Tabla
            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{3, 4, 6, 3, 3});

            agregarCeldaEncabezado(tabla, "Vendedor");
            agregarCeldaEncabezado(tabla, "Nombre");
            agregarCeldaEncabezado(tabla, "Descripción");
            agregarCeldaEncabezado(tabla, "Precio");
            agregarCeldaEncabezado(tabla, "Imagen");

            for (Producto p : productos) {
                tabla.addCell(p.getVendedor() != null ? p.getVendedor().getNombre() : "N/A");
                tabla.addCell(p.getNombre());
                tabla.addCell(p.getDescripcion());
                tabla.addCell("$" + p.getPrecio().toString());

                if (p.getImagenUrl() != null) {
                    String ruta = "uploads/" + p.getImagenUrl();
                    try {
                        Image imagen = Image.getInstance(ruta);
                        imagen.scaleToFit(60, 60);
                        PdfPCell celdaImg = new PdfPCell(imagen, true);
                        tabla.addCell(celdaImg);
                    } catch (Exception ex) {
                        tabla.addCell("Error img");
                    }
                } else {
                    tabla.addCell("Sin imagen");
                }
            }

            documento.add(tabla);

            // QR
            documento.add(Chunk.NEWLINE);
            Paragraph textoQR = new Paragraph("Código QR de verificación:", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            textoQR.setSpacingBefore(20f);
            documento.add(textoQR);

            Image qr = generarQR("Reporte de productos generado por Confer - " + fechaActual);
            qr.scaleToFit(100, 100);
            documento.add(qr);

            documento.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
        tabla.addCell(celda);
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
