package com.github.nstdio.eitheradapter;

import com.github.nstdio.eitheradapter.annotation.InvocationPolicy;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class EitherCallAdapterFactory extends CallAdapter.Factory {

    public static final Annotation[] ANNOTATIONS = new Annotation[0];

    private EitherCallAdapterFactory() {
    }

    public static EitherCallAdapterFactory create() {
        return new EitherCallAdapterFactory();
    }

    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        final Class<?> rawType = getRawType(returnType);
        if (rawType != EitherCall.class) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException("EitherCall return type must be parameterized"
                    + " as EitherCall<Foo> or EitherCall<? extends Foo>");
        }
        final Type leftType = getParameterUpperBound(0, (ParameterizedType) returnType);
        final Type rightType = getParameterUpperBound(1, (ParameterizedType) returnType);

        final Converter<ResponseBody, ?> left = retrofit.responseBodyConverter(leftType, ANNOTATIONS);
        final Converter<ResponseBody, ?> right = retrofit.responseBodyConverter(rightType, ANNOTATIONS);

        final InvocationPolicy statusCode = annotated(annotations);

        return new EitherCallAdapter(left, right, statusCode);
    }

    private InvocationPolicy annotated(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof InvocationPolicy) {
                return (InvocationPolicy) annotation;
            }
        }

        return new InvocationPolicy() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return InvocationPolicy.class;
            }

            @Override
            public int[] left() {
                return new int[0];
            }

            @Override
            public int[] right() {
                return new int[0];
            }

            @Override
            public StatusCodeRange[] leftRange() {
                return new StatusCodeRange[]{StatusCodeRange.SUCCESS, StatusCodeRange.REDIRECT};
            }

            @Override
            public StatusCodeRange[] rightRange() {
                return new StatusCodeRange[]{StatusCodeRange.CLIENT_ERROR, StatusCodeRange.SERVER_ERROR};
            }
        };
    }

    private class EitherCallAdapter<L, R> implements CallAdapter<ResponseBody, EitherCall<L, R>> {
        private final Converter<ResponseBody, L> left;
        private final Converter<ResponseBody, R> right;
        private final InvocationPolicy statusCode;

        private EitherCallAdapter(Converter<ResponseBody, L> left,
                                  Converter<ResponseBody, R> right, InvocationPolicy statusCode) {
            this.left = left;
            this.right = right;
            this.statusCode = statusCode;
        }

        public Type responseType() {
            return ResponseBody.class;
        }

        @Override
        public EitherCall<L, R> adapt(Call<ResponseBody> call) {
            return new EitherCall<L, R>(call, left, right, statusCode);
        }
    }
}
