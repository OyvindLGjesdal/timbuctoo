package nl.knaw.huygens.timbuctoo.v5.rdfio.implementations.rdf4j;

import nl.knaw.huygens.timbuctoo.v5.dataset.RdfProcessor;
import nl.knaw.huygens.timbuctoo.v5.dataset.exceptions.RdfProcessingFailedException;
import nl.knaw.huygens.timbuctoo.v5.filestorage.dto.CachedLog;
import nl.knaw.huygens.timbuctoo.v5.rdfio.RdfParser;
import nl.knaw.huygens.timbuctoo.v5.rdfio.implementations.rdf4j.parsers.TimRdfHandler;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import java.io.IOException;

public class Rdf4jRdfParser implements RdfParser {
  @Override
  public void importRdf(CachedLog input, String baseUri, String defaultGraph, RdfProcessor rdfProcessor)
    throws RdfProcessingFailedException {

    try {
      RDFFormat format = Rio.getParserFormatForMIMEType(input.getMimeType().toString())
        .orElseThrow(
          () -> new UnsupportedRDFormatException(input.getMimeType() + " is not a supported rdf type.")
        );
      RDFParser rdfParser = Rio.createParser(format);
      rdfParser.setPreserveBNodeIDs(true);
      rdfParser.setRDFHandler(new TimRdfHandler(rdfProcessor, defaultGraph, input.getFile().getName()));
      rdfParser.parse(input.getReader(), baseUri);
    } catch (IOException | RDFParseException | UnsupportedRDFormatException e) {
      throw new RdfProcessingFailedException(e);
    } catch (RDFHandlerException e) {
      if (e.getCause() instanceof RdfProcessingFailedException) {
        throw (RdfProcessingFailedException) e.getCause();
      } else {
        throw new RdfProcessingFailedException(e);
      }
    }
  }
}
