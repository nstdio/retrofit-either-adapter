package com.github.nstdio.eitheradapter;

import com.github.nstdio.eitheradapter.annotation.InvocationPolicy;
import okhttp3.mockwebserver.MockResponse;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.http.GET;
import retrofit2.http.POST;

import static org.junit.Assert.*;

public class EitherCallWithExplicitBoundsTest extends TestEnvironmentAware {
    private Service1 service;
    private MockResponse mockResponse;


    @Before
    public void setUp() throws Exception {
        service = defaultRetrofit().create(Service1.class);
        mockResponse = mockResponse();
    }

    @After
    public void tearDown() throws Exception {
        actualPerson = null;
        actualProblem = null;
        actualThrowable = null;
    }

    @Test
    public void specificStatusCodes() throws Exception {
        final Person person = new Person("First", "Last");

        mockResponse
                .setResponseCode(422)
                .setBody(gson.toJson(person));

        server.enqueue(mockResponse);
        service.call().callback(onLeftCallback);

        await();

        assertNotNull(actualPerson);
        assertEquals(actualPerson.firstName, person.firstName);
        assertEquals(actualPerson.lastName, person.lastName);
    }

    @Test
    public void leftSpecifiedRightNot() throws Exception {
        final Problem problem = new Problem("Internal Server Error.");
        mockResponse
                .setResponseCode(500)
                .setBody(gson.toJson(problem));

        server.enqueue(mockResponse);

        defaultRetrofit().create(Service2.class)
                .call()
                .callback(new EitherCallback<Person, Problem>() {
                    @Override
                    public void onLeft(Person left) {
                        fail();
                    }

                    @Override
                    public void onRight(Problem right) {
                        actualProblem = right;
                        countDown();
                    }

                    @Override
                    public void onException(Throwable t) {
                        fail();
                    }
                });

        await();

        assertNotNull(actualProblem);
        assertEquals(actualProblem.desc, problem.desc);
    }


    @Test
    public void notInSingleSpecifiedLeft() throws Exception {
        Person person = new Person("First", "Last");

        mockResponse
                .setResponseCode(200)
                .setBody(gson.toJson(person));

        server.enqueue(mockResponse);
        defaultRetrofit().create(Service3.class)
                .call()
                .callback(onExceptionCallback);

        await(2);

        assertNotNull(actualThrowable);
    }

    @Test
    public void notContains() throws Exception {
        final int code = 201;
        mockResponse.setResponseCode(code);

        server.enqueue(mockResponse);
        defaultRetrofit().create(Service4.class)
                .call()
                .callback(onExceptionCallback);

        await();
        assertNotNull(actualThrowable);
        assertThat(actualThrowable, CoreMatchers.instanceOf(IllegalStateException.class));
        assertEquals(actualThrowable.getMessage(), "Either left nor right does not contain response status code: " + code);
    }

    @Test
    public void rightContains() throws Exception {
        Problem problem = new Problem("Validation Error.");

        mockResponse.setResponseCode(422)
                .setBody(gson.toJson(problem));

        server.enqueue(mockResponse);

        defaultRetrofit().create(Service5.class)
                .call()
                .callback(onRightCallback);

        await();
        assertNotNull(actualProblem);
        assertEquals(problem.desc, actualProblem.desc);
    }

    interface Service1 {
        @GET("/")
        @InvocationPolicy(left = 422, right = 200)
        EitherCall<Person, Problem> call();
    }

    interface Service2 {
        @GET("/")
        @InvocationPolicy(left = {200, 201})
        EitherCall<Person, Problem> call();
    }

    interface Service3 {
        @POST("/")
        @InvocationPolicy(left = 201)
        EitherCall<Person, Problem> call();
    }

    interface Service4 {
        @POST("/")
        @InvocationPolicy(left = 200, right = 401)
        EitherCall<Person, Problem> call();
    }

    interface Service5 {
        @GET("/")
        @InvocationPolicy(left = 200, right = {422, 401})
        EitherCall<Person, Problem> call();
    }
}
