package com.meinunternehmen.invoicemaker;

import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
//import java.util.Arrays;
import java.util.List;

public class TypstGenerator {

	// zielordner 
  private static final String GOAL_FOLDER = "PDF's";

  // nimmt JSON (als String), kompiliert main.typ 
  public static void generateJson(String json) {
    try {
      Path projectRoot = Paths.get("").toAbsolutePath(); // ""= arbeitsverzeichnis + absolut robuster
      
      // build ordner erstellen 
      Path buildDir    = projectRoot.resolve("build");
      Files.createDirectories(buildDir);

      // JSON in build ordner schreiben (damit main.typ dort findet)
      Path dataJson = buildDir.resolve("data.json");
      Files.writeString(dataJson, json, StandardCharsets.UTF_8);

      // templates nach build kopieren aus resources
      copyResourceTo("main.typ", buildDir.resolve("main.typ"));
      copyResourceTo("invoice-maker.typ", buildDir.resolve("invoice-maker.typ"));

      // dateinamen bauen + Zielordner
      // liest Daten aus JSON
      JSONObject root = new JSONObject(json);
      String invoiceId     = root.optString("invoice-id", "");
      String recipientName = root.optJSONObject("recipient") != null
          ? root.getJSONObject("recipient").optString("name", "")
          : "";

      // falls keine Re --> datum nehmen 
      String base = (invoiceId == null || invoiceId.isBlank())
          ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
          : invoiceId;
      String fileName = "Rechnung-" + safeFilename(base) + "-" + safeFilename(recipientName) + ".pdf";

      // hier landet PDF
      Path outPath  = projectRoot.resolve(GOAL_FOLDER);
      Files.createDirectories(outPath);
      Path pdfPath = outPath.resolve(fileName);

      // kompilieren
      compileTypst(buildDir, pdfPath);

      System.out.println("[Typst] OK → " + pdfPath.toAbsolutePath());
    } catch (Exception ex) {
      throw new RuntimeException("PDF erzeugen fehlgeschlagen", ex);
    }
  }

  

  // 
  private static void compileTypst(Path buildDir, Path pdfOutput) throws IOException, InterruptedException {
   
	    List<String> cmd = List.of(
	        "typst", "compile",
	        "main.typ",       // input
	        pdfOutput.toAbsolutePath().toString()  // ziel absoluter pfad 
	    );

	    // Java-API, porzess starten 
	    ProcessBuilder pb = new ProcessBuilder(cmd);
	    pb.directory(buildDir.toFile()); // abreitsverzeichnis = build
	    pb.redirectErrorStream(true);  // einen stream haben für alle meldungen 

	    // starten
	    Process p = pb.start();
	    
	    // ausgaben lesen
	    StringBuilder log = new StringBuilder();
	    try (BufferedReader r = new BufferedReader(
	            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
	        String line;
	        while ((line = r.readLine()) != null) log.append(line).append(System.lineSeparator());
	    }
	    // auf ende warten 
	    int code = p.waitFor();
	    
	    // 0= erfolg 
	    if (code != 0) throw new RuntimeException("Typst-Compile fehlgeschlagen (Exit " + code + ")\n" + log);
	
  }

  // entfernt problematische zeichen 
  private static String safeFilename(String s) {
    String base = (s == null || s.isBlank()) ? "Unbenannt" : s.trim();
    // nur Buchstaben/Ziffern/._- zulassen --> Rest durch _
    return base.replaceAll("[^a-zA-Z0-9._-]+", "_");
   
  }

  // schreibt main + invoicemaker nach build 
  private static void copyResourceTo(String resourceName, Path target) throws IOException {
    try (InputStream in = TypstGenerator.class.getResourceAsStream("/" + resourceName)) {
      if (in == null) throw new FileNotFoundException("Resource not found: " + resourceName);
      Files.createDirectories(target.getParent());
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  
 
  
}

