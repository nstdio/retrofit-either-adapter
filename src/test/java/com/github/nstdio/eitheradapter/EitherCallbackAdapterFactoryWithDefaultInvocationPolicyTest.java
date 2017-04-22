package com.github.nstdio.eitheradapter;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class EitherCallbackAdapterFactoryWithDefaultInvocationPolicyTest extends TestEnvironmentAware {

    private DefaultService defaultService;
    private DefaultNotParametrizedService defaultNotParametrizedService;
    private Person actualPerson;
    private Problem actualProblem;
    private Throwable actualThrowable;
    private EitherOnExceptionCallback<Person, Problem> onExceptionCallback;
    private EitherOnLeftCallback<Person, Problem> onLeftCallback;

    @Before
    public void setUp() throws Exception {
        Retrofit retrofit = defaultRetrofit();

        lock = new CountDownLatch(1);
        defaultService = retrofit.create(DefaultService.class);
        defaultNotParametrizedService = retrofit.create(DefaultNotParametrizedService.class);

        onExceptionCallback = new EitherOnExceptionCallback<Person, Problem>() {
            @Override
            public void onException(Throwable t) {
                actualThrowable = t;
                countDown();
            }
        };

        onLeftCallback = new EitherOnLeftCallback<Person, Problem>() {
            @Override
            public void onLeft(Person left) {
                actualPerson = left;
                countDown();
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        actualPerson = null;
        actualProblem = null;
        actualThrowable = null;
        defaultService = null;
        onExceptionCallback = null;
        onLeftCallback = null;
        lock = null;
    }

    @Test
    public void success() throws Exception {
        final Person person = new Person("John", "Doe");
        final MockResponse response = new MockResponse()
                .setBody(gson.toJson(person));

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(new EitherOnLeftCallback<Person, Problem>() {
            @Override
            public void onLeft(Person left) {
                actualPerson = left;
                countDown();
            }
        });

        await();

        assertNotNull(actualPerson);
        assertEquals(person.firstName, actualPerson.firstName);
        assertEquals(person.lastName, actualPerson.lastName);
    }

    @Test
    public void clientError() throws Exception {
        final Problem problem = new Problem("Validation error.");

        final MockResponse response = new MockResponse()
                .setResponseCode(422)
                .setBody(gson.toJson(problem));

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(new EitherOnRightCallback<Person, Problem>() {
            @Override
            public void onRight(Problem right) {
                actualProblem = right;
                countDown();
            }
        });

        await();

        assertNotNull(actualProblem);
        assertEquals(actualProblem.desc, problem.desc);
    }

    @Test
    public void serverError() throws Exception {
        final Problem problem = new Problem("Server error.");

        final MockResponse response = new MockResponse()
                .setResponseCode(500)
                .setBody(gson.toJson(problem));

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(new EitherOnRightCallback<Person, Problem>() {
            @Override
            public void onRight(Problem right) {
                actualProblem = right;
                countDown();
            }
        });

        await();

        assertNotNull(actualProblem);
        assertEquals(actualProblem.desc, problem.desc);
    }

    @Test
    public void disconnectAtStart() throws Exception {
        final MockResponse response = new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(new EitherOnExceptionCallback<Person, Problem>() {
            @Override
            public void onException(Throwable t) {
                actualThrowable = t;
                countDown();
            }
        });

        await(10);
        assertNotNull(actualThrowable);
    }

    @Test
    public void unknownStatusCode() throws Exception {
        final MockResponse response = new MockResponse()
                .setResponseCode(600);

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(new EitherOnExceptionCallback<Person, Problem>() {
            @Override
            public void onException(Throwable t) {
                actualThrowable = t;
                countDown();
            }
        });

        await();
        assertNotNull(actualThrowable);
        assertThat(actualThrowable, instanceOf(IllegalStateException.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void notParametrizedType() throws Exception {
        server.enqueue(new MockResponse());
        try {
            defaultNotParametrizedService
                    .shouldThrow()
                    .callback(new EitherFailAllCallback());

            fail("Exception doesn't thrown.");
        } catch (Exception e) {
            final Throwable cause = e.getCause();
            assertThat(cause, instanceOf(IllegalStateException.class));
            assertEquals(cause.getMessage(), "EitherCall return type must be parameterized as EitherCall<Foo>" +
                    " or EitherCall<? extends Foo>");
        }

    }

    @Test
    public void unexpectedBody() throws Exception {
        lock = new CountDownLatch(3);

        MockResponse response = new MockResponse()
                .setBody("123");

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(onExceptionCallback);

        await(2);
        assertNotNull(actualThrowable);

        actualThrowable = null;

        response = new MockResponse()
                .setResponseCode(422)
                .setBody("abc");

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(onExceptionCallback);

        await(2);
        assertNotNull(actualThrowable);
        actualThrowable = null;

        response = new MockResponse()
                .setBody(gson.toJson(new Problem("Desc")));

        server.enqueue(response);

        defaultService.eitherTokenOrProblem().callback(onLeftCallback);

        await(2);
        assertNotNull(actualPerson);
        assertNull(actualPerson.firstName);
        assertNull(actualPerson.lastName);
    }

    @Test
    public void emptyBody() throws Exception {
        server.enqueue(new MockResponse());

        actualPerson = new Person("First", "Last"); // to ensure that after callback execution is null.
        defaultService.eitherTokenOrProblem().callback(onLeftCallback);

        await(3);
        assertNull(actualPerson);
    }

    interface DefaultService {
        @GET("/")
        EitherCall<Person, Problem> eitherTokenOrProblem();
    }

    interface DefaultNotParametrizedService {
        @GET("/")
        EitherCall shouldThrow();
    }
}