package ch.unisg.ics.interactions.wot.td.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import no.hasmac.jsonld.JsonLdError;
import no.hasmac.jsonld.document.Document;
import no.hasmac.jsonld.document.JsonDocument;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.jsonld.JSONLDWriter;

public final class ReadWriteUtils {
  private final static Logger LOGGER = Logger.getLogger(ReadWriteUtils.class.getCanonicalName());

  public static Model readModelFromString(RDFFormat format, String description, String baseURI)
      throws RDFParseException, RDFHandlerException, IOException {
    StringReader stringReader = new StringReader(description);
    
    RDFParser rdfParser = Rio.createParser(format);
    Model model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    
    rdfParser.parse(stringReader, baseURI);
    
    return model;
  }

  public static Document getTDFrame() throws URISyntaxException, IOException, JsonLdError {
    final var inputFrame = Files.readString(
        Path.of(ClassLoader.getSystemResource("frame.jsonld").toURI()),
        StandardCharsets.UTF_8
    );
    return JsonDocument.of(new ByteArrayInputStream(inputFrame.getBytes()));
  }
  
  public static String writeToString(RDFFormat format, Model model) {
    OutputStream out = new ByteArrayOutputStream();


    if (format.equals(RDFFormat.JSONLD)) {
      Document d;
      try {
        d = getTDFrame();
      } catch (Exception e) {
        System.out.println("failed to get frame");
        return "";
      }
      JSONLDWriter w = new JSONLDWriter(out);
      w.set(JSONLDSettings.SECURE_MODE, false);
      w.set(JSONLDSettings.FRAME, d);
      w.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.EXPAND);
      w.set(JSONLDSettings.JSONLD_MODE,JSONLDMode.COMPACT);
      Rio.write(model, w);

    } else {

      try {
        Rio.write(model, out, format,
            new WriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, true));
      } finally {
        try {
          out.close();
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, e.getMessage());
        }
      }
    }
    return out.toString();
  }
  
  private ReadWriteUtils() { }
}
