/*
 * Copyright 2019 Telefonaktiebolaget LM Ericsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ericsson.bss.cassandra.ecaudit.integration.allowallauthorizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.servererrors.ServerError;
import com.ericsson.bss.cassandra.ecaudit.logger.Slf4jAuditLogger;
import com.ericsson.bss.cassandra.ecaudit.test.daemon.CassandraDaemonForAuditTest;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class ITAllowAllAuthorizer {
  private static CqlSession session;

  @Captor private ArgumentCaptor<ILoggingEvent> loggingEventCaptor;

  @Mock private Appender<ILoggingEvent> mockAuditAppender;

  @BeforeClass
  public static void beforeClass() throws Exception {
    CassandraDaemonForAuditTest cdt = CassandraDaemonForAuditTest.getInstance();
    session = cdt.createSession();
  }

  @Before
  public void before() {
    LoggerContext loggerContext =
        (LoggerContext)LoggerFactory.getILoggerFactory();
    loggerContext.getLogger(Slf4jAuditLogger.AUDIT_LOGGER_NAME)
        .addAppender(mockAuditAppender);
  }

  @After
  public void after() {
    verifyNoMoreInteractions(mockAuditAppender);
    LoggerContext loggerContext =
        (LoggerContext)LoggerFactory.getILoggerFactory();
    loggerContext.getLogger(Slf4jAuditLogger.AUDIT_LOGGER_NAME)
        .detachAppender(mockAuditAppender);
  }

  @AfterClass
  public static void afterClass() {
    session.close();
  }

  @Test
  public void testBasicStatement() {
    givenTable("school", "students");
    reset(mockAuditAppender);

    session.execute(
        "INSERT INTO school.students (key, value) VALUES (42, 'Kalle')");

    assertThat(getLogEntries())
        .containsOnly(
            "client:'127.0.0.1'|user:'cassandra'|status:'ATTEMPT'|operation:'"
            + "INSERT INTO school.students (key, value) VALUES (42, 'Kalle')'");
  }

  @Test
  public void testGrantFails() {
    givenTable("company", "engineers");
    reset(mockAuditAppender);

    assertThatExceptionOfType(ServerError.class)
        .isThrownBy(
            ()
                -> session.execute(
                    "GRANT SELECT ON TABLE company.engineers TO cassandra"));

    assertThat(getLogEntries())
        .containsOnly(
            "client:'127.0.0.1'|user:'cassandra'|status:'ATTEMPT'|operation:'"
                + "GRANT SELECT ON TABLE company.engineers TO cassandra'",
            "client:'127.0.0.1'|user:'cassandra'|status:'FAILED'|operation:'"
                + "GRANT SELECT ON TABLE company.engineers TO cassandra'");
  }

  private void givenTable(String keyspace, String table) {
    session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                    (" WITH REPLICATION = {'class' : 'SimpleStrategy', "
                     + "'replication_factor' : 1} AND DURABLE_WRITES = false"));
    session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + "." + table +
                    " (key int PRIMARY KEY, value text)");
  }

  private List<String> getLogEntries() {
    verify(mockAuditAppender, atLeast(1))
        .doAppend(loggingEventCaptor.capture());
    return loggingEventCaptor.getAllValues()
        .stream()
        .map(ILoggingEvent::getFormattedMessage)
        .collect(Collectors.toList());
  }
}
