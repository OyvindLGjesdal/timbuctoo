package nl.knaw.huygens.timbuctoo.experimental.databaselog;

public interface VertexLogEntry extends LogEntry {

  void addEdgeLogEntriesTo(EdgeLogEntryAdder edgeLogEntryAdder);
}
