package org.camunda.operate.zeebeimport;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.camunda.operate.entities.ActivityState;
import org.camunda.operate.entities.ActivityType;
import org.camunda.operate.entities.detailview.ActivityInstanceForDetailViewEntity;
import org.camunda.operate.es.schema.templates.ActivityInstanceTemplate;
import org.camunda.operate.exceptions.PersistenceException;
import org.camunda.operate.util.DateUtil;
import org.camunda.operate.util.ElasticsearchUtil;
import org.camunda.operate.util.IdUtil;
import org.camunda.operate.zeebeimport.cache.WorkflowCache;
import org.camunda.operate.zeebeimport.record.value.IncidentRecordValueImpl;
import org.camunda.operate.zeebeimport.record.value.WorkflowInstanceRecordValueImpl;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.exporter.record.Record;
import io.zeebe.protocol.BpmnElementType;
import io.zeebe.protocol.intent.IncidentIntent;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.ELEMENT_COMPLETED;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.ELEMENT_READY;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.ELEMENT_TERMINATED;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.EVENT_ACTIVATED;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.EVENT_ACTIVATING;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.EVENT_TRIGGERED;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.EVENT_TRIGGERING;
import static io.zeebe.protocol.intent.WorkflowInstanceIntent.GATEWAY_ACTIVATED;

@Component
public class DetailViewZeebeRecordProcessor {

  private static final Logger logger = LoggerFactory.getLogger(DetailViewZeebeRecordProcessor.class);

  private static final Set<String> AI_FINISH_STATES = new HashSet<>();
  private static final Set<String> AI_START_STATES = new HashSet<>();

  static {
    AI_FINISH_STATES.add(ELEMENT_COMPLETED.name());
    AI_FINISH_STATES.add(ELEMENT_TERMINATED.name());
    AI_FINISH_STATES.add(EVENT_TRIGGERED.name());

    AI_START_STATES.add(ELEMENT_READY.name());
    AI_START_STATES.add(EVENT_ACTIVATING.name());
  }

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TransportClient esClient;

  @Autowired
  private ActivityInstanceTemplate activityInstanceTemplate;

  @Autowired
  private WorkflowCache workflowCache;

  public void processIncidentRecord(Record record, BulkRequestBuilder bulkRequestBuilder) throws PersistenceException {
    final String intentStr = record.getMetadata().getIntent().name();
    IncidentRecordValueImpl recordValue = (IncidentRecordValueImpl)record.getValue();

    //update activity instance
    bulkRequestBuilder.add(persistActivityInstanceFromIncident(record, intentStr, recordValue));

  }

  public void processWorkflowInstanceRecord(Record record, BulkRequestBuilder bulkRequestBuilder) throws PersistenceException {

    final String intentStr = record.getMetadata().getIntent().name();
    WorkflowInstanceRecordValueImpl recordValue = (WorkflowInstanceRecordValueImpl)record.getValue();

    if (!isProcessEvent(recordValue) && !isOfType(recordValue, BpmnElementType.SEQUENCE_FLOW)){
      bulkRequestBuilder.add(persistActivityInstance(record, intentStr, recordValue));
    }
  }

  private UpdateRequestBuilder persistActivityInstanceFromIncident(Record record, String intentStr, IncidentRecordValueImpl recordValue) throws PersistenceException {
    ActivityInstanceForDetailViewEntity entity = new ActivityInstanceForDetailViewEntity();
    entity.setId(IdUtil.getId(recordValue.getElementInstanceKey(), record));
    entity.setKey(recordValue.getElementInstanceKey());
    entity.setPartitionId(record.getMetadata().getPartitionId());
    entity.setActivityId(recordValue.getElementId());
    entity.setWorkflowInstanceId(IdUtil.getId(recordValue.getWorkflowInstanceKey(), record));
    if (intentStr.equals(IncidentIntent.CREATED.name())) {
      entity.setIncidentKey(record.getKey());
    } else if (intentStr.equals(IncidentIntent.CREATED.name())) {
      entity.setIncidentKey(null);
    }

    //set parent
    String workflowInstanceId = IdUtil.getId(recordValue.getWorkflowInstanceKey(), record);

    return getActivityInstanceFromIncidentQuery(entity, workflowInstanceId);
  }

