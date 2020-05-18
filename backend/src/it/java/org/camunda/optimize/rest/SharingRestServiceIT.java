/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.dto.optimize.query.sharing.DashboardShareDto;
import org.camunda.optimize.dto.optimize.query.sharing.ReportShareDto;
import org.camunda.optimize.service.sharing.AbstractSharingIT;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SharingRestServiceIT extends AbstractSharingIT {

  @Test
  public void checkShareStatusWithoutAuthentication() {
    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildCheckSharingStatusRequest(null)
      .withoutAuthentication()
      .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void createNewReportShareWithoutAuthentication() {
    // when
    Response response =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildShareReportRequest(null)
        .withoutAuthentication()
        .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void createNewDashboardShareWithoutAuthentication() {
    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildShareDashboardRequest(null)
      .withoutAuthentication()
      .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void createNewReportShare() {
    //given
    String reportId = createReport();
    ReportShareDto share = createReportShare(reportId);

    // when
    Response response = sharingClient.createReportShareResponse(share);

    // then
    assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    String id =
        response.readEntity(String.class);
    assertThat(id, is(notNullValue()));
  }

  @Test
  public void createNewReportShareWithSharingDisabled() {
    //given
    String reportId = createReport();
    embeddedOptimizeExtension.getConfigurationService().setSharingEnabled(false);
    ReportShareDto share = createReportShare(reportId);

    // when
    Response response = sharingClient.createReportShareResponse(share);

    // then
    assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
  }

  @Test
  public void createNewDashboardShare() {
    //given
    String dashboard = addEmptyDashboardToOptimize();

    DashboardShareDto sharingDto = new DashboardShareDto();
    sharingDto.setDashboardId(dashboard);

    // when
    Response response = sharingClient.createDashboardShareResponse(sharingDto);

    // then
    assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    String id =
      response.readEntity(String.class);
    assertThat(id, is(notNullValue()));
  }

  @Test
  public void deleteReportShareWithoutAuthentication() {
    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildDeleteReportShareRequest("1124")
      .withoutAuthentication()
      .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void deleteNonExistingReportShare() {
    // when
    Response response = embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteReportShareRequest("nonExistingId")
            .execute();

    // then
    assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
  }

  @Test
  public void deleteDashboardShareWithoutAuthentication() {
    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildDeleteDashboardShareRequest("1124")
      .withoutAuthentication()
      .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void deleteNonExistingDashboardShare() {
    // when
    Response response = embeddedOptimizeExtension
            .getRequestExecutor()
            .buildDeleteDashboardShareRequest("nonExistingId")
            .execute();

    // then
    assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
  }

  @Test
  public void deleteReportShare() {
    //given
    String reportId = createReport();
    String id = addShareForReport(reportId);

    // when
    Response response =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDeleteReportShareRequest(id)
        .execute();

    // then the status code is okay
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    assertThat(getShareForReport(FAKE_REPORT_ID), is(nullValue()));
  }

  @Test
  public void deleteDashboardShare() {
    //given
    String reportId = createReport();
    String dashboardWithReport = createDashboardWithReport(reportId);
    String id = addShareForDashboard(dashboardWithReport);

    // when
    Response response =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDeleteDashboardShareRequest(id)
        .execute();

    // then the status code is okay
    assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    assertThat(getShareForReport(reportId), is(nullValue()));
  }

  @Test
  public void findShareForReport() {
    //given
    String reportId = createReport();
    String id = addShareForReport(reportId);

    //when
    ReportShareDto share = getShareForReport(reportId);

    //then
    assertThat(share, is(notNullValue()));
    assertThat(share.getId(), is(id));
  }

  @Test
  public void findShareForReportWithoutAuthentication() {
    //given
    addShareForFakeReport();

    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildFindShareForReportRequest(FAKE_REPORT_ID)
      .withoutAuthentication()
      .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void findShareForSharedDashboard() {
    //given
    String reportId = createReport();
    String dashboardWithReport = createDashboardWithReport(reportId);
    String id = addShareForDashboard(dashboardWithReport);

    //when
    DashboardShareDto share = findShareForDashboard(dashboardWithReport).readEntity(DashboardShareDto.class);

    //then
    assertThat(share, is(notNullValue()));
    assertThat(share.getId(), is(id));
  }

  @Test
  public void evaluateSharedDashboard() {
    //given
    String reportId = createReport();
    String dashboardId = createDashboardWithReport(reportId);
    String dashboardShareId = addShareForDashboard(dashboardId);

    //when
    DashboardDefinitionDto dashboardShareDto = sharingClient.evaluateDashboard(dashboardShareId);

    //then
    assertThat(dashboardShareDto, is(notNullValue()));
    assertThat(dashboardShareDto.getId(), is(dashboardId));
    assertThat(dashboardShareDto.getReports(), is(notNullValue()));
    assertThat(dashboardShareDto.getReports().size(), is(1));

    // when
    String reportShareId = dashboardShareDto.getReports().get(0).getId();
    HashMap<?, ?> evaluatedReportAsMap =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildEvaluateSharedDashboardReportRequest(dashboardShareId, reportShareId)
        .execute(HashMap.class, Response.Status.OK.getStatusCode());

    // then
    assertReportData(reportId, evaluatedReportAsMap);
  }

  @Test
  public void findShareForDashboardWithoutAuthentication() {
    //given
    String reportId = createReport();
    String dashboardWithReport = createDashboardWithReport(reportId);
    addShareForDashboard(dashboardWithReport);

    //when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildFindShareForDashboardRequest(dashboardWithReport)
      .withoutAuthentication()
      .execute();

    // then
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void evaluationOfNotExistingShareReturnsError() {

    //when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildEvaluateSharedReportRequest("123")
      .execute();

    //then
    assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  @Test
  public void checkSharingAuthorizationWithoutAuthentication() {
    // when
    Response response =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDashboardShareAuthorizationCheck("1124")
        .withoutAuthentication()
        .execute();

    // then the status code is not authorized
    assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void checkSharingAuthorizationIsOkay() {
    //given
    String reportId = createReport();
    String dashboardId = createDashboardWithReport(reportId);

    // when
    Response response =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDashboardShareAuthorizationCheck(dashboardId)
        .execute();

    // then the status code is okay
    assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
  }

  @Test
  public void checkSharingAuthorizationResultsInForbidden() {
    //given
    engineIntegrationExtension.addUser("kermit", "kermit");
    engineIntegrationExtension.grantUserOptimizeAccess("kermit");
    String reportId = createReport();
    String dashboardId = createDashboardWithReport(reportId);

    // when
    Response response =
      embeddedOptimizeExtension
        .getRequestExecutor()
        .buildDashboardShareAuthorizationCheck(dashboardId)
        .withUserAuthentication("kermit", "kermit")
        .execute();

    // then the status code is okay
    assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()));
  }

  private Response findShareForDashboard(String dashboardId) {
    return embeddedOptimizeExtension
      .getRequestExecutor()
      .buildFindShareForDashboardRequest(dashboardId)
      .execute();
  }

  private void addShareForFakeReport() {
    addShareForReport(FAKE_REPORT_ID);
  }

}
