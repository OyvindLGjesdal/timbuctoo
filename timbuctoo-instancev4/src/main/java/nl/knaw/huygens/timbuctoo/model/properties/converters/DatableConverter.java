package nl.knaw.huygens.timbuctoo.model.properties.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.huygens.timbuctoo.experimental.exports.ExcelDescription;
import nl.knaw.huygens.timbuctoo.experimental.exports.excel.StringExcelDescription;

import java.io.IOException;

public class DatableConverter implements Converter {
  @Override
  public Object jsonToTinkerpop(JsonNode json) throws IOException {
    if (!json.isTextual()) {
      throw new IOException("datable should be presented as String");
    }

    return json.toString();
  }

  @Override
  public JsonNode tinkerpopToJson(Object value) throws IOException {
    if (value instanceof String) {
      JsonNode json = new ObjectMapper().readTree((String) value);
      if (!json.isTextual()) {
        throw new IOException("should be a serialized JSON string");
      }
      return json;
    } else {
      throw new IOException("should be a string");
    }
  }

  public String getTypeIdentifier() {
    return "datable";
  }

  @Override
  public ExcelDescription tinkerPopToExcel(Object value, String guiTypeId) throws IOException {
    return new StringExcelDescription(tinkerpopToJson(value).asText(), guiTypeId);
  }
}
