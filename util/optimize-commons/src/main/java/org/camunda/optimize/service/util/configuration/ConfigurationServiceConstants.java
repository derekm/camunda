/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.util.configuration;

/**
 * This interface should contain constants only in order not to have magic
 * string inlined in the service class.
 */
public interface ConfigurationServiceConstants {
  String TOKEN_LIFE_TIME = "$.security.auth.token.lifeMin";
  String TOKEN_SECRET = "$.security.auth.token.secret";
  String SAME_SITE_COOKIE_FLAG_ENABLED = "$.security.auth.cookie.same-site.enabled";
  String SUPER_USER_IDS = "$.security.auth.superUserIds";

  String RESPONSE_HEADER_HSTS_MAX_AGE = "$.security.responseHeaders.HSTS.max-age";
  String RESPONSE_HEADER_HSTS_INCLUDE_SUBDOMAINS = "$.security.responseHeaders.HSTS.includeSubDomains";
  String RESPONSE_HEADER_X_XSS_PROTECTION = "$.security.responseHeaders.X-XSS-Protection";
  String RESPONSE_HEADER_X_CONTENT_TYPE_OPTIONS = "$.security.responseHeaders.X-Content-Type-Options";
  String RESPONSE_HEADER_CONTENT_SECURITY_POLICY = "$.security.responseHeaders.Content-Security-Policy";

  String CONFIGURED_ENGINES = "$.engines";

  String QUARTZ_JOB_STORE_CLASS = "$.alerting.quartz.jobStore";

  String EMAIL_ADDRESS = "$.email.address";
  String EMAIL_ENABLED = "$.email.enabled";
  String EMAIL_HOSTNAME = "$.email.hostname";
  String EMAIL_PORT = "$.email.port";

  String EMAIL_AUTHENTICATION = "$.email.authentication";

  String CONFIGURED_WEBHOOKS = "$.webhookAlerting.webhooks";

  String ELASTICSEARCH_MAX_JOB_QUEUE_SIZE = "$.import.elasticsearchJobExecutorQueueSize";
  String ELASTICSEARCH_IMPORT_EXECUTOR_THREAD_COUNT = "$.import.elasticsearchJobExecutorThreadCount";

  String IMPORT_CURRENT_TIME_BACKOFF_MILLISECONDS = "$.import.currentTimeBackoffMilliseconds";
  String ENGINE_IMPORT_PROCESS_DEFINITION_XML_MAX_PAGE_SIZE = "$.import.data.process-definition-xml.maxPageSize";
  String ENGINE_IMPORT_PROCESS_DEFINITION_MAX_PAGE_SIZE = "$.import.data.process-definition.maxPageSize";
  String ENGINE_IMPORT_ACTIVITY_INSTANCE_MAX_PAGE_SIZE = "$.import.data.activity-instance.maxPageSize";
  String ENGINE_IMPORT_USER_TASK_INSTANCE_MAX_PAGE_SIZE = "$.import.data.user-task-instance.maxPageSize";
  String ENGINE_IMPORT_IDENTITY_LING_LOG_MAX_PAGE_SIZE = "$.import.data.identity-link-log.maxPageSize";
  String ENGINE_IMPORT_USER_OPERATION_LOG_MAX_PAGE_SIZE = "$.import.data.user-operation-log.maxPageSize";
  String ENGINE_IMPORT_PROCESS_INSTANCE_MAX_PAGE_SIZE = "$.import.data.process-instance.maxPageSize";
  String ENGINE_IMPORT_VARIABLE_INSTANCE_MAX_PAGE_SIZE = "$.import.data.variable.maxPageSize";
  String ENGINE_IMPORT_DECISION_DEFINITION_MAX_PAGE_SIZE = "$.import.data.decision-definition.maxPageSize";
  String ENGINE_IMPORT_DECISION_DEFINITION_XML_MAX_PAGE_SIZE = "$.import.data.decision-definition-xml.maxPageSize";
  String ENGINE_IMPORT_DECISION_INSTANCE_MAX_PAGE_SIZE = "$.import.data.decision-instance.maxPageSize";
  String ENGINE_IMPORT_TENANT_MAX_PAGE_SIZE = "$.import.data.tenant.maxPageSize";
  String ENGINE_IMPORT_GROUP_MAX_PAGE_SIZE = "$.import.data.group.maxPageSize";
  String ENGINE_IMPORT_AUTHORIZATION_MAX_PAGE_SIZE = "$.import.data.authorization.maxPageSize";
  String IMPORT_DMN_DATA = "$.import.data.dmn.enabled";
  String IMPORT_USER_TASK_WORKER_DATA = "$.import.data.user-task-worker.enabled";

