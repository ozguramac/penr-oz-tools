package com.oz.mp.service;

import com.oz.mp.domain.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("mproximateService")
public class MproximateServiceImpl implements MproximateService {

    private static final Logger logger = LoggerFactory.getLogger(MproximateServiceImpl.class);

    @Autowired
    private EntityService entityService;

    @Override
    public float[] mp(Entity entity, final float[] Y) {
        final float[][] Φ = entity.getMeasurementMatrix(); //Nxd

        if (Y.length != Φ.length) {
            throw new IllegalStateException("Bad Dimensions!!");
        }

        final int d = Φ[0].length;
        final int N = Φ.length;

        //maximum trials
        final int K = d;

        //initialize residual Nx1 R = Y
        final float[] R = Arrays.copyOf(Y, N);

        //initialize reconstructed dx1 X
        final float[] X = new float[d];
        Arrays.fill(X, Float.NaN);

        //indices
        final int[] I = new int[d];
        Arrays.fill(I, -1);

        //get dxN ΦT
        final float[][] ΦT = entity.getMeasurementMatrixTransposed();

        //get dxd matrix G
        final float[][] G = entity.getMeasurementMatrixDotTransposed();

        //initialize dx1 vector α = ΦT.Y (dxN . Nx1 -> dx1)
        final float[] α = new float[d];
        for (int i = 0; i < d; i++) {
            α[i] = 0;
            for (int j = 0; j < N; j++) {
                α[i] += ΦT[i][j] * Y[j];
            }
        }

        //initialize dxd matrix G-1
        final float[][] G_1 = new float[d][d];

        if (isPrettyLog()) {
            //Pretty log
            logger.info("====== Initialize =============");
            log("Φ", Φ);
            log("ΦT", ΦT);
            log("ΦT.Φ -> G", G);
            log("~X", X);
            log("I", I);
            log("α", α);
            log("G-1", G_1);
            log("R", R);
        }

        //repeat for K rounds at most
        Mib min_mib = null;
        for (int k=0; k < K; k++)
        {
            if (isPrettyLog()) {
                logger.info("====== At iteration k=" + k + " =============");
            }

            //find measurement column: i = argmaxj (|α[j]|)
            Mib tmp_mib = null;
            float max_αj = Float.NEGATIVE_INFINITY;
            int ik = -1;
            for (int i=0; i < d; i++) { // ~Ik = {0...d}
                if (false == Float.isNaN(X[i]) ) {
                    continue; //except in set I
                }

                final float abs_αj = Math.abs(α[i]);
                if (max_αj < abs_αj) {
                    ik = i;
                    max_αj = abs_αj;
                }
            }

            tmp_mib = new Mib(k, G, G_1, ik, I, N, Φ, ΦT, X, Y);

            if (null == min_mib || min_mib.magSq_R > tmp_mib.magSq_R) {
                min_mib = tmp_mib;
            } else {
                if (isPrettyLog()) {
                    logger.info("Residue starting to diverge " + tmp_mib.magSq_R + " > " + min_mib.magSq_R + " Stopping...");

                }
                break;
            }

            if (null == min_mib || ik < 0) {
                if (isPrettyLog()) {
                    logger.info("No more max measurement column left to measure -> Stopping...");
                }
                break;
            }

            //Ik = Ik−1 U i
            I[k] = ik;

            final Mib mib = min_mib;

            //Calculate V.AT.A -> 1x1 . 1x(k-1) . (k-1)x1 -> 1x1
            float V_AT_A = 0;
            for (int p = 0; p < k; p++) {
                V_AT_A += mib.V * mib.A[p] * mib.A[p];
            }
            //Calculate G−1 Ik-1,Ik-1 += V.AT.A
            //Calculate G−1 Ik-1,ik = −V.AT
            //Calculate G−1 ik,Ik-1 = −V.A
            //Calculate G−1 ik,ik = V
            for (int p = 0; p < k; p++) { // Ik-1 = {I0...Ip...Ik-1}
                for (int j = 0; j < k; j++) { // Ik-1 = {I0...Ij...Ik-1}
                    G_1[I[p]][I[j]] += V_AT_A; //V.AT.A
                }
                //Ik-1, ik
                G_1[I[p]][ik] -= mib.V * mib.A[p]; //-V.AT (rows of last column up to I[k-1])
                //ik, Ik-1
                G_1[ik][I[p]] -= mib.V * mib.A[p]; //-V.A (columns of last row up to I[k-1])
            }
            //ik, ik
            G_1[ik][ik] += mib.V; //V bottom right corner at ik,ik

            //Calculate X Ik-1 += V.M.AT
            //Calculate X i = -V.M
            for (int j=0; j < k; j++) { // Ik-1 = {I0...Ij...Ik-1}
              X[I[j]] += mib.V*mib.M*mib.A[j]; //V.M.AT (columns)
            }
            X[ik] = -(mib.V*mib.M); //-V.M at X ik

            //For [VM.AT -VM] -> k-1x1 + 1x1 -> kx1
            //For G ~Ik,Ik .[V.M.AT -V.M] -> (d-k)xk . kx1 -> (d-k)x1
            //~Ik is complement set meaning indices 1 to d except the ones in set I
            //G ~Ik,Ik is rows of G not in set I with columns of G in set I
            //Calculate α k -= G ~Ik,Ik .[V.M.AT -V.M]
            for (int p = 0; p < d; p++) { // ~Ik = {0....~Ip....d}
                if (false == Double.isNaN(X[p])) {
                    continue; //except in set I
                }

                for (int j = 0; j < k; j++) { //Ik = {I0...Ij...Ik}
                    α[p] -= G[p][I[j]] * mib.V * mib.M * mib.A[j];
                }
                α[p] -= G[p][ik] * -mib.V * mib.M;
            }

            if (isPrettyLog()) {
                //Pretty log
                log("A", mib.A);
                logger.info("1x1 V = " + mib.V);
                logger.info("1x1 M = " + mib.M);
                log("~X", X);
                log("I", I);
                log("α", α);
                log("G-1", G_1);
                log("R", mib.R);
            }

            if (isPrettyLog()) {
                logger.info("||R||2 = "+mib.magSq_R);
            }
        }

        //Fill in blanks for reconstructed X
        for (int i=0; i < d; i++) {
            if ( Float.isNaN(X[i]) ) {
                X[i] = 0;
            }
        }

        if (isPrettyLog()) {
            //Pretty log X
            log("~X", X);
        }

        return X;
    }


