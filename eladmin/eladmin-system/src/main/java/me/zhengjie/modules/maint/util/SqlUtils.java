/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.modules.maint.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.enums.DataTypeEnum;
import me.zhengjie.utils.CloseUtil;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author /
 */
@Slf4j
public class SqlUtils {

	/**
	 * Get data source
	 *
	 * @param jdbcUrl /
	 * @param userName /
	 * @param password /
	 * @return DataSource
	 */
	private static DataSource getDataSource(String jdbcUrl, String userName, String password) {
		DruidDataSource druidDataSource = new DruidDataSource();
		String className;
		try {
			className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
		} catch (SQLException e) {
			throw new RuntimeException("Get class name error: =" + jdbcUrl);
		}
		if (StringUtils.isEmpty(className)) {
			DataTypeEnum dataTypeEnum = DataTypeEnum.urlOf(jdbcUrl);
			if (null == dataTypeEnum) {
				throw new RuntimeException("Not supported data type: jdbcUrl=" + jdbcUrl);
			}
			druidDataSource.setDriverClassName(dataTypeEnum.getDriver());
		} else {
			druidDataSource.setDriverClassName(className);
		}

		// Remove insecure parameters
		jdbcUrl = sanitizeJdbcUrl(jdbcUrl);

		druidDataSource.setUrl(jdbcUrl);
		druidDataSource.setUsername(userName);
		druidDataSource.setPassword(password);
		// Configure timeout waiting for connection acquire
		druidDataSource.setMaxWait(3000);
		// Configure initial, minimum, and maximum pool size
		druidDataSource.setInitialSize(1);
		druidDataSource.setMinIdle(1);
		druidDataSource.setMaxActive(1);

		// If connection fails, fail immediately rather than retrying indefinitely
		druidDataSource.setBreakAfterAcquireFailure(true);
		try {
			druidDataSource.init();
		} catch (SQLException e) {
			log.error("Exception during pool initialization", e);
			throw new RuntimeException(e.getMessage());
		}

		return druidDataSource;
	}

	private static Connection getConnection(String jdbcUrl, String userName, String password) {
		DataSource dataSource = getDataSource(jdbcUrl, userName, password);
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
		} catch (Exception ignored) {}
		try {
			int timeOut = 5;
			if (null == connection || connection.isClosed() || !connection.isValid(timeOut)) {
				log.info("connection is closed or invalid, retry get connection!");
				connection = dataSource.getConnection();
			}
		} catch (Exception e) {
			log.error("create connection error, jdbcUrl: {}", jdbcUrl);
			throw new RuntimeException("create connection error, jdbcUrl: " + jdbcUrl);
		}
		return connection;
	}

	private static void releaseConnection(Connection connection) {
		if (null != connection) {
			try {
				connection.close();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
	}

	public static boolean testConnection(String jdbcUrl, String userName, String password) {
		Connection connection = null;
		try {
			connection = getConnection(jdbcUrl, userName, password);
			if (null != connection) {
				return true;
			}
		} catch (Exception e) {
            log.error("Get connection failed:{}", e.getMessage());
		} finally {
			releaseConnection(connection);
		}
		return false;
	}

	public static String executeFile(String jdbcUrl, String userName, String password, File sqlFile) {
		Connection connection = getConnection(jdbcUrl, userName, password);
		try {
			batchExecute(connection, readSqlList(sqlFile));
		} catch (Exception e) {
			log.error("Exception occurred while executing SQL script: {}", e.getMessage());
			return e.getMessage();
		}finally {
			releaseConnection(connection);
		}
		return "success";
	}

	/**
	 * Batch execute SQL
	 * @param connection /
	 * @param sqlList /
	 */
	public static void batchExecute(Connection connection, List<String> sqlList) {
		try (Statement st = connection.createStatement()) {
			for (String sql : sqlList) {
				// Remove trailing semicolon
				if (sql.endsWith(";")) {
					sql = sql.substring(0, sql.length() - 1);
				}
				// Check if SQL statement is empty
				if (!sql.trim().isEmpty()) {
					st.addBatch(sql);
				}
			}
			st.executeBatch();
		} catch (SQLException e) {
			log.error("Exception occurred during batch execution of SQL script: {}, error code: {}", e.getMessage(), e.getErrorCode());
		}
	}

	/**
	 * Read SQL statements from file into a list separated by semicolon
	 * @param sqlFile /
	 * @return /
     */
	private static List<String> readSqlList(File sqlFile) {
		List<String> sqlList = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = Files.newBufferedReader(sqlFile.toPath(), StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				log.info("line: {}", line);
				sb.append(line.trim());

				if (line.trim().endsWith(";")) {
					sqlList.add(sb.toString());
					// Clear StringBuilder
					sb.setLength(0);
				} else {
					// Add a space between lines
					sb.append(" ");
				}
			}
			if (sb.length() > 0) {
				sqlList.add(sb.toString().trim());
			}
		} catch (Exception e) {
			log.error("Exception occurred while reading SQL file: {}", e.getMessage());
		}
		return sqlList;
	}

	/**
	 * Remove insecure parameters
	 * @param jdbcUrl /
	 * @return /
	 */
	private static String sanitizeJdbcUrl(String jdbcUrl) {
		// Define insecure parameters and their secure replacements
		String[][] unsafeParams = {
				// allowLoadLocalInfile: allows LOAD DATA LOCAL INFILE, may cause file leakage
				{"allowLoadLocalInfile", "false"},
				// allowUrlInLocalInfile: allows URL in LOAD DATA LOCAL INFILE, may cause unauthorized file access
				{"allowUrlInLocalInfile", "false"},
				// autoDeserialize: allows automatic deserialization of objects, may cause deserialization vulnerabilities
				{"autoDeserialize", "false"},
				// allowNanAndInf: allows NaN and Infinity as numbers, may cause inconsistent data processing
				{"allowNanAndInf", "false"},
				// allowMultiQueries: allows multiple queries in one statement, may lead to SQL injection attacks
				{"allowMultiQueries", "false"},
				// allowPublicKeyRetrieval: allows retrieving public key from server, may lead to MITM attacks
				{"allowPublicKeyRetrieval", "false"}
		};

		// Replace insecure parameters
		for (String[] param : unsafeParams) {
			jdbcUrl = jdbcUrl.replaceAll("(?i)" + param[0] + "=true", param[0] + "=" + param[1]);
		}
		return jdbcUrl;
	}
}