  String PLUGIN_BASE_DIRECTORY = "$.plugin.directory";
  String VARIABLE_IMPORT_PLUGIN_BASE_PACKAGES = "$.plugin.variableImport.basePackages";
  String ENGINE_REST_FILTER_PLUGIN_BASE_PACKAGES = "$.plugin.engineRestFilter.basePackages";
  String AUTHENTICATION_EXTRACTOR_BASE_PACKAGES = "$.plugin.authenticationExtractor.basePackages";
  String DECISION_INPUT_IMPORT_PLUGIN_BASE_PACKAGES = "$.plugin.decisionInputImport.basePackages";
  String DECISION_OUTPUT_IMPORT_PLUGIN_BASE_PACKAGES = "$.plugin.decisionOutputImport.basePackages";
  String BUSINESS_KEY_IMPORT_PLUGIN_BASE_PACKAGES = "$.plugin.businessKeyImport.basePackages";
  String ELASTIC_SEARCH_CONNECTION_TIMEOUT = "$.es.connection.timeout";
  String ELASTIC_SEARCH_SCROLL_TIMEOUT = "$.es.scrollTimeout";
  String ELASTIC_SEARCH_CONNECTION_NODES = "$.es.connection.nodes";
  String ELASTIC_SEARCH_PROXY = "$.es.connection.proxy";


  String ELASTIC_SEARCH_SECURITY_USERNAME = "$.es.security.username";
  String ELASTIC_SEARCH_SECURITY_PASSWORD = "$.es.security.password";
  String ELASTIC_SEARCH_SECURITY_SSL_ENABLED = "$.es.security.ssl.enabled";
  String ELASTIC_SEARCH_SECURITY_SSL_CERTIFICATE = "$.es.security.ssl.certificate";
  String ELASTIC_SEARCH_SECURITY_SSL_CERTIFICATE_AUTHORITIES = "$.es.security.ssl.certificate_authorities";

  String IMPORT_INDEX_AUTO_STORAGE_INTERVAL = "$.import.importIndexStorageIntervalInSec";

  String ENGINE_CONNECT_TIMEOUT = "$.engine-commons.connection.timeout";
  String ENGINE_READ_TIMEOUT = "$.engine-commons.read.timeout";
  String PROCESS_DEFINITION_ENDPOINT = "$.engine-commons.procdef.resource";
  String PROCESS_DEFINITION_XML_ENDPOINT = "$.engine-commons.procdef.xml";
  String USER_VALIDATION_ENDPOINT = "$.engine-commons.user.validation.resource";

  String DECISION_DEFINITION_ENDPOINT = "$.engine-commons.decision-definition.resource";
  String DECISION_DEFINITION_XML_ENDPOINT = "$.engine-commons.decision-definition.xml";

  String INITIAL_BACKOFF_INTERVAL = "$.import.handler.backoff.initial";
  String MAXIMUM_BACK_OFF = "$.import.handler.backoff.max";
  String ES_AGGREGATION_BUCKET_LIMIT = "$.es.settings.aggregationBucketLimit";
  String ES_REFRESH_INTERVAL = "$.es.settings.index.refresh_interval";
  String ES_NUMBER_OF_REPLICAS = "$.es.settings.index.number_of_replicas";
  String ES_NUMBER_OF_SHARDS = "$.es.settings.index.number_of_shards";
  String ES_INDEX_PREFIX = "$.es.settings.index.prefix";

  String ENGINE_DATE_FORMAT = "$.serialization.engineDateFormat";
  String CONTAINER_HOST = "$.container.host";
  String CONTAINER_KEYSTORE_PASSWORD = "$.container.keystore.password";
  String CONTAINER_KEYSTORE_LOCATION = "$.container.keystore.location";
  String CONTAINER_HTTPS_PORT = "$.container.ports.https";
  String CONTAINER_HTTP_PORT = "$.container.ports.http";
  String CONTAINER_STATUS_MAX_CONNECTIONS = "$.container.status.connections.max";

  String CONTAINER_ACCESSURL = "$.container.accessUrl";

  String EXPORT_CSV_LIMIT = "$.export.csv.limit";

  String HISTORY_CLEANUP = "$.historyCleanup";

  String SHARING_ENABLED = "$.sharing.enabled";

  String AVAILABLE_LOCALES = "$.locales.availableLocales";
  String FALLBACK_LOCALE = "$.locales.fallbackLocale";

  String UI_CONFIGURATION = "$.ui";

  String IDENTITY_SYNC_CONFIGURATION = "$.import.identitySync";

  String EVENT_INDEX_ROLLOVER_CONFIGURATION = "$.eventBasedProcess.eventIndexRollover";

  String EVENT_BASED_PROCESS_CONFIGURATION = "$.eventBasedProcess";
}
