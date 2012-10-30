package com.camunda.fox.cycle.web.service.resource;

import java.io.File;
import static org.fest.assertions.api.Assertions.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.camunda.fox.cycle.connector.Connector;
import com.camunda.fox.cycle.connector.ConnectorNode;
import com.camunda.fox.cycle.connector.ConnectorNodeType;
import com.camunda.fox.cycle.connector.ConnectorRegistry;
import com.camunda.fox.cycle.connector.ContentInformation;
import com.camunda.fox.cycle.connector.test.util.RepositoryUtil;
import com.camunda.fox.cycle.connector.vfs.VfsConnector;
import com.camunda.fox.cycle.entity.BpmnDiagram;
import com.camunda.fox.cycle.entity.ConnectorConfiguration;
import com.camunda.fox.cycle.entity.Roundtrip;
import com.camunda.fox.cycle.entity.Roundtrip.SyncMode;
import com.camunda.fox.cycle.repository.RoundtripRepository;
import com.camunda.fox.cycle.util.IoUtil;
import com.camunda.fox.cycle.web.dto.BpmnDiagramDTO;
import com.camunda.fox.cycle.web.dto.BpmnDiagramStatusDTO;
import com.camunda.fox.cycle.web.dto.ConnectorNodeDTO;
import com.camunda.fox.cycle.web.dto.RoundtripDTO;

