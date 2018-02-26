package nl.knaw.huygens.timbuctoo.v5.graphql.mutations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import nl.knaw.huygens.timbuctoo.util.Tuple;
import nl.knaw.huygens.timbuctoo.v5.dataset.DataSetRepository;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.DataSet;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.DataSetMetaData;
import nl.knaw.huygens.timbuctoo.v5.datastores.implementations.bdb.MergeExplicitSchemas;
import nl.knaw.huygens.timbuctoo.v5.datastores.implementations.bdb.MergeSchemas;
import nl.knaw.huygens.timbuctoo.v5.datastores.schemastore.SchemaStore;
import nl.knaw.huygens.timbuctoo.v5.datastores.schemastore.dto.ExplicitField;
import nl.knaw.huygens.timbuctoo.v5.datastores.schemastore.dto.ExplicitType;
import nl.knaw.huygens.timbuctoo.v5.datastores.schemastore.dto.Type;
import nl.knaw.huygens.timbuctoo.v5.filehelper.FileHelper;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.ContextData;
import nl.knaw.huygens.timbuctoo.v5.jacksonserializers.TimbuctooCustomSerializers;
import nl.knaw.huygens.timbuctoo.v5.security.dto.User;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MergeSchemaDataFetcher implements DataFetcher {
  private final DataSetRepository dataSetRepository;


  public MergeSchemaDataFetcher(DataSetRepository dataSetRepository) {
    this.dataSetRepository = dataSetRepository;
  }

  @Override
  public Object get(DataFetchingEnvironment env) {
    ContextData contextData = env.getContext();
    Optional<User> user = contextData.getUser();
    String dataSetId = env.getArgument("dataSet");
    Tuple<String, String> ownerIdDataSetName = DataSetMetaData.splitCombinedId(dataSetId);

    if (!user.isPresent()) {
      throw new RuntimeException("User not present.");
    }

    Optional<DataSet> dataSet = dataSetRepository.getDataSet(user.get(),
      ownerIdDataSetName.getLeft(), ownerIdDataSetName.getRight());

    if (!dataSet.isPresent()) {
      throw new RuntimeException("Can't retrieve dataset.");
    }

    final SchemaStore generatedSchema = dataSet.get().getSchemaStore();

    Map<String, Type> customTypes = new HashMap<>();
    List<ExplicitType> customSchema;
    Map<String, List<ExplicitField>> newCustomSchema = new HashMap<>();

    ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new Jdk8Module())
      .registerModule(new GuavaModule())
      .registerModule(new TimbuctooCustomSerializers())
      .enable(SerializationFeature.INDENT_OUTPUT);

    try {
      String customSchemaString = objectMapper.writeValueAsString(env.getArgument("customSchema"));
      customSchema = objectMapper.readValue(customSchemaString, new TypeReference<List<ExplicitType>>() {
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    for (ExplicitType explicitType : customSchema) {
      customTypes.put(explicitType.getName(), explicitType.convertToType());
      newCustomSchema.put(explicitType.getName(), explicitType.getFields());
    }

    File customSchemaFile = dataSet.get().getCustomSchemaFile();


    Map<String, List<ExplicitField>> existingCustomSchema = new HashMap<>();

    if (customSchemaFile.exists()) {
      try {
        existingCustomSchema = objectMapper.readValue(customSchemaFile,
          new TypeReference<Map<String, List<ExplicitField>>>() {
          });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    Map<String, Type> existingCustomSchemaTypes = new HashMap<>();

    for (Map.Entry<String, List<ExplicitField>> entry : existingCustomSchema.entrySet()) {
      ExplicitType tempExplicitType = new ExplicitType(entry.getKey(), entry.getValue());

      existingCustomSchemaTypes.put(entry.getKey(), tempExplicitType.convertToType());
    }

    MergeSchemas mergeSchemas = new MergeSchemas();

    customTypes = mergeSchemas.mergeSchema(existingCustomSchemaTypes, customTypes);

    MergeExplicitSchemas mergeExplicitSchemas = new MergeExplicitSchemas();

    Map<String, List<ExplicitField>> mergedExplicitSchema = mergeExplicitSchemas.mergeExplicitSchemas(
      existingCustomSchema,
      newCustomSchema);

    try {
      objectMapper.writeValue(customSchemaFile, mergedExplicitSchema);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Map<String, Type> mergedSchema = mergeSchemas.mergeSchema(generatedSchema.getStableTypes(),
      customTypes);

    for (Map.Entry<String, Type> customType : customTypes.entrySet()) {
      if (!mergedSchema.containsKey(customType.getKey())) {
        return ImmutableMap.of("message", "Schema extension was unsuccessful.");
      }
    }

    return ImmutableMap.of("message", "Schema extended successfully.");
  }
}
