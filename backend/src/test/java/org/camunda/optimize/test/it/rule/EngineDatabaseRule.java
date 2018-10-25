package org.camunda.optimize.test.it.rule;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Properties;

public class EngineDatabaseRule extends TestWatcher {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private static final String DATABASE_H2 = "h2";
  private static final String DATABASE_POSTGRESQL = "postgresql";

  private static final String JDBC_DRIVER_H2 = "org.h2.Driver";
  private static final String DB_URL_H2 = "jdbc:h2:tcp://localhost:9092/mem:camunda1";
  private static final String USER_H2 = "sa";
  private static final String PASS_H2 = "";

  private static final String JDBC_DRIVER_POSTGRESQL = "org.postgresql.Driver";
  private static final String DB_URL_POSTGRESQL = "jdbc:postgresql://localhost:5432/engine";
  private static final String USER_POSTGRESQL = "camunda";
  private static final String PASS_POSTGRESQL = "camunda";

  private static Connection connection = null;

  private String database = System.getProperty("database", "h2");
  private Boolean usePostgresOptimizations = true;

  public EngineDatabaseRule(Properties properties) {
    database = properties.getProperty("db.name");
    usePostgresOptimizations = Boolean.valueOf(properties.getProperty("db.usePostgresOptimizations"));
    String jdbcDriver = properties.getProperty("db.jdbc.driver");
    String dbUrl = properties.getProperty("db.url");
    if (dbUrl == null || dbUrl.isEmpty() || dbUrl.startsWith("${")) {
      dbUrl = properties.getProperty("db.url.default");
    }
    String dbUser = properties.getProperty("db.username");
    String dbPassword = properties.getProperty("db.password");
    initDatabaseConnection(jdbcDriver, dbUrl, dbUser, dbPassword);
  }

  public EngineDatabaseRule() {
    String jdbcDriver;
    String dbUrl;
    String dbUser;
    String dbPassword;

    switch (database) {
      case DATABASE_H2:
        jdbcDriver = JDBC_DRIVER_H2;
        dbUrl = DB_URL_H2;
        dbUser = USER_H2;
        dbPassword = PASS_H2;
        break;
      case DATABASE_POSTGRESQL:
        jdbcDriver = JDBC_DRIVER_POSTGRESQL;
        dbUrl = DB_URL_POSTGRESQL;
        dbUser = USER_POSTGRESQL;
        dbPassword = PASS_POSTGRESQL;
        break;
      default:
        throw new IllegalArgumentException("Unable to discover database " + database);
    }
    initDatabaseConnection(jdbcDriver, dbUrl, dbUser, dbPassword);
  }

  private void initDatabaseConnection(String jdbcDriver, String dbUrl, String dbUser, String dbPassword) {
    try {
      if (connection == null || connection.isClosed()) {

        // Register JDBC driver
        Class.forName(jdbcDriver);

        logger.info("Connecting to a selected " + database + " database...");
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        logger.info("Connected to " + database + " database successfully...");

        // to be able to batch sql statements
        connection.setAutoCommit(false);
      }
    } catch (SQLException e) {
      logger.error("Error while trying to connect to database " + database + "!", e);
    } catch (ClassNotFoundException e) {
      logger.error("Could not find " + database + " jdbc driver class!", e);
    }
  }

  private String handleDatabaseSyntax(String statement) {
    return (database.equals(DATABASE_POSTGRESQL)) ? statement.toLowerCase() : statement;
  }

  @Override
  protected void starting(Description description) {
    super.starting(description);
  }

