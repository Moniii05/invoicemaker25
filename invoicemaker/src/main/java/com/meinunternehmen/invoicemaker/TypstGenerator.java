package com.meinunternehmen.invoicemaker;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TypstGenerator {
    /**
     * Kompiliert aus einer .typ–Ressource eine PDF.
     *
     * @param templateResource  Name der .typ–Datei in src/main/resources (z.B. "main.typ")
     * @param data              Map mit Template-Variablen
     * @param outputPdf         Pfad zur Ausgabedatei (z.B. Path.of("invoice.pdf"))
     */
    public static void generate(String templateResource,
                                Map<String, Object> data,
                                Path outputPdf) throws Exception {
        // 1) Build-Ordner anlegen
        Path buildDir = Path.of("build");
        Files.createDirectories(buildDir);

        // 2) Haupt-Template einlesen
        String tpl;
        try (InputStream in = TypstGenerator.class.getResourceAsStream("/" + templateResource)) {
            if (in == null) throw new RuntimeException("Resource nicht gefunden: /" + templateResource);
            tpl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 3) invoice-maker.typ & banner.png kopieren
        try (InputStream in = TypstGenerator.class.getResourceAsStream("/invoice-maker.typ")) {
            if (in == null) throw new RuntimeException("Resource nicht gefunden: /invoice-maker.typ");
            Files.copy(in, buildDir.resolve("invoice-maker.typ"), StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream in = TypstGenerator.class.getResourceAsStream("/banner.png")) {
            if (in != null) {
                Files.copy(in, buildDir.resolve("banner.png"), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // 4) Daten-Map in Typst-Literal umwandeln
        String dataLiteral = toTypstLiteral(data);

        // 5) Generiertes Typst-File schreiben
        Path gen = buildDir.resolve("generated_main.typ");
        String combined = "#let data = " + dataLiteral + "\n\n" + tpl;
        Files.writeString(gen, combined, StandardCharsets.UTF_8);

        // Debug
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        System.out.println("Compile-Template: " + gen);
        System.out.println("Output-PDF:       " + outputPdf.toAbsolutePath());

        // 6) Typst im Build-Ordner ausführen
        Process p = new ProcessBuilder(
               // "/opt/homebrew/bin/typst",  // oder einfach "typst" im PATH
        		// "typst", 
        		"/Users/monikapham/.cargo/bin/typst",
        		"compile",
                gen.getFileName().toString(),
                outputPdf.toString()
            )
            .directory(buildDir.toFile())
            .inheritIO()
            .start();

        if (p.waitFor() != 0) {
            throw new RuntimeException("Typst-Compile fehlgeschlagen mit Code " + p.exitValue());
        }

        // 7) PDF öffnen
        File pdfFile = outputPdf.toFile();
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(pdfFile);
        }
    }

   
    private static String toTypstLiteral(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            String entries = m.entrySet().stream()
                .map(e -> {
                    String key = e.getKey().toString();
                    // wenn der Key ein einfacher Identifier ist, unquoted,
                    // sonst backtick-quoted
                    String keyLit = key.matches("[A-Za-z_][A-Za-z0-9_]*")
                            ? key
                            : "\"" + key + "\"";
                    return keyLit + ": " + toTypstLiteral(e.getValue());
                })
                .collect(Collectors.joining(", "));
            return "( " + entries + " )";
        }
        if (obj instanceof List<?> list) {
            String items = list.stream()
                .map(TypstGenerator::toTypstLiteral)
                .collect(Collectors.joining(", "));
            return "[ " + items + " ]";
        }
        if (obj instanceof String s) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        // für Zahlen/Booleans:
        return obj.toString();
    }
}

