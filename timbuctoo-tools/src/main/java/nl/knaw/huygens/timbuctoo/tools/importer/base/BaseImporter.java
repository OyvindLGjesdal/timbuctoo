package nl.knaw.huygens.timbuctoo.tools.importer.base;

/*
 * #%L
 * Timbuctoo tools
 * =======
 * Copyright (C) 2012 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.io.File;

import nl.knaw.huygens.timbuctoo.XRepository;
import nl.knaw.huygens.timbuctoo.model.base.BaseLanguage;
import nl.knaw.huygens.timbuctoo.model.base.BaseLocation;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.tools.config.ToolsInjectionModule;
import nl.knaw.huygens.timbuctoo.tools.importer.DefaultImporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * Importer for base domain entities, such as language.
 */
public class BaseImporter extends DefaultImporter {

  private static final Logger LOG = LoggerFactory.getLogger(BaseImporter.class);

  private static final String USER_ID = "importer";
  private static final String VRE_ID = "base";

  public static void main(String[] args) throws Exception {
    Stopwatch stopWatch = Stopwatch.createStarted();

    // Handle commandline arguments
    String directoryName = (args.length > 0) ? args[0] : "../../timbuctoo-testdata/src/main/resources/general/";
    File directory = new File(directoryName);
    if (!directory.isDirectory()) {
      System.out.println("## Not a directory: " + directoryName);
      System.exit(-1);
    }
    File languageFile = new File(directory, "iso-639-3.tab");
    if (!languageFile.canRead()) {
      System.out.println("## Can not read file: " + languageFile.getAbsolutePath());
      System.exit(-1);
    }
    File locationFile = new File(directory, "locations.json");
    if (!locationFile.canRead()) {
      System.out.println("## Can not read file: " + locationFile.getAbsolutePath());
      System.exit(-1);
    }

    Change change = new Change(USER_ID, VRE_ID);

    BaseImporter importer = null;
    try {
      XRepository instance = ToolsInjectionModule.createRepositoryInstance();

      // Get rid of existing stuff
      importer = new BaseImporter(instance);
      importer.removeNonPersistentEntities(BaseLanguage.class);
      importer.removeNonPersistentEntities(BaseLocation.class);

      importer.printBoxedText("Import languages");
      new LanguageImporter(instance.getRepository(), change).handleFile(languageFile, 0, false);

      importer.printBoxedText("Import locations");
      new LocationImporter(instance, change).handleFile(locationFile);

      importer.printBoxedText("Indexing");
      importer.indexEntities(BaseLanguage.class);
      importer.indexEntities(BaseLocation.class);

    } finally {
      if (importer != null) {
        importer.close();
      }
      LOG.info("Time used: {}", stopWatch);
    }
  }

  public BaseImporter(XRepository repository) {
    super(repository);
  }

}
