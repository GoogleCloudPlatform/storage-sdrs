/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 * Any software provided by Google hereunder is distributed “AS IS”,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.
 *
 */

package com.google.cloudy.retention.service.dataaccess;

import java.io.Serializable;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import javax.sql.DataSource;

import com.google.cloudy.retention.pojo.StsJobModel;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.google.cloudy.retention.pojo.RetentionJobLog;
import com.google.cloudy.retention.pojo.RetentionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/*
 *@deprecated POC code only
 */
@Deprecated
public class SqlDbManager {

  private static final Logger logger = LoggerFactory.getLogger(SqlDbManager.class);

  private static String dbHost;
  private static String dbName;
  private static String user;
  private static String password;;
  private static int poolSize;

  private static SqlDbManager instance;
  private static GenericObjectPool gPool = null;
  private static DataSource dataSource = null;

  /**
   * @deprecated
   * @return
   */
  public static SqlDbManager getInstance()  {
    if (instance == null) {
      synchronized (SqlDbManager.class) {
        if(instance == null){
          try {
            instance = new SqlDbManager();
          } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
          }
        }
      }
    }
    return instance;
  }

  public static SqlDbManager getInstance(String host, String schema, String user, String password, int poolSize)  {
    SqlDbManager.dbHost=host;
    SqlDbManager.dbName=schema;
    SqlDbManager.user=user;
    SqlDbManager.password=password;
    SqlDbManager.poolSize=poolSize;
    if (instance == null) {
      synchronized (SqlDbManager.class) {
        if(instance == null){
          try {
            instance = new SqlDbManager();
          } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
          }
        }
      }
    }
    return instance;
  }

  private SqlDbManager() throws ClassNotFoundException {
    dataSource = setUpPool();
  }


  public DataSource setUpPool() throws ClassNotFoundException {
    Class.forName("com.mysql.jdbc.Driver");
    gPool = new GenericObjectPool();
    gPool.setMaxActive(poolSize);
    ConnectionFactory cf = new DriverManagerConnectionFactory("jdbc:mysql://"+dbHost+"/"+dbName,user,password);
    PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf, gPool, null, null, false, true);

    return new PoolingDataSource(gPool);
  }

  /**
   *
   * @return
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  protected Connection getDbConnection() throws SQLException, ClassNotFoundException {
    return dataSource.getConnection();
  }


  public static void main(String[] argv) throws ClassNotFoundException, SQLException  {
    logger.info("running db test code");
    try{
      SqlDbManager sqlDb = new SqlDbManager();
      Connection con = sqlDb.getDbConnection();
      insert("testing 123");
      Statement stmt=con.createStatement();
      ResultSet rs=stmt.executeQuery("select * from test");
      while(rs.next())
        logger.debug(String.format("%s %s %s", rs.getString(1),
            rs.getString(2), rs.getString(3)));

      con.close();
    }catch(Exception e){
      logger.error((e.getCause().toString()));
    }
  }

  public void saveOrUpdate(Serializable object) throws ClassNotFoundException, SQLException {
    if (object instanceof RetentionJobLog) {
      saveOrUpdate((RetentionJobLog)object);
    }
  }

  public List<StsJobModel> getStsJobsValidation(int hours) throws ClassNotFoundException, SQLException {
    List<StsJobModel> jobs = new ArrayList<>();
    Connection con = getDbConnection();

    String query = " select name, batch_id, type, bucket, project_id,status, created_at from sts_jobs "
        + "where created_at > ? or type = 'default' ";
    PreparedStatement preparedStmt = con.prepareStatement(query);
    preparedStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(Clock.systemUTC()).minusHours(hours)));



    ResultSet rs = preparedStmt.executeQuery();

    while (rs != null && rs.next()) {
      StsJobModel stsJobModel = new StsJobModel(rs.getString("name"),rs.getString("batch_id"),rs.getTimestamp("created_at").getTime(),rs.getString("status"),rs.getString("type"),rs.getString("bucket"), rs.getString("project_id"));
      jobs.add(stsJobModel);
    }

    preparedStmt.close();
    con.close();

    return jobs;
  }

  public StsJobModel getStsJob(String bucket, String type) throws ClassNotFoundException, SQLException {
    StsJobModel stsJobModel = null;
    Connection con = getDbConnection();
    String query = " select name, batch_id, type, bucket, project_id,status, created_at from sts_jobs "
        + " where type = ? and bucket = ? order by created_at desc ";
    PreparedStatement preparedStmt = con.prepareStatement(query);
    preparedStmt.setString (1, type);
    preparedStmt.setString(2, bucket);


    ResultSet rs = preparedStmt.executeQuery();

    if (rs != null && rs.next()) {
      stsJobModel = new StsJobModel(rs.getString("name"),rs.getString("batch_id"),rs.getTimestamp("created_at").getTime(),rs.getString("status"),rs.getString("type"),rs.getString("bucket"), rs.getString("project_id"));
    }

    preparedStmt.close();
    con.close();

    return stsJobModel;

  }

  public void saveStsJob(StsJobModel job)  throws ClassNotFoundException, SQLException {
    Connection con = getDbConnection();

    String query = " insert into sts_jobs (name, batch_id, type, bucket, project_id,status) "

        + " values (?, ?, ?, ?, ?, ?) ";

    PreparedStatement preparedStmt = con.prepareStatement(query);
    preparedStmt.setString (1, job.getJobName());
    preparedStmt.setString(2, job.getBatchID());
    preparedStmt.setString (3, job.getType());
    preparedStmt.setString (4, job.getBucket());
    preparedStmt.setString (5, job.getProjectId());
    preparedStmt.setString(6, job.getStatus());

    // execute the preparedstatement
    preparedStmt.execute();
    preparedStmt.close();
    con.close();

  }
  public List<RetentionRule> getRetentionRules() throws ClassNotFoundException, SQLException{
    List<RetentionRule> retentionRuleList = new ArrayList<>();
    Connection con = getDbConnection();
    Statement stmt = con.createStatement();
    String query = "select dataset, age, bucket from retention_rules where status = 'active' and type = 'dataset' ";
    ResultSet rs = stmt.executeQuery(query);
    while (rs.next()) {
      //String dataset, int age, String bucket
      RetentionRule rule = new RetentionRule(rs.getString("dataset"), rs.getInt("age"), rs.getString("bucket"));
      retentionRuleList.add(rule);
    }
    stmt.close();
    con.close();

    return retentionRuleList;

  }


  public int getDefaultRule() throws ClassNotFoundException, SQLException {
    int defaultDays = 0;
    Connection con = getDbConnection();
    Statement stmt = con.createStatement();
    String query = "select age from retention_rules where status = 'active' and type = 'default' ";
    ResultSet rs = stmt.executeQuery(query);
    if (rs.next()) {
      defaultDays = rs.getInt("age");
    }

    stmt.close();
    con.close();
    return defaultDays;
  }


  protected void saveOrUpdate(RetentionJobLog retentionJobLog) throws ClassNotFoundException, SQLException {
    Connection con = getDbConnection();

    String query = " insert into retention_job_log (id, start_time, end_time, type, status) "

        + " values (?, ?, ?, ?, ?) "
        + " on duplicate key update start_time=values(start_time), end_time=values(end_time), type=values(type), "
        + " status=values(status) ";

    // create the mysql insert preparedstatement
    PreparedStatement preparedStmt = con.prepareStatement(query);
    preparedStmt.setString (1, retentionJobLog.getId());
    preparedStmt.setString(2, retentionJobLog.getStartTime()== null ? null: retentionJobLog.getStartTime().toString());
    preparedStmt.setString (3, retentionJobLog.getEndTime() == null ? null: retentionJobLog.getEndTime().toString());
    preparedStmt.setString (4, retentionJobLog.getType());
    preparedStmt.setString (5, retentionJobLog.getStatus());

    // execute the preparedstatement
    preparedStmt.execute();
    preparedStmt.close();
    con.close();
  }



  public static void insert (String data) throws ClassNotFoundException, SQLException
  {
    Random r = new Random();
    String id = UUID.randomUUID().toString();
    Class.forName("com.mysql.jdbc.Driver");
    Connection con=DriverManager.getConnection(
        "jdbc:mysql://"+dbHost+"/"+dbName,user,password);

    // the mysql insert statement
    String query = " insert into test (id, file_name, testcol)"
        + " values (?, ?, ?)";

    // create the mysql insert preparedstatement
    PreparedStatement preparedStmt = con.prepareStatement(query);
    preparedStmt.setString (1, id);
    preparedStmt.setString (2, "helloWorld");
    preparedStmt.setString (3, data);

    // execute the preparedstatement
    preparedStmt.execute();

    con.close();
  }

}