  private UpdateRequestBuilder persistActivityInstance(Record record, String intentStr, WorkflowInstanceRecordValueImpl recordValue) throws PersistenceException {
    ActivityInstanceForDetailViewEntity entity = new ActivityInstanceForDetailViewEntity();
    entity.setId(IdUtil.getId(record.getKey(), record));
    entity.setPartitionId(record.getMetadata().getPartitionId());
    entity.setActivityId(recordValue.getElementId());
    entity.setWorkflowInstanceId(IdUtil.getId(recordValue.getWorkflowInstanceKey(), record));
    entity.setScopeId(IdUtil.getId(recordValue.getScopeInstanceKey(), record));

    boolean activityFinished = AI_FINISH_STATES.contains(intentStr);
    if (!activityFinished && intentStr.equals(EVENT_ACTIVATED.name()) && isEndEvent(recordValue)) {
      activityFinished = true;
    }
    if (activityFinished) {
      if (intentStr.equals(ELEMENT_TERMINATED.name())) {
        entity.setState(ActivityState.TERMINATED);
      } else {
        entity.setState(ActivityState.COMPLETED);
      }
      entity.setEndDate(DateUtil.toOffsetDateTime(record.getTimestamp()));
    } else {
      entity.setState(ActivityState.ACTIVE);
      //TODO fix this for gateways and start events, when new event flow is ready
      if (AI_START_STATES.contains(intentStr)
        || (intentStr.equals(EVENT_TRIGGERING.name()) && isOfType(recordValue, BpmnElementType.START_EVENT))
        || intentStr.equals(GATEWAY_ACTIVATED.name())) {
        entity.setStartDate(DateUtil.toOffsetDateTime(record.getTimestamp()));
        entity.setPosition(record.getPosition());
      }
    }

    entity.setType(ActivityType.fromZeebeBpmnElementType(recordValue.getBpmnElementType()));

    //set parent
    String workflowInstanceId = IdUtil.getId(recordValue.getWorkflowInstanceKey(), record);

    return getActivityInstanceQuery(entity, workflowInstanceId);

  }

  private UpdateRequestBuilder getActivityInstanceQuery(ActivityInstanceForDetailViewEntity entity, String workflowInstanceId) throws PersistenceException {
    try {
      logger.debug("Activity instance for list view: id {}", entity.getId());
      Map<String, Object> updateFields = new HashMap<>();
      updateFields.put(ActivityInstanceTemplate.ID, entity.getId());
      updateFields.put(ActivityInstanceTemplate.PARTITION_ID, entity.getPartitionId());
      updateFields.put(ActivityInstanceTemplate.TYPE, entity.getType());
      updateFields.put(ActivityInstanceTemplate.STATE, entity.getState());
      updateFields.put(ActivityInstanceTemplate.SCOPE_ID, entity.getScopeId());
      if (entity.getStartDate() != null) {
        updateFields.put(ActivityInstanceTemplate.START_DATE, entity.getStartDate());
      }
      if (entity.getEndDate() != null) {
        updateFields.put(ActivityInstanceTemplate.END_DATE, entity.getEndDate());
      }
      if (entity.getPosition() != null) {
        updateFields.put(ActivityInstanceTemplate.POSITION, entity.getPosition());
      }

      //TODO some weird not efficient magic is needed here, in order to format date fields properly, may be this can be improved
      Map<String, Object> jsonMap = objectMapper.readValue(objectMapper.writeValueAsString(updateFields), HashMap.class);

      return esClient
        .prepareUpdate(activityInstanceTemplate.getAlias(), ElasticsearchUtil.ES_INDEX_TYPE, entity.getId())
        .setUpsert(objectMapper.writeValueAsString(entity), XContentType.JSON)
        .setDoc(jsonMap)
        .setRouting(workflowInstanceId);

    } catch (IOException e) {
      logger.error("Error preparing the query to upsert activity instance for list view", e);
      throw new PersistenceException(String.format("Error preparing the query to upsert activity instance [%s]  for list view", entity.getId()), e);
    }
  }

  private UpdateRequestBuilder getActivityInstanceFromIncidentQuery(ActivityInstanceForDetailViewEntity entity, String workflowInstanceId) throws PersistenceException {
    try {
      logger.debug("Activity instance for list view: id {}", entity.getId());
      Map<String, Object> updateFields = new HashMap<>();
      updateFields.put(ActivityInstanceTemplate.INCIDENT_KEY, entity.getIncidentKey());

      //TODO some weird not efficient magic is needed here, in order to format date fields properly, may be this can be improved
      Map<String, Object> jsonMap = objectMapper.readValue(objectMapper.writeValueAsString(updateFields), HashMap.class);

      return esClient
        .prepareUpdate(activityInstanceTemplate.getAlias(), ElasticsearchUtil.ES_INDEX_TYPE, entity.getId())
        .setUpsert(objectMapper.writeValueAsString(entity), XContentType.JSON)
        .setDoc(jsonMap)
        .setRouting(workflowInstanceId);

    } catch (IOException e) {
      logger.error("Error preparing the query to upsert activity instance for list view", e);
      throw new PersistenceException(String.format("Error preparing the query to upsert activity instance [%s]  for list view", entity.getId()), e);
    }
  }

  private boolean isProcessEvent(WorkflowInstanceRecordValueImpl recordValue) {
    return isOfType(recordValue, BpmnElementType.PROCESS);
  }

  private boolean isEndEvent(WorkflowInstanceRecordValueImpl recordValue) {
    return isOfType(recordValue, BpmnElementType.END_EVENT);
  }

  private boolean isOfType(WorkflowInstanceRecordValueImpl recordValue, BpmnElementType type) {
    final BpmnElementType bpmnElementType = recordValue.getBpmnElementType();
    if (bpmnElementType == null) {
      return false;
    }
    return bpmnElementType.equals(type);
  }

}
