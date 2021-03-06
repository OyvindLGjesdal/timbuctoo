package nl.knaw.huygens.timbuctoo.v5.datastores.rssource;

import nl.knaw.huygens.timbuctoo.remote.rs.xml.Capability;
import nl.knaw.huygens.timbuctoo.remote.rs.xml.RsLn;
import nl.knaw.huygens.timbuctoo.remote.rs.xml.RsMd;
import nl.knaw.huygens.timbuctoo.remote.rs.xml.UrlItem;
import nl.knaw.huygens.timbuctoo.remote.rs.xml.Urlset;
import nl.knaw.huygens.timbuctoo.util.UriHelper;
import nl.knaw.huygens.timbuctoo.v5.dataset.DataSetRepository;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.DataSet;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.DataSetMetaData;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.LogEntry;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.LogList;
import nl.knaw.huygens.timbuctoo.v5.filestorage.FileStorage;
import nl.knaw.huygens.timbuctoo.v5.filestorage.dto.CachedFile;
import nl.knaw.huygens.timbuctoo.v5.security.dto.User;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Builds ResourceSync documents and provides ResourceSync-related resources.
 *
 * @see <a href="http://www.openarchives.org/rs/toc">http://www.openarchives.org/rs/toc</a>
 */
public class RsDocumentBuilder {

  public static final String SOURCE_DESCRIPTION_PATH = "sourceDescription.xml";
  private static final String REL_DESCRIBED_BY = "describedby";
  private static final String REL_UP = "up";
  private static final String DESCRIPTION_FILENAME = "description.xml";
  private static final String DESCRIPTION_TYPE = "application/rdf+xml";

  private final DataSetRepository dataSetRepository;
  private final RsUriHelper rsUriHelper;

  /**
   * Construct a {@link RsDocumentBuilder}. Construction is usually done on server-configuration level.
   *
   * @param dataSetRepository Repository
   * @param uriHelper helper class
   */
  public RsDocumentBuilder(DataSetRepository dataSetRepository, UriHelper uriHelper) {
    this.dataSetRepository = dataSetRepository;
    rsUriHelper = new RsUriHelper(uriHelper);
  }

  /**
   * Get the source description document. If <code>user</code> == <code>null</code> the source description will
   * only have links to capability lists of published dataSets. Otherwise the source description will have
   * links to capability lists of published dataSets and the dataSets for which the user has read access.
   *
   * @param user User that requests the document, may be <code>null</code>
   * @return the source description document.
   */
  public Urlset getSourceDescription(@Nullable User user) {
    RsMd rsMd = new RsMd(Capability.DESCRIPTION.xmlValue);
    Urlset sourceDescription = new Urlset(rsMd);

    for (DataSet dataSet : dataSetRepository.getDataSetsWithReadAccess(user)) {
      DataSetMetaData dataSetMetaData = dataSet.getMetadata();
      String loc = rsUriHelper.uriForRsDocument(dataSetMetaData, Capability.CAPABILITYLIST);
      String descriptionUrl = rsUriHelper.uriForRsDocument(dataSetMetaData, DESCRIPTION_FILENAME);
      UrlItem item = new UrlItem(loc)
        .withMetadata(new RsMd(Capability.CAPABILITYLIST.xmlValue))
        .addLink(new RsLn(REL_DESCRIBED_BY, descriptionUrl)
          .withType(DESCRIPTION_TYPE));
      sourceDescription.addItem(item);
    }
    return sourceDescription;
  }

  /**
   * Get the capability list for the dataSet denoted by <code>ownerId</code> and <code>dataSetId</code>.
   * The {@link Optional} is empty if the dataSet is not published and the given <code>user</code> == <code>null</code>
   * or has no read access for the dataSet or the dataSet does not exist.
   *
   * @param user User that requests the list, may be <code>null</code>
   * @param ownerId ownerId
   * @param dataSetId dataSetId
   * @return the capability list for the dataSet denoted by <code>ownerId</code> and <code>dataSetId</code>
   */
  public Optional<Urlset> getCapabilityList(@Nullable User user, String ownerId, String dataSetId) {
    Urlset capabilityList = null;
    Optional<DataSet> maybeDataSet = dataSetRepository.getDataSet(user, ownerId, dataSetId);
    if (maybeDataSet.isPresent()) {
      RsMd rsMd = new RsMd(Capability.CAPABILITYLIST.xmlValue);
      capabilityList = new Urlset(rsMd)
        .addLink(new RsLn(REL_UP, rsUriHelper.uriForWellKnownResourceSync()));

      DataSetMetaData dataSetMetaData = maybeDataSet.get().getMetadata();
      String descriptionUrl = rsUriHelper.uriForRsDocument(dataSetMetaData, DESCRIPTION_FILENAME);
      capabilityList.addLink(new RsLn(REL_DESCRIBED_BY, descriptionUrl)
        .withType(DESCRIPTION_TYPE));
      String loc = rsUriHelper.uriForRsDocument(dataSetMetaData, Capability.RESOURCELIST);
      UrlItem item = new UrlItem(loc)
        .withMetadata(new RsMd(Capability.RESOURCELIST.xmlValue));
      capabilityList.addItem(item);
    }
    return Optional.ofNullable(capabilityList);
  }

