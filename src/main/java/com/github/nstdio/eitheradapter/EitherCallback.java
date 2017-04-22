package com.github.nstdio.eitheradapter;

import com.github.nstdio.eitheradapter.annotation.InvocationPolicy;

/**
 * {@linkplain EitherCallback} is used to notify the user of the successful or unsuccessful completion of the
 * asynchronous operation. Only one method can be called per request.
 *
 * @param <L> The first possible type of response.
 * @param <R> The second possible type of response.
 */
public interface EitherCallback<L, R> {

    /**
     * Called if the response status code is in {@link InvocationPolicy#left()} or if {@link InvocationPolicy#left()} is
     * empty, status code is in range {@link InvocationPolicy#leftRange()}. {@code left} might be {@code null} as a
     * result of unexpected data from the server and hence a conversion error.
     *
     * @param left The first possible response.
     *
     * @see InvocationPolicy#left()
     * @see InvocationPolicy#leftRange()
     */
    void onLeft(L left);

    /**
     * Called if the response status code is in {@link InvocationPolicy#right()} or if {@link InvocationPolicy#right()}
     * is empty, status code is in range {@link InvocationPolicy#rightRange()}. {@code right} might be {@code null} as a
     * result of unexpected data from the server and hence a conversion error.
     *
     * @param right The second possible response.
     *
     * @see InvocationPolicy#right()
     * @see InvocationPolicy#rightRange()
     */
    void onRight(R right);

    /**
     * Called if the response status code is not found in the {@link InvocationPolicy#left()}, {@link
     * InvocationPolicy#right()}, {@link InvocationPolicy#leftRange()}, {@link InvocationPolicy#rightRange()} or when
     * any exception occurred during the execution of the request or the processing of the response.
     *
     * @param t The exception occurred.
     */
    void onException(Throwable t);
}