  public void changeActivityDuration(String processInstanceId,
                                     String activityId,
                                     long duration) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET DURATION_ = ? WHERE " +
      "PROC_INST_ID_ = ? AND " +
      "ACT_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setLong(1, duration);
    statement.setString(2, processInstanceId);
    statement.setString(3, activityId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeActivityDuration(String processInstanceId,
                                     long duration) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET DURATION_ = ? WHERE " +
      "PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setLong(1, duration);
    statement.setString(2, processInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeActivityDurationForProcessDefinition(String processDefinitionId,
                                     long duration) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET DURATION_ = ? WHERE " +
      "PROC_DEF_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setLong(1, duration);
    statement.setString(2, processDefinitionId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeProcessInstanceStartDate(String processInstanceId, OffsetDateTime startDate) throws SQLException {
    String sql = "UPDATE ACT_HI_PROCINST " +
      "SET START_TIME_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.toLocalDateTime()));
    statement.setString(2, processInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeActivityInstanceStartDate(String processInstanceId,
                                              OffsetDateTime startDate) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET START_TIME_ = ? WHERE " +
      "PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.toLocalDateTime()));
    statement.setString(2, processInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeFirstActivityInstanceStartDate(String activityInstanceId,
                                                OffsetDateTime startDate) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET START_TIME_ = ? WHERE " +
      "ID_ = (SELECT ID_ FROM ACT_HI_ACTINST WHERE ACT_ID_ = ? ORDER BY START_TIME_ LIMIT 1) ";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.toLocalDateTime()));
    statement.setString(2, activityInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeActivityInstanceStartDateForProcessDefinition(
                                              String processDefinitionId,
                                              OffsetDateTime startDate) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET START_TIME_ = ? WHERE " +
      "PROC_DEF_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.toLocalDateTime()));
    statement.setString(2, processDefinitionId);
    statement.executeUpdate();
    connection.commit();
  }

  public void updateActivityInstanceStartDates(Map<String, OffsetDateTime> processInstanceToDates) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET START_TIME_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    for (Map.Entry<String, OffsetDateTime> idToStartDate : processInstanceToDates.entrySet()) {
      statement.setTimestamp(1, java.sql.Timestamp.valueOf(idToStartDate.getValue().toLocalDateTime()));
      statement.setString(2, idToStartDate.getKey());
      statement.executeUpdate();
    }
    connection.commit();
  }

  public void changeActivityInstanceEndDate(String processInstanceId,
                                              OffsetDateTime endDate) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET END_TIME_ = ? WHERE " +
      "PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(endDate.toLocalDateTime()));
    statement.setString(2, processInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeFirstActivityInstanceEndDate(String activityInstanceId,
                                                OffsetDateTime endDate) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET END_TIME_ = ? WHERE " +
      "ID_ = (SELECT ID_ FROM ACT_HI_ACTINST WHERE ACT_ID_ = ? ORDER BY END_TIME_ LIMIT 1) ";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(endDate.toLocalDateTime()));
    statement.setString(2, activityInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void changeActivityInstanceEndDateForProcessDefinition(
                                              String processDefinitionId,
                                              OffsetDateTime endDate) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET END_TIME_ = ? WHERE " +
      "PROC_DEF_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(endDate.toLocalDateTime()));
    statement.setString(2, processDefinitionId);
    statement.executeUpdate();
    connection.commit();
  }

  public void updateActivityInstanceEndDates(Map<String, OffsetDateTime> processInstanceToDates) throws SQLException {
    String sql = "UPDATE ACT_HI_ACTINST " +
      "SET END_TIME_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    for (Map.Entry<String, OffsetDateTime> idToActivityEndDate : processInstanceToDates.entrySet()) {
      statement.setTimestamp(1, java.sql.Timestamp.valueOf(idToActivityEndDate.getValue().toLocalDateTime()));
      statement.setString(2, idToActivityEndDate.getKey());
      statement.executeUpdate();
    }
    connection.commit();
  }

  public void changeProcessInstanceState(String processInstanceId, String newState) throws SQLException {
    String sql = "UPDATE ACT_HI_PROCINST " +
            "SET STATE_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setString(1, newState);
    statement.setString(2, processInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void updateProcessInstanceStartDates(Map<String, OffsetDateTime> processInstanceIdToStartDate) throws SQLException {
    String sql = "UPDATE ACT_HI_PROCINST " +
      "SET START_TIME_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    for (Map.Entry<String, OffsetDateTime> idToStartDate : processInstanceIdToStartDate.entrySet()) {
      statement.setTimestamp(1, java.sql.Timestamp.valueOf(idToStartDate.getValue().toLocalDateTime()));
      statement.setString(2, idToStartDate.getKey());
      statement.executeUpdate();
    }
    connection.commit();
  }

   public void changeProcessInstanceEndDate(String processInstanceId, OffsetDateTime endDate) throws SQLException {
    String sql = "UPDATE ACT_HI_PROCINST " +
      "SET END_TIME_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    statement.setTimestamp(1, java.sql.Timestamp.valueOf(endDate.toLocalDateTime()));
    statement.setString(2, processInstanceId);
    statement.executeUpdate();
    connection.commit();
  }

  public void updateProcessInstanceEndDates(Map<String, OffsetDateTime> processInstanceIdToEndDate) throws SQLException {
    String sql = "UPDATE ACT_HI_PROCINST " +
      "SET END_TIME_ = ? WHERE PROC_INST_ID_ = ?";
    PreparedStatement statement = connection.prepareStatement(handleDatabaseSyntax(sql));
    for (Map.Entry<String, OffsetDateTime> idToStartDate : processInstanceIdToEndDate.entrySet()) {
      statement.setTimestamp(1, java.sql.Timestamp.valueOf(idToStartDate.getValue().toLocalDateTime()));
      statement.setString(2, idToStartDate.getKey());
      statement.executeUpdate();
    }
    connection.commit();
  }

  public int countHistoricActivityInstances() throws SQLException {
    String sql = "select count(*) as total from act_hi_actinst;";
    String postgresSQL =
      "SELECT reltuples AS total FROM pg_class WHERE relname = 'act_hi_actinst';";
    sql = usePostgresOptimizations() ? postgresSQL: sql;
    ResultSet statement =
      connection.createStatement().executeQuery(sql);
    statement.next();
    int totalCount =  statement.getInt("total");

    // substract the amount of activity instances that are not finished yet
    sql = "select count(*) as total from act_hi_actinst where END_TIME_ is null;";
    statement =
      connection.createStatement().executeQuery(sql);
    statement.next();
    totalCount -=  statement.getInt("total");
    return totalCount;
  }

  public int countHistoricProcessInstances() throws SQLException {
    String sql = "select count(*) as total from act_hi_procinst;";
    String postgresSQL =
      "SELECT reltuples AS total FROM pg_class WHERE relname = 'act_hi_procinst';";
    sql = usePostgresOptimizations() ? postgresSQL: sql;
    ResultSet statement =
      connection.createStatement().executeQuery(sql);
    statement.next();
    return statement.getInt("total");
  }

  public int countHistoricVariableInstances() throws SQLException {
    String sql = "select count(*) as total from act_hi_varinst;";
    String postgresSQL =
      "SELECT reltuples AS total FROM pg_class WHERE relname = 'act_hi_varinst';";
    sql = usePostgresOptimizations() ? postgresSQL: sql;
    ResultSet statement =
      connection.createStatement().executeQuery(sql);
    statement.next();
    int totalAmount =  statement.getInt("total");

    // subtract all case and complex variables
    sql = "select count(*) as total from act_hi_varinst " +
            "where var_type_ not in ('string', 'double', 'integer', 'long', 'short', 'date', 'boolean' ) " +
              "or CASE_INST_ID_  is not null;";
    statement =
      connection.createStatement().executeQuery(sql);
    statement.next();
    totalAmount -= statement.getInt("total");

    return totalAmount;
  }

  public int countProcessDefinitions() throws SQLException {
    String sql = "select count(*) as total from act_re_procdef;";
    ResultSet statement =
      connection.createStatement().executeQuery(sql);
    statement.next();
    return statement.getInt("total");
  }

  @Override
  protected void finished(Description description) {
    super.finished(description);
  }

  private boolean usePostgresOptimizations() {
    return DATABASE_POSTGRESQL.equals(database) && usePostgresOptimizations;
  }
}
