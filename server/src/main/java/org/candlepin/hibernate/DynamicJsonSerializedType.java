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
package org.candlepin.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.Properties;



/**
 * The JsonSerializedMapType implements the Hibernate type interface necessary for properly
 * serializing and deserializing JsonSerializedMap instances associated with a given entity.
 */
public class DynamicJsonSerializedType extends JsonSerializedType implements DynamicParameterizedType {

    private static final String JSON_CLASS_PARAMETER = "jsonClass";

    private Class valueClass;

    @Override
    public Class returnedClass() {
        return this.valueClass;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        try {
            return value != null ? this.deserialize(this.serialize(value)) : null;
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
    @SuppressWarnings("unchecked")
    public void setParameterValues(Properties parameters) {
        ParameterType ptype = (ParameterType) parameters.get(PARAMETER_TYPE);

        if (ptype != null) {
            this.valueClass = ptype.getReturnedClass();
        }
        else {
            // This only occurs if the entity is configured via XML

            try {
                // We're using the same parameter that we used in the ResultDataUserType for
                // legacy/consistency reasons. We can rename or add additional options here
                // as needed.
                String jsonClassName = (String) parameters.get(JSON_CLASS_PARAMETER);
                this.valueClass = this.loadClass(jsonClassName);
            }
            catch (ClassNotFoundException e) {
                throw new HibernateException("Unable to load value class", e);
            }
        }
    }

    /**
     * Attempts to load the class for the given class name using the appropriate class loaders.
     * If the class cannot be loaded for any reason, this method throws an exception.
     *
     * @param className
     *  the name of the class to load
     *
     * @throws ClassNotFoundException
     *  if the class does not exist or cannot be loaded
     *
     * @return
     *  the class for the given class name
     */
    private Class loadClass(String className) throws ClassNotFoundException {
        Class loaded = null;

        ClassLoader[] classloaders = new ClassLoader[] {
            Thread.currentThread().getContextClassLoader(),
            this.getClass().getClassLoader()
        };

        for (ClassLoader classloader : classloaders) {
            if (classloader != null) {
                try {
                    loaded = Class.forName(className, true, classloader);
                    break;
                }
                catch (Throwable t) {
                    // Something went wrong attempting to load/initialize the class; treat
                    // it as if it doesn't exist.
                }
            }
        }

        if (loaded == null) {
            String errmsg = String.format("Class not found: %s", className);
            throw new ClassNotFoundException(errmsg);
        }

        return loaded;
    }
}
