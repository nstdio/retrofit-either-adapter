package com.github.nstdio.eitheradapter;

import android.os.Handler;
import android.os.Looper;
import com.github.nstdio.eitheradapter.annotation.InvocationPolicy;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;

/**
 * In real-life situations, REST API users often need to determine the type of response data from the server at runtime
 * based on the response status code. Often we have to do this manually. For example, when we made a {@code GET} request
 * and got a response code of {@code 200}, we expect to get the data and convert it in a specific Java type, and if we
 * received a {@code 422} response code, which means we have data validation errors and assume the server has run errors
 * in the response body. Often this means that we need a different type of data to represent these validation errors. To
 * automate this routine there is this adapter. It supports only two types of response.
 * <p>
 * The adapter serves to determine the return type based on the status code of the response.
 *
 * @param <L> The first possible type of response.
 * @param <R> The second possible type of response.
 */
public class EitherCall<L, R> {
    private final Call<ResponseBody> call;
    private final Converter<ResponseBody, L> leftConverter;
    private final Converter<ResponseBody, R> rightConverter;
    private final InvocationPolicy invocationPolicy;
    private final Handler handler;
    private EitherCallback<L, R> callback;
    private boolean converterExc;

    public EitherCall(final Call<ResponseBody> call,
                      Converter<ResponseBody, L> leftConverter,
                      Converter<ResponseBody, R> rightConverter,
                      InvocationPolicy statusCode) {
        this.call = call;
        this.leftConverter = leftConverter;
        this.rightConverter = rightConverter;
        this.invocationPolicy = statusCode;
        handler = new Handler(Looper.getMainLooper());

        checkEmptyBounds();
    }

    private void checkEmptyBounds() {
        if (invocationPolicy.right().length == 0 &&
                invocationPolicy.left().length == 0 &&
                invocationPolicy.leftRange().length == 0 &&
                invocationPolicy.rightRange().length == 0) {

            throw new IllegalStateException("Invocation policy has no bound for status code checking.");
        }
    }

    /**
     * @param callback
     */
    public void callback(final EitherCallback<L, R> callback) {
        this.callback = callback;

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                final int code = response.code();

                if (contains(invocationPolicy.left(), code)) {
                    callOnLeft(response);
                    return;
                } else if (contains(invocationPolicy.right(), code)) {
                    callOnRight(response);
                    return;
                }

                final boolean leftEmpty = invocationPolicy.left().length == 0;
                final boolean rightEmpty = invocationPolicy.right().length == 0;

                if (!leftEmpty && !rightEmpty) {
                    callback.onException(new IllegalStateException("Either left nor right does not contain response" +
                            " status code: " + code));
                    return;
                }

                if (leftEmpty && inRange(invocationPolicy.leftRange(), code)) {
                    callOnLeft(response);
                } else if (rightEmpty && inRange(invocationPolicy.rightRange(), code)) {
                    callOnRight(response);
                } else {
                    callback.onException(new IllegalStateException("Cannot determine status code: " + code));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onException(t);
            }
        });
    }

    private void callOnRight(Response<ResponseBody> response) {
        final R right = convertRight(response);
        if (converterExc) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onRight(right);
            }
        });
    }

    private void callOnLeft(Response<ResponseBody> response) {
        final L left = convertLeft(response);
        if (converterExc) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onLeft(left);
            }
        });
    }

    private boolean clientOrServerError(int code) {
        return inRange(InvocationPolicy.StatusCodeRange.CLIENT_ERROR, code) ||
                inRange(InvocationPolicy.StatusCodeRange.SERVER_ERROR, code);
    }

    @SuppressWarnings("unchecked")
    private R convertRight(Response<ResponseBody> response) {
        return (R) convert(rightConverter, determineResponseBody(response));
    }

    @SuppressWarnings("unchecked")
    private L convertLeft(Response<ResponseBody> response) {
        return (L) convert(leftConverter, determineResponseBody(response));
    }

    private ResponseBody determineResponseBody(Response<ResponseBody> response) {
        return clientOrServerError(response.code()) ? response.errorBody() : response.body();
    }

    private Object convert(Converter<ResponseBody, ?> converter, ResponseBody body) {
        if (body == null || body.contentLength() == 0) {
            return null;
        }

        try {
            return converter.convert(body);
        } catch (Exception e) {
            converterExc = true;
            callback.onException(e);
        }

        return null;
    }

    private boolean inRange(InvocationPolicy.StatusCodeRange[] ranges, int code) {
        for (InvocationPolicy.StatusCodeRange range : ranges) {
            if (inRange(range, code)) {
                return true;
            }
        }

        return false;
    }

    private boolean inRange(InvocationPolicy.StatusCodeRange range, int code) {
        return code >= range.low() && code <= range.high();
    }

    private boolean contains(int[] statusCodes, int search) {
        if (statusCodes == null || statusCodes.length == 0) {
            return false;
        }

        for (int statusCode : statusCodes) {
            if (statusCode == search) {
                return true;
            }
        }

        return false;
    }
}
