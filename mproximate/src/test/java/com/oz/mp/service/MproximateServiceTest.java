package com.oz.mp.service;

import com.oz.mp.domain.Entity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MproximateServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(MproximateServiceTest.class);

    @Mock
    private EntityService mockEntitySvc;
    @InjectMocks @Spy
    private final MproximateService svc = new MproximateServiceImpl();

    @Spy
    private Entity entity;

    private final String userName = "AnyUser666";
    private final String code = "SMW666GOT";

    @Before
    public void setUp() throws Exception {
        doReturn(true).when((MproximateServiceImpl)svc).isPrettyLog();
        when(mockEntitySvc.get(eq(userName), eq(code))).thenReturn(entity);
    }

    @Test
    public void test() throws Exception {
        //Device measurement matrix
        final float[][] Φ = {
             {-0.707f,   0.8f,     0}
            ,{ 0.707f,   0.6f,    -1}
        };
        when(entity.getMeasurementMatrix()).thenReturn(Φ);

        //original
        final float[] X = { -1.2f, 1, 0 };
        logger.info("X = " + Arrays.toString(X));

        final float tolerance = 0.1f;

        assertReconstructed(Φ, X, tolerance, 0.0f, X.length);
    }

    @Test
    public void test2() throws Exception {
        //Device measurement matrix
        final float[][] Φ = {
             { 0, 1, 1,-1,-1, 0,-1, 0,-1, 0}
            ,{-1,-1, 0, 1,-1, 0, 0,-1, 0, 1}
            ,{ 1,-1, 1,-1, 0,-1, 1, 1, 0, 0}
            ,{ 1, 0,-1, 0, 0, 1,-1,-1, 1, 1}
            ,{-1, 0, 0, 0, 1, 0, 1, 0, 1,-1}
            ,{ 0, 0,-1,-1,-1, 0,-1, 1,-1, 0}
        };
        when(entity.getMeasurementMatrix()).thenReturn(Φ);

        //original
        final float[] X = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 };
        logger.info(X.length+"x1 X = " + Arrays.toString(X));

        final float tolerance = 0.3f;

        assertReconstructed(Φ, X, tolerance, 0.0f, X.length);
    }

    @Test
    public void test3() throws Exception {
        //Device measurement matrix
        final float[][] Φ = {
                { 0, 1, 1,-1,-1, 0,-1, 0,-1, 0}
                ,{-1,-1, 0, 1,-1, 0, 0,-1, 0, 1}
                ,{ 1,-1, 1,-1, 0,-1, 1, 1, 0, 0}
                ,{ 1, 0,-1, 0, 0, 1,-1,-1, 1, 1}
                ,{-1, 0, 0, 0, 1, 0, 1, 0, 1,-1}
                ,{ 0, 0,-1,-1,-1, 0,-1, 1,-1, 0}
        };
        when(entity.getMeasurementMatrix()).thenReturn(Φ);

        //original
        final float[] X = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 };
        logger.info(X.length+"x1 X = " + Arrays.toString(X));

        final float tolerance = 0.3f;

        assertReconstructed(Φ, X, tolerance, 0.0f, 100);
    }

    private void assertReconstructed(float[][] φ, float[] X, float tolerance, float neutral, int maxTrials) {
        //check size
        final int N = φ.length;
        Assert.assertTrue(N > 0);
        final int d = X.length;
        Assert.assertTrue(d > 0);

        //compress
        // Nx1 Y = X.Φ
        final float[] Y = new float[N];
        for (int i=0; i < N; i++) {
            Y[i] = 0;
            for (int j=0; j < d; j++) {
                Y[i] += X[j] * φ[i][j];
            }
        }
        logger.info(N+"x1 Y = " + Arrays.toString(Y));

        //approximate
        final float[] _X = svc.mp(entity, Y);
        logger.info("      Original X = " + Arrays.toString(X));
        logger.info("Reconstructed ~X = " + Arrays.toString(_X));

        //check size
        Assert.assertEquals(d, _X.length);

        //check tolerance met
        final float similarFactor = cosineSimilarity(_X, X);
        logger.info("cosSimilar(~X, X) = " + similarFactor);

        Assert.assertEquals("Approximation not within acceptable tolerance", 1.0, similarFactor, tolerance);
    }

    private static float cosineSimilarity(final float[] V1, final float[] V2) {
        final int N = V1.length;
        Assert.assertEquals(N, V2.length);

        float dotSum = 0, sqSum1 = 0, sqSum2 = 0;
        for (int i=0; i < N; i++) {
            dotSum += V1[i] * V2[i];
            sqSum1 += V1[i] * V1[i];
            sqSum2 += V2[i] * V2[i];
        }

        return dotSum / ((float)(Math.sqrt(sqSum1) * Math.sqrt(sqSum2)));
    }
}