/**
 *
 * @author nico.rehwaldt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  locations = {"classpath:/spring/test-*.xml"}
)
public class RoundtripServiceTest {
  
  // statically cached connector
  private static Connector connector;
  
  @Inject
  private ConnectorRegistry connectorRegistry;

  @Inject
  private RoundtripRepository roundtripRepository;

  @Inject
  private RoundtripService roundtripService;
  
  @Inject
  private BpmnDiagramService diagramService;

  private ConnectorNode rightNode;

  private ConnectorNode leftNode;

  @Test
  public void shouldCreateRoundtrip() throws Exception {
    // given
    RoundtripDTO data = createTestRoundtripDTOWithDetails();

    // when
    RoundtripDTO createdData = roundtripService.create(data);

    // then
    assertThat(createdData.getId()).isNotNull();
  }

  @Test
  public void shouldUpdateRoundtripDetails() throws Exception {
    // given
    RoundtripDTO roundtrip = createAndFlushRoundtrip();

    // then
    assertThat(roundtrip.getLeftHandSide()).isNotNull();
    assertThat(roundtrip.getRightHandSide()).isNotNull();
    assertThat(roundtrip.getLeftHandSide().getId()).isNotNull();
    assertThat(roundtrip.getRightHandSide().getId()).isNotNull();
  }

  @Test
  public void shouldNotServeNonExistingDiagram() throws Exception {

    // given
    RoundtripDTO roundtrip = createAndFlushRoundtrip();
    BpmnDiagramDTO leftHandSide = roundtrip.getLeftHandSide();

    try {
      // when
      diagramService.getImage(leftHandSide.getId());

      fail("expected NOT_FOUND");
    } catch (WebApplicationException e) {
      // then
      // expected
    }
  }

  @Test
  public void shouldServeExistingDiagram() throws Exception {

    // given
    RoundtripDTO roundtrip = createAndFlushRoundtrip();
    BpmnDiagramDTO rightHandSide = roundtrip.getRightHandSide();

    // when
    Response response = diagramService.getImage(rightHandSide.getId());
    
    // then
    assertThat(response.getEntity()).isInstanceOf(byte[].class);
  }

  @Test
  public void shouldServeValidDiagramStatus() throws Exception {

    // given
    RoundtripDTO roundtrip = createAndFlushRoundtrip();
    BpmnDiagramDTO leftHandSide = roundtrip.getLeftHandSide();
    
    try {
      // when
      diagramService.getImage(leftHandSide.getId());
      
      fail("expected NOT_FOUND");
    } catch (WebApplicationException e) {
      // then
      // expected
    }
  }

  @Test
  public void shouldSynchronizeRightToLeft() throws FileNotFoundException, Exception {
    RoundtripDTO testRoundtrip = createAndFlushRoundtrip();
    BpmnDiagramDTO rightHandSide = testRoundtrip.getRightHandSide();
    
    roundtripService.doSynchronize(SyncMode.RIGHT_TO_LEFT, testRoundtrip.getId());
    Response imageResponse = diagramService.getImage(rightHandSide.getId());
    
    // then
    assertThatIsInSync(testRoundtrip.getRightHandSide());
    assertThatIsInSync(testRoundtrip.getLeftHandSide());
    
    assertThat(imageResponse.getStatus()).isEqualTo(200);
    assertThat(IoUtil.toString(connector.getContent(leftNode))).contains("activiti:class=\"java.lang.Object\"");
  }

  @Test
  public void shouldSynchronizeLeftToRight() throws FileNotFoundException, Exception {
    RoundtripDTO testRoundtrip = createAndFlushRoundtrip();
    BpmnDiagramDTO rightHandSide = testRoundtrip.getRightHandSide();
    
    roundtripService.doSynchronize(SyncMode.LEFT_TO_RIGHT, testRoundtrip.getId());
    
    assertThatIsInSync(testRoundtrip.getRightHandSide());
    assertThatIsInSync(testRoundtrip.getLeftHandSide());
    
    try {
      diagramService.getImage(rightHandSide.getId());
      fail("Expected out of date diagram image");
    } catch (WebApplicationException e) {
      // then (1)
      // expected
    }
    
    // then (2)
    assertThat(IoUtil.toString(connector.getContent(rightNode))).doesNotContain("activiti:class=\"java.lang.Object\"");
  }

  @Test
  @Ignore
  public void shouldTestCreateModelLeftToRight() {
    fail("no test");
  }

  @Test
  @Ignore
  public void shouldTestCreateModelRightToLeft() {
    fail("no test");
  }

  @Test
  public void shouldDeleteRoundtrip() {
    RoundtripDTO roundtripDTO = createTestRoundtripDTO();
    roundtripDTO = roundtripService.create(roundtripDTO);
    
    Roundtrip roundtrip = roundtripRepository.findById(roundtripDTO.getId());
    Assert.assertNotNull(roundtrip);
    
    roundtripRepository.delete(roundtrip.getId());
    
    Roundtrip deletedRoundtrip = roundtripRepository.findById(roundtrip.getId());
    Assert.assertNull(deletedRoundtrip);
  }

  // Assertions //////////////////////////////////////////////////
  
  private void assertThatIsInSync(BpmnDiagramDTO diagram) {
    BpmnDiagramStatusDTO rhsAsyncSyncStatus = diagramService.synchronizationStatus(diagram.getId());
    assertThat(rhsAsyncSyncStatus.getStatus()).isEqualTo(BpmnDiagram.Status.SYNCED);
  }

  // Test data generation //////////////////////////////////////// 
  
  private RoundtripDTO createAndFlushRoundtrip() {
    Roundtrip r = roundtripRepository.saveAndFlush(new Roundtrip("Test Roundtrip"));
    RoundtripDTO data = createTestRoundtripDTOWithDetails();
    data.setId(r.getId());
    return roundtripService.updateDetails(data);
  }

  private RoundtripDTO createTestRoundtripDTO() {
    RoundtripDTO dto = new RoundtripDTO();
    dto.setLastSync(new Date());
    dto.setName("Test Roundtrip");
    return dto;
  }

  private RoundtripDTO createTestRoundtripDTOWithDetails() {
    RoundtripDTO dto = createTestRoundtripDTO();

    BpmnDiagramDTO rhs = new BpmnDiagramDTO();
    ConnectorNodeDTO rhsNode = new ConnectorNodeDTO("foo/Impl.bpmn", "Impl", connector.getId());

    rhs.setModeler("Fox designer");
    rhs.setConnectorNode(rhsNode);

    BpmnDiagramDTO lhs = new BpmnDiagramDTO();
    ConnectorNodeDTO lhsNode = new ConnectorNodeDTO("foo/Modeler.bpmn", "Modeler", connector.getId());

    lhs.setModeler("Another Modeler");
    lhs.setConnectorNode(lhsNode);

    dto.setRightHandSide(rhs);
    dto.setLeftHandSide(lhs);

    return dto;
  }

  // Test bootstraping //////////////////////////////////////// 

  private static final File VFS_DIRECTORY = new File("target/vfs-repository");

  private static Class<? extends Connector> CONNECTOR_CLS = VfsConnector.class;

  /**
   * For tests only, ensures that the connector is initialized and the 
   * connector cache contains it
   * 
   * @throws Exception 
   */
  protected void ensureConnectorInitialized() throws Exception {
    List<ConnectorConfiguration> configurations = connectorRegistry.getConnectorConfigurations(CONNECTOR_CLS);
    ConnectorConfiguration config = configurations.get(0);

    // put mock connector to registry
    connectorRegistry.getCache().put(config.getId(), connector);

    // fake some connector properties
    connector.getConfiguration().setId(config.getId());
    connector.getConfiguration().setConnectorClass(config.getConnectorClass());
  }

  @Before
  public void before() throws FileNotFoundException, Exception {
    ensureConnectorInitialized();

    connector.createNode("/", "foo", ConnectorNodeType.FOLDER);

    ConnectorNode rightNodeImg = connector.createNode("/foo", "Impl.png", ConnectorNodeType.PNG_FILE);
    importNode(rightNodeImg, "com/camunda/fox/cycle/roundtrip/repository/test-rhs.png");

    rightNode = connector.createNode("/foo", "Impl.bpmn", ConnectorNodeType.ANY_FILE);
    importNode(rightNode, "com/camunda/fox/cycle/roundtrip/repository/test-rhs.bpmn");

    leftNode = connector.createNode("/foo", "Modeler.bpmn", ConnectorNodeType.ANY_FILE);
    importNode(leftNode, "com/camunda/fox/cycle/roundtrip/repository/test-lhs.bpmn");
  }

  @After
  public void after() {
    // Remove all entities
    roundtripRepository.deleteAll();

    connector.deleteNode(new ConnectorNode("foo/Impl.png"));
    connector.deleteNode(new ConnectorNode("foo/Impl.bpmn"));
    connector.deleteNode(new ConnectorNode("foo/Modeler.bpmn"));
    connector.deleteNode(new ConnectorNode("foo"));
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    ConnectorConfiguration config = new ConnectorConfiguration();

    String url = RepositoryUtil.createVFSRepository(VFS_DIRECTORY);

    config.getProperties().put(VfsConnector.BASE_PATH_KEY, url);

    // NOT a spring bean!
    connector = new VfsConnector();
    connector.setConfiguration(config);
    connector.init();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    RepositoryUtil.clean(VFS_DIRECTORY);
  }

  // Utility methods /////////////////////////////
  
  protected ContentInformation importNode(ConnectorNode node, String classPathEntry) throws Exception {
    return connector.updateContent(node, new FileInputStream(IoUtil.getFile(classPathEntry)));
  }
}
