/**
 * MediaMosa API
 *
 * A partial implementation of the MediaMosa API in Java.
 *
 * Copyright 2010 Universiteit van Amsterdam
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

package nl.uva.mediamosa.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Datatype xs:dateTime has format "yyyy-MM-ddThh:mm:ss" and the specification requires
 * that timestamps are given in UTC time, not local time. This is a custom converter to
 * use format "yyyy-MM-ddThh:mm:ss".
 */

@SuppressWarnings({"PMD.AvoidUsingShortType"})
public class DataTypeConverterUtil {

    private static final String DATE_TIME_FORMAT = "dd-MM-yyyy hh:mm";
    private static final Logger log = LoggerFactory.getLogger(DataTypeConverterUtil.class.getName());

    /**
     * @param value
     * @return
     */
    public static String printDateTime(Calendar value) {
        String format = DATE_TIME_FORMAT;
        DateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(value.getTime());
    }

    /**
     * @param value
     * @return
     */
    public static Boolean parseBoolean(String value) {
        if (!("").equals(value)) {
            return Boolean.valueOf(value.toLowerCase());
        }
        return Boolean.FALSE;
    }

    public static String printBoolean(Boolean value) {
        return String.valueOf(value);
    }


    /**
     * @param value
     * @return
     */
    public static Calendar parseDateTime(String value) {
        if (!("").equals(value)) {
            String format = DATE_TIME_FORMAT;
            DateFormat formatter = new SimpleDateFormat(format);
            try {
                Date parsedDate = formatter.parse(value);
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsedDate);
                return cal;
            } catch (ParseException e) {
                log.error("Unable to parse datetime string {} to format {}", new Object[]{value, DATE_TIME_FORMAT});
                if (log.isDebugEnabled()) {
                    log.debug("Unable to parse datetime string {} to format {}", new Object[]{value, DATE_TIME_FORMAT}, e);
                }
            }
        }
        return null;
    }

    /*
    http://java.sun.com/webservices/docs/1.6/api/javax/xml/bind/DatatypeConverter.html
    http://docs.sun.com/app/docs/doc/819-3669/bnbbz?a=view
    */

    /**
     * @param value
     * @return
     */
    public static short parseIntegerToShort(String value) {
        BigInteger result = DatatypeConverter.parseInteger(value);
        return (short) (result.intValue());
    }

    /**
     * @param value
     * @return
     */
    public static String printShortToInteger(short value) {
        BigInteger result = BigInteger.valueOf(value);
        return DatatypeConverter.printInteger(result);
    }

    /**
     * @param value
     * @return
     */
    public static int parseIntegerToInt(String value) {
        BigInteger result = DatatypeConverter.parseInteger(value);
        return result.intValue();
    }

    /**
     * @param value
     * @return
     */
    public static String printIntToInteger(int value) {
        BigInteger result = BigInteger.valueOf(value);
        return DatatypeConverter.printInteger(result);
    }
}
