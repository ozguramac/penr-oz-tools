package com.oz.mp.domain;

public abstract class Entity {

    private static final long serialVersionUID = 1L;

    private float[][] measurementMatrix = null;
    private float[][] measurementMatrixTransposed = null;
    private float[][] measurementMatrixDotTransposed = null;

    public abstract String getSerialNumber();

    public abstract String getSecret();

    public abstract float[][] getDctMatrixInverse();

    public abstract short[][] getSensingMatrix();

    public float[][] getMeasurementMatrix() {
        if (null == measurementMatrix && getSensingMatrix() != null) {
            final short[][] A = getSensingMatrix();
            final int N = A.length;
            final int d = A[0].length;
            final float[][] dct_1 = getDctMatrixInverse();

            //initialize dxd matrix Φ = A.D^-1 (Nxd . dxd -> Nxd)
            final float[][] Φ = new float[N][d];
            for (int i=0; i < N; i++) {
                for (int j=0; j < d; j++) {
                    Φ[i][j] = 0;
                    for (int k=0; k < d; k++) {
                        Φ[i][j] += A[i][k] * dct_1[k][j];
                    }
                }
            }
            measurementMatrix = Φ; //cached

        }
        return measurementMatrix;
    }

    public float[][] getMeasurementMatrixTransposed() {
        if (null == measurementMatrixTransposed && getMeasurementMatrix() != null) {
            final float[][] Φ = getMeasurementMatrix();
            final int d = Φ[0].length;
            final int N = Φ.length;

            //initialize dxN ΦT
            final float[][] ΦT = new float[d][N];
            for (int i=0; i < N; i++) {
                for (int j=0; j < d; j++) {
                    ΦT[j][i] = Φ[i][j];
                }
            }
            measurementMatrixTransposed = ΦT; //cached
        }
        return measurementMatrixTransposed;
    }

    public float[][] getMeasurementMatrixDotTransposed() {
        if (null == measurementMatrixDotTransposed && getMeasurementMatrix() != null && getMeasurementMatrixTransposed() != null) {
            final float[][] Φ = getMeasurementMatrix();
            final int d = Φ[0].length;
            final int N = Φ.length;
            final float[][] ΦT = getMeasurementMatrixTransposed();

            //initialize dxd matrix G = ΦT.Φ (dxN . Nxd -> dxd)
            final float[][] G = new float[d][d];
            for (int i=0; i < d; i++) {
                for (int j=0; j < d; j++) {
                    G[i][j] = 0;
                    for (int n=0; n < N; n++) {
                        G[i][j] += ΦT[i][n] * Φ[n][j];
                    }
                }
            }
            measurementMatrixDotTransposed = G; //cached
        }

        return measurementMatrixDotTransposed;
    }
}
