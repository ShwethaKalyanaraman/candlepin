/**
 * Copyright (c) 2009 - 2018 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.liquibase;

import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;



/**
 * The Candlepin Database class holds references to the database connection and provides utility
 * methods for performing various database operations.
 */
public class Database {

    protected liquibase.database.Database database;
    protected JdbcConnection connection;

    private int nullType;
    private Map<String, PreparedStatement> preparedStatements;

    /**
     * Creates a new (Candlepin) Database object, wrapping the provided Liquibase Database.
     *
     * @param database
     *  The Liquibase database object to wrap
     *
     * @throws IllegalArgumentException
     *  if database is null
     */
    public Database(liquibase.database.Database database) {
        if (database == null) {
            throw new IllegalArgumentException("database is null");
        }

        DatabaseConnection dbconn = database.getConnection();
        if (!(dbconn instanceof JdbcConnection)) {
            throw new RuntimeException("Database connection is not a JDBC connection");
        }

        this.database = database;
        this.connection = (JdbcConnection) dbconn;

        // Check which type we need to use for nulls (courtesy of Oracle's moody adapter)
        // See the comments on this SO question for details:
        // http://stackoverflow.com/questions/11793483/setobject-method-of-preparedstatement
        this.nullType =
            this.database.getDatabaseProductName().matches(".*(?i:oracle).*") ? Types.VARCHAR : Types.NULL;

        this.preparedStatements = new HashMap<>();
    }

    /**
     * Fetches a reference to the JdbcConnection backing this Database object.
     *
     * @return
     *  The JdbcConnection object backing this
     */
    public JdbcConnection getJdbcConnection() {
        return this.connection;
    }

    /**
     * Fetches a reference to the backing Liquibase Database object.
     *
     * @return
     *  The backing Liquibase Database object
     */
    public liquibase.database.Database getLiquibaseDatabase() {
        return this.database;
    }

    /**
     * Fetches the name of the database product this connection is connected to
     *
     * @return
     *  The name of the database product this connection is connected to
     */
    public String getDatabaseProductName() {
        return this.database.getDatabaseProductName();
    }

    /**
     * Sets the parameter at the specified index to the given value. This method attempts to perform
     * safe assignment of parameters across all supported platforms.
     *
     * @param statement
     *  the statement on which to set a parameter
     *
     * @param index
     *  the index of the parameter to set
     *
     * @param value
     *  the value to set
     *
     * @throws NullPointerException
     *  if statement is null
     *
     * @return
     *  the PreparedStatement being updated
     */
    protected PreparedStatement setParameter(PreparedStatement statement, int index, Object value)
        throws DatabaseException, SQLException {

        if (value != null) {
            statement.setObject(index, value);
        }
        else {
            statement.setNull(index, this.nullType);
        }

        return statement;
    }

    /**
     * Fills the parameters of a prepared statement with the given arguments
     *
     * @param statement
     *  the statement to fill
     *
     * @param argv
     *  the collection of arguments with which to fill the statement's parameters
     *
     * @throws NullPointerException
     *  if statement is null
     *
     * @return
     *  the provided PreparedStatement
     */
    protected PreparedStatement fillStatementParameters(PreparedStatement statement, Object... argv)
        throws DatabaseException, SQLException {

        statement.clearParameters();

        if (argv != null) {
            for (int i = 0; i < argv.length; ++i) {
                this.setParameter(statement, i + 1, argv[i]);
            }
        }

        return statement;
    }

    /**
     * Prepares a statement and populates it with the specified arguments, pulling from cache when
     * possible.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given query.
     *
     * @return
     *  a PreparedStatement instance representing the specified SQL statement
     */
    protected PreparedStatement prepareStatement(String sql, Object... argv)
        throws DatabaseException, SQLException {

        PreparedStatement statement = this.preparedStatements.get(sql);
        if (statement == null) {
            statement = this.connection.prepareStatement(sql);
            this.preparedStatements.put(sql, statement);
        }

        return this.fillStatementParameters(statement, argv);
    }

    /**
     * Executes the given SQL query.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given query.
     *
     * @return
     *  A ResultSet instance representing the result of the query.
     */
    protected ResultSet executeQuery(String sql, Object... argv) throws DatabaseException, SQLException {
        PreparedStatement statement = this.prepareStatement(sql, argv);
        return statement.executeQuery();
    }

    /**
     * Executes the given SQL update/insert.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given update.
     *
     * @return
     *  The number of rows affected by the update.
     */
    protected int executeUpdate(String sql, Object... argv) throws DatabaseException, SQLException {
        PreparedStatement statement = this.prepareStatement(sql, argv);
        return statement.executeUpdate();
    }

}
