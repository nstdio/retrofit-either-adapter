package com.github.nstdio.eitheradapter;

import com.github.nstdio.eitheradapter.annotation.InvocationPolicy;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import retrofit2.http.GET;

import static org.junit.Assert.*;

public class EitherCallWithEmptyBoundsTest extends TestEnvironmentAware {

    private EmptyStatusCodeBoundsService emptyStatusCodeBoundsService;

    @Before
    public void setUp() throws Exception {
        emptyStatusCodeBoundsService = defaultRetrofit().create(EmptyStatusCodeBoundsService.class);
    }

    @Test
    public void emptyStatusCodeBounds() throws Exception {
        try {
            emptyStatusCodeBoundsService.shouldThrow().callback(allFailCallback);
            fail();
        } catch (Exception e) {
            assertThat(e, CoreMatchers.instanceOf(IllegalStateException.class));
            assertEquals(e.getMessage(), "Invocation policy has no bound for status code checking.");
        }
    }

    @SuppressWarnings("DefaultAnnotationParam")
    interface EmptyStatusCodeBoundsService {
        @GET("/")
        @InvocationPolicy(left = {}, right = {}, leftRange = {}, rightRange = {})
        EitherCall<Person, Problem> shouldThrow();
    }
}
