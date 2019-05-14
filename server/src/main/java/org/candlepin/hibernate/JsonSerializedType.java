/**
 * Copyright (c) 2009 - 2019 Red Hat, Inc.
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
package org.candlepin.hibernate;

import org.candlepin.util.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;



/**
 * A base class providing a configured ObjectMapper for serialization and deserialization services.
 */
public abstract class JsonSerializedType implements UserType {

    /** A shared ObjectMapper to be used by all JSON-serialized types */
    private static ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();

    /**
     * Fetches the ObjectMapper instance to use for serializing and deserializing data for this
     * type.
     *
     * @return
     *  the ObjectMapper instance to use for serialization and deserialization
     */
    protected ObjectMapper getObjectMapper() {
        return mapper;
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.VARCHAR };
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session,
        Object owner) throws HibernateException, SQLException {

        try {
            String data = StandardBasicTypes.STRING.nullSafeGet(resultSet, names[0], session);
            return this.deserialize(data);
        }
        catch (HibernateException | SQLException e) {
            // Rethrow these exceptions
            throw e;
        }
        catch (Exception e) {
            // Unexpected exception; wrap it and rethrow it so Hibernate fails "gracefully"
            throw new HibernateException(e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement statement, Object value, int index,
        SharedSessionContractImplementor session) throws HibernateException, SQLException {

        try {
            String data = this.serialize(value);
            StandardBasicTypes.STRING.nullSafeSet(statement, data, index, session);
        }
        catch (HibernateException | SQLException e) {
            // Rethrow these exceptions
            throw e;
        }
        catch (Exception e) {
            // Unexpected exception; wrap it and rethrow it so Hibernate fails "gracefully"
            throw new HibernateException(e);
        }
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        try {
            return this.serialize(value);
        }
        catch (HibernateException e) {
            // Rethrow this exceptions
            throw e;
        }
        catch (Exception e) {
            // Unexpected exception; wrap it and rethrow it so Hibernate fails "gracefully"
            throw new HibernateException(e);
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        try {
            return this.deserialize((String) cached);
        }
        catch (HibernateException e) {
            // Rethrow this exceptions
            throw e;
        }
        catch (Exception e) {
            // Unexpected exception; wrap it and rethrow it so Hibernate fails "gracefully"
            throw new HibernateException(e);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) {
        // During merge, replace the existing (target) value in the entity we are merging to with a
        // new (original) value from the detached entity we are merging.
        try {
            return this.isMutable() ? this.deepCopy(original) : original;
        }
        catch (HibernateException e) {
            // Rethrow this exceptions
            throw e;
        }
        catch (Exception e) {
            // Unexpected exception; wrap it and rethrow it so Hibernate fails "gracefully"
            throw new HibernateException(e);
        }
    }

    @Override
    public boolean equals(Object lhs, Object rhs) {
        return lhs != null ? lhs.equals(rhs) : rhs == null;
    }

    @Override
    public int hashCode(Object obj) {
        return obj != null ? obj.hashCode() : 0;
    }

    /**
     * Deserializes the provided JSON data into the appropriate object.
     * <p></p>
     * Subclasses intending to change how deserialization is performed should override this method
     * rather than those defined by the UserType interface.
     *
     * @param json
     *  The JSON data to deserialize
     *
     * @return
     *  the deserialized object
     */
    protected Object deserialize(String json) throws Exception {
        ObjectMapper mapper = this.getObjectMapper();
        return json != null ? mapper.readValue(json, this.returnedClass()) : null;
    }

    /**
     * Serializes the given value into a JSON string.
     * <p></p>
     * Subclasses intending to change how serialization is performed should override this method
     * rather than those defined by the UserType interface.
     *
     * @param value
     *  The value to serialize
     *
     * @return
     *  the JSON-serialized representation of the given value
     */
    protected String serialize(Object value) throws Exception {
        ObjectMapper mapper = this.getObjectMapper();
        return value != null ? mapper.writeValueAsString(value) : null;
    }

}
