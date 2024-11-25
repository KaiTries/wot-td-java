package ch.unisg.ics.interactions.wot.td.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.stream.Stream;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.commons.rdf.rdf4j.RDF4JGraph;
import org.apache.commons.rdf.rdf4j.RDF4JTriple;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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
  private final static RDF4J rdfImpl = new RDF4J();

  static Model readModelFromString(RDFFormat format, String description, String baseURI) 
      throws RDFParseException, RDFHandlerException, IOException {
    StringReader stringReader = new StringReader(description);
    
    RDFParser rdfParser = Rio.createParser(format);
    Model model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    
    rdfParser.parse(stringReader, baseURI);
    
    return model;
  }

  public static Graph stringToGraph(String graphString, IRI baseIRI, RDFSyntax syntax) throws IllegalArgumentException, IOException {
    StringReader stringReader = new StringReader(graphString);

    RDFFormat format = RDFFormat.JSONLD;
    if (syntax.equals(RDFSyntax.TURTLE)) {
      format = RDFFormat.TURTLE;
    }

    RDFParser rdfParser = Rio.createParser(format);
    Model model = new LinkedHashModel();
    rdfParser.set(JSONLDSettings.SECURE_MODE, false);
    // rdfParser.set(JSONLDSettings.EXCEPTION_ON_WARNING, true);
    rdfParser.setRDFHandler(new StatementCollector(model));

    try {
      rdfParser.parse(stringReader, null);
    }
    catch (RDFParseException e) {
      throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
    }
    catch (RDFHandlerException e) {
      throw new IOException("RDF handler exception: " + e.getMessage());
    }
    finally {
      stringReader.close();
    }
    return rdfImpl.asGraph(model);
  }


  public static String graphToString(Graph graph, RDFSyntax syntax) throws IllegalArgumentException,
      IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    RDFWriter writer;

    if (syntax.equals(RDFSyntax.TURTLE)) {
      writer = Rio.createWriter(RDFFormat.TURTLE, out);
    } else if (syntax.equals(RDFSyntax.JSONLD)) {
      writer = Rio.createWriter(RDFFormat.JSONLD, out);
      writer.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.FLATTEN);
      writer.getWriterConfig().set(JSONLDSettings.USE_NATIVE_TYPES, true);
      writer.getWriterConfig().set(JSONLDSettings.OPTIMIZE, true);
    } else {
      throw new IllegalArgumentException("Unsupported RDF serialization format.");
    }

    writer.getWriterConfig()
        .set(BasicWriterSettings.PRETTY_PRINT, true)
        .set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true)
        .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
        .set(BasicWriterSettings.INLINE_BLANK_NODES, true);

    if (graph instanceof RDF4JGraph) {
      try {
        writer.startRDF();

        writer.handleNamespace("eve", "http://w3id.org/eve#");
        writer.handleNamespace("td", "https://www.w3.org/2019/wot/td#");
        writer.handleNamespace("htv", "http://www.w3.org/2011/http#");
        writer.handleNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#");
        writer.handleNamespace("wotsec", "https://www.w3.org/2019/wot/security#");
        writer.handleNamespace("dct", "http://purl.org/dc/terms/");
        writer.handleNamespace("js", "https://www.w3.org/2019/wot/json-schema#");
        writer.handleNamespace("saref", "https://w3id.org/saref#");

        try (Stream<RDF4JTriple> stream = ((RDF4JGraph) graph).stream()) {
          stream.forEach(triple -> {
            writer.handleStatement(triple.asStatement());
          });
        }
        writer.endRDF();
      }
      catch (RDFHandlerException e) {
        throw new IOException("RDF handler exception: " + e.getMessage());
      }
      catch (UnsupportedRDFormatException e) {
        throw new IllegalArgumentException("Unsupported RDF syntax: " + e.getMessage());
      }
      finally {
        out.close();
      }
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }
    return out.toString();
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
