package com.meinunternehmen.invoicemaker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TypstGenerator {

  public static void generateJson(String json) {
    try {
      Path projectRoot = Paths.get("").toAbsolutePath();
      Path buildDir    = projectRoot.resolve("build");
      Files.createDirectories(buildDir);

      // 1) JSON neben main.typ in build/ schreiben
      Path dataJson = buildDir.resolve("data.json");
      try (BufferedWriter w = Files.newBufferedWriter(dataJson, StandardCharsets.UTF_8)) {
        w.write(json);
      }

      // 2) main.typ aus resources nach build/ kopieren
      copyResourceTo("main.typ", buildDir.resolve("main.typ"));
      
      copyOptionalResourceTo("invoice-maker.typ", buildDir.resolve("invoice-maker.typ"));


      // 3) Typst aufrufen: Arbeitsverzeichnis = build/
      String typstCmd = findTypstBinary();
      List<String> cmd = List.of(
          typstCmd, "compile",
          "main.typ",                                  // Template relativ zu build/
          projectRoot.resolve("invoice.pdf").toString()// Ausgabe absolut
      );
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.directory(buildDir.toFile());                 // <- wichtig!
      pb.redirectErrorStream(true);

      Process p = pb.start();
      StringBuilder log = new StringBuilder();
      try (BufferedReader r = new BufferedReader(
              new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = r.readLine()) != null) log.append(line).append(System.lineSeparator());
      }
      int code = p.waitFor();
      if (code != 0) throw new RuntimeException("Typst-Compile fehlgeschlagen (Exit " + code + ")\n" + log);

      System.out.println("[Typst] OK → " + projectRoot.resolve("invoice.pdf"));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void copyResourceTo(String resourceName, Path target) throws IOException {
    try (InputStream in = TypstGenerator.class.getResourceAsStream("/" + resourceName)) {
      if (in == null) throw new FileNotFoundException("Resource not found: " + resourceName);
      Files.createDirectories(target.getParent());
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
  
  
  private static void copyOptionalResourceTo(String resourceName, Path target) {
	    try (InputStream in = TypstGenerator.class.getResourceAsStream("/" + resourceName)) {
	      if (in == null) return; // optional
	      Files.createDirectories(target.getParent());
	      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
	    } catch (IOException e) {
	      System.err.println("[Typst] Hinweis: Optionale Resource nicht kopiert: " + resourceName + " → " + e.getMessage());
	    }
	  }

  // wie gehabt: nutzt vollen Pfad /opt/homebrew/bin/typst, wenn vorhanden
  private static String findTypstBinary() {
    String env = System.getenv("TYPST_BIN");
    if (env != null && !env.isBlank() && Files.isExecutable(Path.of(env))) return env;
    String[] candidates = {
      "/opt/homebrew/bin/typst",
      "/usr/local/bin/typst",
      "/usr/bin/typst"
    };
    for (String c : candidates) if (Files.isExecutable(Path.of(c))) return c;
    return "typst";
  }
}

