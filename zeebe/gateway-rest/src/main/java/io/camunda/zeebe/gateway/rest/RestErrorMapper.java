/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest;

import static io.camunda.zeebe.transport.stream.impl.messages.ErrorCode.NOT_FOUND;

import com.fasterxml.jackson.core.JsonParseException;
import io.atomix.cluster.messaging.MessagingException;
import io.camunda.document.api.DocumentError.DocumentNotFound;
import io.camunda.document.api.DocumentError.InvalidInput;
import io.camunda.document.api.DocumentError.OperationNotSupported;
import io.camunda.service.CamundaServiceException;
import io.camunda.service.DocumentServices.DocumentException;
import io.camunda.service.exception.NotFoundException;
import io.camunda.zeebe.broker.client.api.BrokerErrorException;
import io.camunda.zeebe.broker.client.api.BrokerRejectionException;
import io.camunda.zeebe.broker.client.api.PartitionNotFoundException;
import io.camunda.zeebe.broker.client.api.RequestRetriesExhaustedException;
import io.camunda.zeebe.broker.client.api.dto.BrokerError;
import io.camunda.zeebe.broker.client.api.dto.BrokerRejection;
import io.camunda.zeebe.msgpack.spec.MsgpackException;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public class RestErrorMapper {

  public static final Function<BrokerRejection, ProblemDetail> DEFAULT_REJECTION_MAPPER =
      rejection -> {
        final String message =
            String.format(
                "Command '%s' rejected with code '%s': %s",
                rejection.intent(), rejection.type(), rejection.reason());
        final String title = rejection.type().name();
        return switch (rejection.type()) {
          case NOT_FOUND:
            yield RestErrorMapper.createProblemDetail(HttpStatus.NOT_FOUND, message, title);
          case INVALID_STATE:
          case ALREADY_EXISTS:
            yield RestErrorMapper.createProblemDetail(HttpStatus.CONFLICT, message, title);
          case INVALID_ARGUMENT:
            yield RestErrorMapper.createProblemDetail(HttpStatus.BAD_REQUEST, message, title);
          default:
            {
              yield RestErrorMapper.createProblemDetail(
                  HttpStatus.INTERNAL_SERVER_ERROR, message, title);
            }
        };
      };
  private static final Logger REST_GATEWAY_LOGGER =
      LoggerFactory.getLogger("io.camunda.zeebe.gateway.rest");

  public static <T> Optional<ResponseEntity<T>> getResponse(
      final Throwable error, final Function<BrokerRejection, ProblemDetail> rejectionMapper) {
    return Optional.ofNullable(error)
        .map(e -> mapErrorToProblem(e, rejectionMapper))
        .or(() -> mapBrokerErrorToProblem(error))
        .or(() -> mapRejectionToProblem(error, rejectionMapper))
        .map(RestErrorMapper::mapProblemToResponse);
  }

  public static ProblemDetail mapErrorToProblem(
      final Throwable error, final Function<BrokerRejection, ProblemDetail> rejectionMapper) {
    if (error == null) {
      return null;
    }
    return switch (error) {
      case final NotFoundException nfe:
        yield createProblemDetail(
            HttpStatus.NOT_FOUND, nfe.getMessage(), RejectionType.NOT_FOUND.name());
      case final CamundaServiceException cse:
        yield cse.getCause() != null ? mapErrorToProblem(cse.getCause(), rejectionMapper) : null;
      case final BrokerErrorException bee:
        yield mapBrokerErrorToProblem(bee.getError(), error);
      case final DocumentException de:
        yield mapDocumentHandlingExceptionToProblem(de);
      case final BrokerRejectionException bre:
        REST_GATEWAY_LOGGER.trace(
            "Expected to handle REST request, but the broker rejected it", error);
        yield rejectionMapper.apply(bre.getRejection());
      case final ExecutionException ee:
        yield mapErrorToProblem(ee.getCause(), rejectionMapper);
      case final CompletionException ce:
        yield mapErrorToProblem(ce.getCause(), rejectionMapper);
      case final MsgpackException mpe:
        final var mpeMsg =
            "Expected to handle REST API request, but messagepack property was invalid";
        REST_GATEWAY_LOGGER.debug(mpeMsg, mpe);
        yield createProblemDetail(HttpStatus.BAD_REQUEST, mpeMsg, mpe.getClass().getName());
      case final JsonParseException jpe:
        final var jpeMsg = "Expected to handle REST API request, but JSON property was invalid";
        REST_GATEWAY_LOGGER.debug(jpeMsg, jpe);
        yield createProblemDetail(HttpStatus.BAD_REQUEST, jpeMsg, jpe.getClass().getName());
      case final IllegalArgumentException iae:
        final var iaeMsg = "Expected to handle REST API request, but JSON property was invalid";
        REST_GATEWAY_LOGGER.debug(iaeMsg, iae);
        yield createProblemDetail(HttpStatus.BAD_REQUEST, iaeMsg, iae.getClass().getName());
      case final RequestRetriesExhaustedException rree:
        final var rreeMsg =
            "Expected to handle REST API request, but all retries have been exhausted";
        REST_GATEWAY_LOGGER.trace(rreeMsg, rree);
        yield createProblemDetail(HttpStatus.TOO_MANY_REQUESTS, rreeMsg, rree.getClass().getName());
      case final TimeoutException te:
        final var teMsg =
            "Expected to handle REST API request, but request timed out between gateway and broker";
        REST_GATEWAY_LOGGER.debug(teMsg, te);
        yield createProblemDetail(HttpStatus.GATEWAY_TIMEOUT, teMsg, te.getClass().getName());
      case final MessagingException.ConnectionClosed cc:
        final var ccMsg =
            "Expected to handle REST API request, but the connection was cut prematurely with the broker; "
                + "the request may or may not have been accepted, and may not be safe to retry";
        REST_GATEWAY_LOGGER.warn(ccMsg, cc);
        yield createProblemDetail(HttpStatus.BAD_GATEWAY, ccMsg, cc.getClass().getName());
      case final ConnectTimeoutException cte:
        final var cteMsg =
            "Expected to handle REST API request, but a connection timeout exception occurred";
        REST_GATEWAY_LOGGER.warn(cteMsg, cte);
        yield createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, cteMsg, cte.getClass().getName());
      case final ConnectException ce:
        final var ceMsg =
            "Expected to handle REST API request, but there was a connection error with one of the brokers";
        REST_GATEWAY_LOGGER.warn(ceMsg, ce);
        yield createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, ceMsg, ce.getClass().getName());
      case final PartitionNotFoundException pnfe:
        final var pnfeMsg =
            "Expected to handle REST API request, but request could not be delivered";
        REST_GATEWAY_LOGGER.debug(pnfeMsg, pnfe);
        yield createProblemDetail(
            HttpStatus.SERVICE_UNAVAILABLE, pnfeMsg, pnfe.getClass().getName());
      default:
        REST_GATEWAY_LOGGER.error(
            "Expected to handle REST request, but an unexpected error occurred", error);
        yield createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected error occurred during the request processing: " + error.getMessage(),
            error.getClass().getName());
    };
  }

  private static Optional<ProblemDetail> mapBrokerErrorToProblem(final Throwable exception) {
    if (!(exception instanceof CamundaServiceException)) {
      return Optional.empty();
    }
    return ((CamundaServiceException) exception)
        .getBrokerError()
        .map(error -> mapBrokerErrorToProblem(error, null));
  }

  private static ProblemDetail mapBrokerErrorToProblem(
      final BrokerError error, final Throwable rootError) {
    if (error == null) {
      return null;
    }
    String message = error.getMessage();
    final String title = error.code().name();

    return switch (error.getCode()) {
      case PROCESS_NOT_FOUND -> createProblemDetail(HttpStatus.NOT_FOUND, message, title);
      case RESOURCE_EXHAUSTED -> {
        REST_GATEWAY_LOGGER.trace("Target broker is currently overloaded: {}", error, rootError);
        yield createProblemDetail(HttpStatus.TOO_MANY_REQUESTS, message, title);
      }
      case PARTITION_LEADER_MISMATCH -> {
        // return 503 to indicate to the user that retrying might solve the issue, as this
        // is usually a transient issue
        REST_GATEWAY_LOGGER.trace(
            "Target broker was not the leader of the partition: {}", error, rootError);
        yield createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, message, title);
      }
      default -> {
        // all the following are for cases where retrying (with the same gateway) is not
        // expected
        // to solve anything
        REST_GATEWAY_LOGGER.error(
            "Expected to handle REST request, but received an internal error from broker: {}",
            error,
            rootError);
        message =
            String.format(
                "Received an unexpected error from the broker, code: %s, message: %s",
                error.getCode(), error.getMessage());
        yield createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, message, title);
      }
    };
  }

  private static Optional<ProblemDetail> mapRejectionToProblem(
      final Throwable exception, final Function<BrokerRejection, ProblemDetail> rejectionMapper) {
    if (!(exception instanceof CamundaServiceException)) {
      return Optional.empty();
    }
    return ((CamundaServiceException) exception).getBrokerRejection().map(rejectionMapper);
  }

  public static ProblemDetail createProblemDetail(
      final HttpStatusCode status, final String detail, final String title) {
    final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(title);
    return problemDetail;
  }

  public static <T> ResponseEntity<T> mapProblemToResponse(final ProblemDetail problemDetail) {
    return ResponseEntity.of(problemDetail)
        .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON))
        .build();
  }

  public static <T> CompletableFuture<ResponseEntity<T>> mapProblemToCompletedResponse(
      final ProblemDetail problemDetail) {
    return CompletableFuture.completedFuture(RestErrorMapper.mapProblemToResponse(problemDetail));
  }

  public static ResponseEntity<Object> mapUserManagementExceptionsToResponse(final Exception e) {
    if (e instanceof IllegalArgumentException) {
      final var problemDetail =
          createProblemDetail(HttpStatus.BAD_REQUEST, e.getMessage(), e.getClass().getName());
      return mapProblemToResponse(problemDetail);
    }

    final var problemDetail =
        createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e.getClass().getName());
    return mapProblemToResponse(problemDetail);
  }

  public static ProblemDetail mapDocumentHandlingExceptionToProblem(final DocumentException e) {
    final var status =
        switch (e.getDocumentError()) {
          case final DocumentNotFound ignored -> HttpStatus.NOT_FOUND;
          case final InvalidInput ignored -> HttpStatus.BAD_REQUEST;
          case final OperationNotSupported ignored -> HttpStatus.NOT_IMPLEMENTED;
          default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    return createProblemDetail(status, e.getMessage(), e.getDocumentError().getClass().getName());
  }

  public static ResponseEntity<Object> mapDocumentHandlingExceptionToResponse(
      final DocumentException e) {
    return mapProblemToResponse(mapDocumentHandlingExceptionToProblem(e));
  }
}
