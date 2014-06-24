package nl.knaw.huygens.timbuctoo.index;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import nl.knaw.huygens.facetedsearch.FacetedSearchLibrary;
import nl.knaw.huygens.facetedsearch.model.parameters.IndexDescription;
import nl.knaw.huygens.solr.AbstractSolrServer;
import nl.knaw.huygens.solr.AbstractSolrServerBuilder;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;

import org.apache.solr.core.CoreDescriptor;
import org.junit.Test;

import test.timbuctoo.index.model.BaseType1;

public class SolrIndexFactoryTest {
  @Test
  public void testCreate() {
    // It should create a list of facet definitions.
    // It should create an AbstractSolrServer.
    // It should create a FacetedSearchLibrary

    // setup
    AbstractSolrServer solrServerMock = mock(AbstractSolrServer.class);
    IndexDescription facetDefinitions = mock(IndexDescription.class);
    FacetedSearchLibrary facetedSearchLibraryMock = mock(FacetedSearchLibrary.class);
    SolrInputDocumentCreator solrInputDocumentCreatorMock = mock(SolrInputDocumentCreator.class);

    IndexDescriptionFactory indexDescriptionFactoryMock = mock(IndexDescriptionFactory.class);
    AbstractSolrServerBuilder solrServerBuilderMock = mock(AbstractSolrServerBuilder.class);
    FacetedSearchLibraryFactory facetedSearchLibraryFactoryMock = mock(FacetedSearchLibraryFactory.class);

    String name = "test";
    Class<? extends DomainEntity> type = BaseType1.class;

    Index expectedSolrIndex = new SolrIndex(name, solrInputDocumentCreatorMock, solrServerMock, facetedSearchLibraryMock);

    when(indexDescriptionFactoryMock.create(type)).thenReturn(facetDefinitions);
    when(solrServerBuilderMock.setCoreName(name)).thenReturn(solrServerBuilderMock);
    when(solrServerBuilderMock.build(facetDefinitions)).thenReturn(solrServerMock);
    when(solrServerBuilderMock.addProperty(CoreDescriptor.CORE_DATADIR, "data/" + name.replace('.', '/'))).thenReturn(solrServerBuilderMock);
    when(facetedSearchLibraryFactoryMock.create(solrServerMock)).thenReturn(facetedSearchLibraryMock);

    SolrIndexFactory instance = new SolrIndexFactory(solrInputDocumentCreatorMock, solrServerBuilderMock, indexDescriptionFactoryMock, facetedSearchLibraryFactoryMock);

    // action
    SolrIndex actualSolrIndex = instance.createIndexFor(type, name);

    // verify
    assertThat(actualSolrIndex, is(equalTo(expectedSolrIndex)));
  }
}