  /**
   * Get the resource list for the dataSet denoted by <code>ownerId</code> and <code>dataSetId</code>.
   * The {@link Optional} is empty if the dataSet is not published and the given <code>user</code> == <code>null</code>
   * or has no read access for the dataSet or the dataSet does not exist.
   *
   * @param user User that requests the list, may be <code>null</code>
   * @param ownerId ownerId
   * @param dataSetId dataSetId
   * @return the resource list for the dataSet denoted by <code>ownerId</code> and <code>dataSetId</code>
   */
  public Optional<Urlset> getResourceList(@Nullable User user, String ownerId, String dataSetId) throws IOException {
    Urlset resourceList = null;
    Optional<DataSet> maybeDataSet = dataSetRepository.getDataSet(user, ownerId, dataSetId);
    if (maybeDataSet.isPresent()) {
      DataSetMetaData dataSetMetaData = maybeDataSet.get().getMetadata();
      LogList loglist = maybeDataSet.get().getImportManager().getLogList();
      RsMd rsMd = new RsMd(Capability.RESOURCELIST.xmlValue)
        .withAt(ZonedDateTime.parse(loglist.getLastImportDate())); // lastImportDate set on server startup?
      resourceList = new Urlset(rsMd)
        .addLink(new RsLn(REL_UP, rsUriHelper.uriForRsDocument(dataSetMetaData, Capability.CAPABILITYLIST)));

      FileStorage fileStorage = maybeDataSet.get().getFileStorage();
      List<LogEntry> entries = loglist.getEntries();
      entries.sort((e1, e2) -> {
        if (e1.getImportStatus().isPresent() && e2.getImportStatus().isPresent()) {
          return e1.getImportStatus().get().getDate().compareTo(e2.getImportStatus().get().getDate());
        } else if (e1.getImportStatus().isPresent()) {
          return 1;
        } else {
          return -1;
        }
      });
      for (LogEntry logEntry : entries) {
        Optional<String> maybeToken = logEntry.getLogToken();
        if (maybeToken.isPresent()) {
          String loc = rsUriHelper.uriForToken(dataSetMetaData, maybeToken.get());
          Optional<CachedFile> maybeCachedFile = fileStorage.getFile(maybeToken.get());
          if (maybeCachedFile.isPresent()) {
            UrlItem item = new UrlItem(loc)
              .withMetadata(new RsMd()
              .withType(maybeCachedFile.get().getMimeType().toString())
              //.withEncoding(maybeCachedFile.get().charset()) // charset not handed down from FileInfo to CachedFile
              //.withHash(maybeCachedFile.get().getHash()) // hash not computed for imported files...
              //.withLength(maybeCachedFile.get().getLength()) // length not computed ...
              );
            resourceList.addItem(item);
          }
        }
      }
      rsMd.withCompleted(ZonedDateTime.now(ZoneOffset.UTC));
    }
    return Optional.ofNullable(resourceList);
  }

  /**
   * Get the {@link CachedFile} denoted by <code>ownerId</code>, <code>dataSetId</code> and <code>fileId</code>.
   * The {@link Optional} is empty if the dataSet is not published and the given <code>user</code> == <code>null</code>
   * or has no read access for the dataSet or the dataSet does not exist.
   *
   * @param user User that requests the file, may be <code>null</code>
   * @param ownerId ownerId
   * @param dataSetId dataSetId
   * @param fileId fileId
   * @return the {@link CachedFile} denoted by <code>ownerId</code>, <code>dataSetId</code> and <code>fileId</code>
   * @throws IOException for FileStorage failure
   */
  public Optional<CachedFile> getCachedFile(@Nullable User user, String ownerId, String dataSetId, String fileId)
    throws IOException {
    Optional<DataSet> maybeDataSet = dataSetRepository.getDataSet(user, ownerId, dataSetId);
    if (maybeDataSet.isPresent()) {
      return maybeDataSet.get().getFileStorage().getFile(fileId);
    }
    return Optional.empty();
  }

  /**
   * Get the dataSet description for the dataSet denoted by <code>ownerId</code> and <code>dataSetId</code>.
   * The {@link Optional} is empty if the dataSet is not published and the given <code>user</code> == <code>null</code>
   * or has no read access for the dataSet or the dataSet does not exist.
   *
   * @param user User that requests the description, may be <code>null</code>
   * @param ownerId ownerId
   * @param dataSetId datasetId
   * @return the dataSet description for the dataSet denoted by <code>ownerId</code> and <code>dataSetId</code>
   */
  public Optional<File> getDataSetDescription(@Nullable User user, String ownerId, String dataSetId) {
    return dataSetRepository.getDataSet(user, ownerId, dataSetId).map(DataSet::getResourceSyncDescription);
  }


}
