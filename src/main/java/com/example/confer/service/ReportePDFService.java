package com.example.confer.service;

import com.example.confer.model.Usuario;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ReportePDFService {

    public ByteArrayInputStream generarReporteUsuarios(List<Usuario> usuarios) {
        Document documento = new Document();
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

            // 👉 Tabla de usuarios
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new int[]{2, 4, 3, 2});

            // Encabezados
            Stream.of("Nombre", "Correo", "Teléfono", "Rol").forEach(header -> {
                PdfPCell celda = new PdfPCell();
                celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
                celda.setPhrase(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                tabla.addCell(celda);
            });

            for (Usuario usuario : usuarios) {
                tabla.addCell(usuario.getNombre());
                tabla.addCell(usuario.getCorreo());
                tabla.addCell(usuario.getTelefono() != null ? usuario.getTelefono() : "");
                tabla.addCell(
                        usuario.getIdRol() == 1 ? "Admin" :
                        usuario.getIdRol() == 2 ? "Usuario" : "Vendedor"
                );
            }

            documento.add(tabla);
            documento.close();

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
