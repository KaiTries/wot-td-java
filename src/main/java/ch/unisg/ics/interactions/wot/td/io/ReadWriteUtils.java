package ch.unisg.ics.interactions.wot.td.io;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public final class ReadWriteUtils {
  private final static Logger LOGGER = Logger.getLogger(ReadWriteUtils.class.getCanonicalName());
  private final static ObjectMapper o = new ObjectMapper()
      .registerModule(new Jdk8Module());


  public static String tdToJsonLD(final ThingDescription td) throws JsonProcessingException {
    o.enable(SerializationFeature.INDENT_OUTPUT);
    o.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return o.writeValueAsString(td);
  }

  static Model readModelFromString(String description, String baseURI)
      throws RDFParseException, RDFHandlerException, IOException {
    StringReader stringReader = new StringReader(description);
    
    RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
    Model model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    
    rdfParser.parse(stringReader, baseURI);
    
    return model;
  }

  public static String modelToString(final Model model, final RDFFormat format, final String base)
      throws IllegalArgumentException {
    final var test = new ByteArrayOutputStream();

    final RDFWriter writer;
    try {
      if (base == null) {
        writer = Rio.createWriter(format, test);
      } else {
        writer = Rio.createWriter(format, test, base);
      }
    } catch (final UnsupportedRDFormatException | URISyntaxException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
      return "";
    }

    if (format.equals(RDFFormat.JSONLD)) {
      writer.getWriterConfig()
          .set(JSONLDSettings.JSONLD_MODE,
              JSONLDMode.FLATTEN)
          .set(JSONLDSettings.USE_NATIVE_TYPES, true)
          .set(JSONLDSettings.OPTIMIZE, true);
    }
    writer.getWriterConfig()
        .set(BasicWriterSettings.PRETTY_PRINT, true)
        .set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true)
        .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
        .set(BasicWriterSettings.INLINE_BLANK_NODES, true);

    try {
      writer.startRDF();
      model.getNamespaces().forEach(namespace ->
          writer.handleNamespace(namespace.getPrefix(), namespace.getName()));
      model.forEach(writer::handleStatement);
      writer.endRDF();
    } catch (RDFHandlerException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
    }
    return test.toString(StandardCharsets.UTF_8);
  }



  static String writeToString(RDFFormat format, Model model) {
    OutputStream out = new ByteArrayOutputStream();

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
    
    return out.toString();
  }
  
  private ReadWriteUtils() { }
}