    private static class Mib {
        final float[] A, R;
        final float V, M, magSq_R;

        Mib(final int k, final float[][] G, final float[][] G_1, final int ik, final int[] I, final int N,
            final float[][] Φ, final float[][] ΦT, final float[] X, final float[] Y)
        {
            //A is 1x(k-1) vector
            //For G.G-1 -> 1xk-1 . k-1xk-1 -> 1x(k-1)
            //Calculate A = G ik,Ik−1 . G−1 Ik−1,Ik−1
            A = new float[k];
            for (int p=0; p < k; p++) { // Ik-1 = {I0...Ip...Ik-1}
                A[p] = 0;
                for (int j=0; j < k; j++) { // Ik-1 = {I0...Ij...Ik-1}
                    A[p] += G[ik][I[j]] * G_1 [I[p]][I[j]];
                }
            }

            //V is 1x1 scalar
            //For G.G-1 ->  1xk-1 . k-1xk-1 . k-1x1 -> 1x1
            //Calculate V = 1 / (G ik,ik − A . G Ik−1,i )
            float A_G_Ik_1_Ik = 0;
            for (int p=0; p < k; p++) {  // Ik-1 = {I0...Ip...Ik-1}
                A_G_Ik_1_Ik += A[p] * G[I[p]][ik];
            }
            if (G[ik][ik] == A_G_Ik_1_Ik) {
                V = 0;
            }
            else {
                V = 1 / (G[ik][ik] - A_G_Ik_1_Ik);
            }

            //M is 1x1 scalar
            //For ΦT ik.Y -> 1xN . Nx1 -> 1x1
            //For A.ΦT Ik−1.Y -> 1x(k-1) . (k-1)xN . Nx1 -> 1x1
            //Calculate M = A. ΦT Ik−1 .Y − ΦT ik .Y
            float[] ΦT_Ik_1_Y = new float[k];
            for (int p=0; p < k; p++) {  // Ik-1 = {I0...Ip...Ik-1}
                ΦT_Ik_1_Y[p] = dot(ΦT[I[p]], Y);
            }
            M = dot(A, ΦT_Ik_1_Y) - dot( ΦT[ik], Y );

            //X-hat -> X Ik
            //Φ Ik . X-hat -> Nxk . kx1 -> Nx1
            //Update R = Y − Φ Ik . X-hat
            R = new float[N];
            for (int p=0; p < N; p++) { //Φ Ik . X-hat
                R[p] = Y[p];
                for (int j=0; j < k; j++) { // Ik-1 = {I0...Ip...Ik-1}
                    R[p] -= Φ[p][I[j]] * (X[I[j]] + V*M*A[j]);
                }
                R[p] -= Φ[p][ik] * -(V*M); //ik
            }

            //Calculate ||R||2
            float sumSq = 0;
            for (int j=0; j < N; j++) {
                sumSq += R[j] * R[j];
            }
            magSq_R = sumSq;
        }
    }

    //S = V1.V2 (1xd dot dx1 is 1x1 scalar)
    private static float dot(final float[] V1, final float[] V2) {
        final int d;
        if (null == V1 || null == V2 || (d = V1.length) != V2.length || d < 0) {
            throw new IllegalArgumentException("Bad Dimensions!!");
        }

        if (d == 0) {
            return 0;
        }

        float S = 0;
        for (int i=0; i < d; i++) {
            S += V1[i] * V2[i];
        }
        return S;
    }

    //Pretty log vector
    private void log(final String name, final float[] V) {
        logger.info(V.length +"x1 "+name + " = " + Arrays.toString(V));
    }
    private void log(final String name, final int[] V) {
        logger.info(V.length +"x1 "+name + " = " + Arrays.toString(V));
    }

    //Pretty log matrix
    private void log(final String name, final float[][] M) {
        final StringBuffer sb = new StringBuffer()
                .append(M.length).append('x').append(M[0].length).append(' ').append(name).append(" = ");

        for (int i=0; i < M.length; i++) {
            sb.append('\n').append(Arrays.toString(M[i]));
        }

        logger.info(sb.toString());
    }
    
    boolean isPrettyLog() {
        return false; //Testing only
    }
}
