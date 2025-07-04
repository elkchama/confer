package com.example.confer.service;

import com.example.confer.model.Usuario;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ReportePDFService {

    public ByteArrayInputStream generarReporteUsuarios(List<Usuario> usuarios) {
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();

            // 👉 Logo
            Image logo = Image.getInstance("src/main/resources/static/img/logo.png");
            logo.scaleToFit(100, 100);
            logo.setAlignment(Image.ALIGN_CENTER);
            documento.add(logo);

            // 👉 Título
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph encabezado = new Paragraph("Reporte de Usuarios", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            documento.add(encabezado);

            // 👉 Fecha actual
            Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph fecha = new Paragraph("Generado el: " + fechaActual, fechaFont);
            fecha.setAlignment(Element.ALIGN_CENTER);
            documento.add(fecha);

            documento.add(Chunk.NEWLINE);

            // Agrupar por roles
            agregarTablaPorRol(documento, usuarios, 1, "Admin");
            agregarTablaPorRol(documento, usuarios, 2, "Usuario");
            agregarTablaPorRol(documento, usuarios, 3, "Vendedor");

            documento.add(Chunk.NEWLINE);

            // 👉 Código QR
            Paragraph textoQR = new Paragraph("Código QR de verificación:", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            textoQR.setSpacingBefore(20f);
            documento.add(textoQR);

            Image qr = generarQR("Reporte generado por Confer - " + fechaActual);
            qr.scaleToFit(100, 100);
            documento.add(qr);

            documento.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void agregarTablaPorRol(Document doc, List<Usuario> usuarios, int rol, String tituloRol) throws DocumentException {
        List<Usuario> filtrados = usuarios.stream()
                .filter(u -> u.getIdRol() != null && u.getIdRol() == rol)
                .toList();

        if (filtrados.isEmpty()) return;

        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Paragraph p = new Paragraph("Usuarios - " + tituloRol, subtitulo);
        p.setSpacingBefore(15f);
        doc.add(p);

        PdfPTable tabla;
        if (rol == 3) {
            tabla = new PdfPTable(7); // columnas específicas para vendedor
            tabla.setWidths(new float[]{2, 4, 3, 2.5f, 3, 3.5f, 2});
        } else {
            tabla = new PdfPTable(4);
            tabla.setWidths(new float[]{2, 4, 3, 2});
        }
        tabla.setWidthPercentage(110);

        // Encabezados
        if (rol == 3) {
            Stream.of("Nombre", "Correo", "Teléfono", "Empresa", "NIT", "Dirección", "Rol")
                    .forEach(h -> agregarCeldaEncabezado(tabla, h));
        } else {
            Stream.of("Nombre", "Correo", "Teléfono", "Rol")
                    .forEach(h -> agregarCeldaEncabezado(tabla, h));
        }

        for (Usuario u : filtrados) {
            tabla.addCell(u.getNombre());
            tabla.addCell(u.getCorreo());
            tabla.addCell(u.getTelefono() != null ? u.getTelefono() : "");
            if (rol == 3) {
                tabla.addCell(u.getEmpresa() != null ? u.getEmpresa() : "");
                tabla.addCell(u.getNit() != null ? u.getNit() : "");
                tabla.addCell(u.getDireccion() != null ? u.getDireccion() : "");
            }
            tabla.addCell(tituloRol); // Rol explícito
        }

        doc.add(tabla);

        Paragraph total = new Paragraph("Total " + tituloRol + "s: " + filtrados.size(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
        total.setSpacingAfter(10f);
        doc.add(total);
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
