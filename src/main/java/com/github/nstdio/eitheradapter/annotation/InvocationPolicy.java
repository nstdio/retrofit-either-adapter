package com.github.nstdio.eitheradapter.annotation;

import com.github.nstdio.eitheradapter.EitherCall;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.github.nstdio.eitheradapter.annotation.InvocationPolicy.StatusCodeRange.*;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation serves to determine the type of data into which the response body must be converted.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface InvocationPolicy {

    /**
     * The response status codes for which the {@link EitherCall} attempts to convert to
     * the first parameterized type.
     */
    int[] left() default {};

    /**
     * The response status codes for which the {@link EitherCall} attempts to convert to
     * the second parameterized type.
     */
    int[] right() default {};

    /**
     * Sequential range of response status code values at which the {@link EitherCall}
     * tries to convert to the first parameterized type.
     */
    StatusCodeRange[] leftRange() default {SUCCESS, REDIRECT};

    /**
     * Sequential range of response status code values at which the {@link EitherCall}
     * tries to convert to the second parameterized type.
     */
    StatusCodeRange[] rightRange() default {CLIENT_ERROR, SERVER_ERROR};

    /**
     * Sequential range of values ​​for the status code of the response.
     */
    enum StatusCodeRange {
        SUCCESS(200, 299),
        REDIRECT(300, 399),
        CLIENT_ERROR(400, 499),
        SERVER_ERROR(500, 599);

        /**
         * The lower bound.
         */
        private final int low;

        /**
         * The upper bound.
         */
        private final int high;

        StatusCodeRange(final int low, final int high) {
            this.low = low;
            this.high = high;
        }

        public static boolean inRange(InvocationPolicy.StatusCodeRange[] ranges, int code) {
            for (InvocationPolicy.StatusCodeRange range : ranges) {
                if (inRange(range, code)) {
                    return true;
                }
            }

            return false;
        }


        public static boolean inRange(InvocationPolicy.StatusCodeRange range, int code) {
            return code >= range.low() && code <= range.high();
        }

        public int low() {
            return low;
        }

        public int high() {
            return high;
        }
    }
}
