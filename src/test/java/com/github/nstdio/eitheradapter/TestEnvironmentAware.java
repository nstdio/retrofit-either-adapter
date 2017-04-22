package com.github.nstdio.eitheradapter;

import com.google.gson.Gson;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class TestEnvironmentAware {
    @Rule
    public final MockWebServer server = new MockWebServer();

    final Gson gson = new Gson();
    final EitherCallback<Person, Problem> allFailCallback = new EitherFailAllCallback<Person, Problem>();
    CountDownLatch lock = new CountDownLatch(1);

    Person actualPerson;
    Problem actualProblem;
    Throwable actualThrowable;

    EitherCallback<Person, Problem> onLeftCallback =
            new EitherOnLeftCallback<Person, Problem>() {
                @Override
                public void onLeft(Person left) {
                    actualPerson = left;
                    countDown();
                }
            };

    EitherCallback<Person, Problem> onExceptionCallback =
            new EitherOnExceptionCallback<Person, Problem>() {
                @Override
                public void onException(Throwable t) {
                    actualThrowable = t;
                    countDown();
                }
            };

    EitherCallback<Person, Problem> onRightCallback = new EitherOnRightCallback<Person, Problem>() {
        @Override
        public void onRight(Problem right) {
            actualProblem = right;
            countDown();
        }
    };

    Retrofit defaultRetrofit() {
        final GsonConverterFactory factory = GsonConverterFactory.create(gson);

        return new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addCallAdapterFactory(EitherCallAdapterFactory.create())
                .addConverterFactory(factory)
                .build();
    }

    MockResponse mockResponse() {
        return new MockResponse();
    }

    void countDown() {
        lock.countDown();
    }

    void await() throws InterruptedException {
        await(1);
    }

    void await(final int seconds) throws InterruptedException {
        lock.await(seconds, TimeUnit.SECONDS);
    }

    abstract static class EitherOnLeftCallback<L, R> implements EitherCallback<L, R> {

        @Override
        abstract public void onLeft(L left);

        @Override
        public void onRight(R right) {
            fail("onRight called.");
        }

        @Override
        public void onException(Throwable t) {
            fail("onException called.");
        }
    }

    abstract static class EitherOnRightCallback<L, R> implements EitherCallback<L, R> {

        @Override
        public void onLeft(L left) {
            fail("onLeft called.");
        }

        @Override
        abstract public void onRight(R right);

        @Override
        public void onException(Throwable t) {
            fail("onException called.");
        }
    }


    abstract static class EitherOnExceptionCallback<L, R> implements EitherCallback<L, R> {

        @Override
        public void onLeft(L left) {
            fail("onLeft called.");
        }

        @Override
        public void onRight(R right) {
            fail("onRight called.");
        }

        @Override
        abstract public void onException(Throwable t);
    }


    static class EitherFailAllCallback<L, R> implements EitherCallback<L, R> {

        @Override
        public void onLeft(L left) {
            fail("onLeft called.");
        }

        @Override
        public void onRight(R right) {
            fail("onRight called.");
        }

        @Override
        public void onException(Throwable t) {
            fail("onException called.");
        }
    }

    static class Person {
        final String firstName;
        final String lastName;

        Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    static class Problem {
        final String desc;

        Problem(String desc) {
            this.desc = desc;
        }
    }
}
