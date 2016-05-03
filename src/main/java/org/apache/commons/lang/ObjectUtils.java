package org.apache.commons.lang;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang.exception.CloneFailedException;
import org.apache.commons.lang.reflect.MethodUtils;

public class ObjectUtils {

    public static final ObjectUtils.Null NULL = new ObjectUtils.Null();

    public static Object defaultIfNull(Object object, Object defaultValue) {
        return object != null ? object : defaultValue;
    }

    public static boolean equals(Object object1, Object object2) {
        return object1 == object2 ? true : (object1 != null && object2 != null ? object1.equals(object2) : false);
    }

    public static boolean notEqual(Object object1, Object object2) {
        return !equals(object1, object2);
    }

    public static int hashCode(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    public static String identityToString(Object object) {
        if (object == null) {
            return null;
        } else {
            StringBuffer buffer = new StringBuffer();

            identityToString(buffer, object);
            return buffer.toString();
        }
    }

    public static void identityToString(StringBuffer buffer, Object object) {
        if (object == null) {
            throw new NullPointerException("Cannot get the toString of a null identity");
        } else {
            buffer.append(object.getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(object)));
        }
    }

    /** @deprecated */
    public static StringBuffer appendIdentityToString(StringBuffer buffer, Object object) {
        if (object == null) {
            return null;
        } else {
            if (buffer == null) {
                buffer = new StringBuffer();
            }

            return buffer.append(object.getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(object)));
        }
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    public static String toString(Object obj, String nullStr) {
        return obj == null ? nullStr : obj.toString();
    }

    public static Object min(Comparable c1, Comparable c2) {
        return compare(c1, c2, true) <= 0 ? c1 : c2;
    }

    public static Object max(Comparable c1, Comparable c2) {
        return compare(c1, c2, false) >= 0 ? c1 : c2;
    }

    public static int compare(Comparable c1, Comparable c2) {
        return compare(c1, c2, false);
    }

    public static int compare(Comparable c1, Comparable c2, boolean nullGreater) {
        return c1 == c2 ? 0 : (c1 == null ? (nullGreater ? 1 : -1) : (c2 == null ? (nullGreater ? -1 : 1) : c1.compareTo(c2)));
    }

    public static Object clone(Object o) {
        if (!(o instanceof Cloneable)) {
            return null;
        } else {
            Object result;

            if (o.getClass().isArray()) {
                Class e = o.getClass().getComponentType();

                if (!e.isPrimitive()) {
                    result = ((Object[]) ((Object[]) o)).clone();
                } else {
                    int length = Array.getLength(o);

                    result = Array.newInstance(e, length);

                    while (length-- > 0) {
                        Array.set(result, length, Array.get(o, length));
                    }
                }
            } else {
                try {
                    result = MethodUtils.invokeMethod(o, "clone", (Object[]) null);
                } catch (NoSuchMethodException nosuchmethodexception) {
                    throw new CloneFailedException("Cloneable type " + o.getClass().getName() + " has no clone method", nosuchmethodexception);
                } catch (IllegalAccessException illegalaccessexception) {
                    throw new CloneFailedException("Cannot clone Cloneable type " + o.getClass().getName(), illegalaccessexception);
                } catch (InvocationTargetException invocationtargetexception) {
                    throw new CloneFailedException("Exception cloning Cloneable type " + o.getClass().getName(), invocationtargetexception.getTargetException());
                }
            }

            return result;
        }
    }

    public static Object cloneIfPossible(Object o) {
        Object clone = clone(o);

        return clone == null ? o : clone;
    }

    public static class Null implements Serializable {

        private static final long serialVersionUID = 7092611880189329093L;

        private Object readResolve() {
            return ObjectUtils.NULL;
        }
    }
}
