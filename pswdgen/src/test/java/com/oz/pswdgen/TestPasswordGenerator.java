package com.oz.pswdgen;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by consultant on 11/24/2016.
 */
public class TestPasswordGenerator {

    private final PasswordGenerator pg = new PasswordGenerator("PeNR^0z_G3n");

    @Test(expected = SecurityException.class)
    public void verifyBadKey() {
        new PasswordGenerator("Idontknowthekey:-D");
    }

    @Test(expected = SecurityException.class)
    public void verifyNullInput() throws Exception {
        pg.generate(null, null);
    }

    @Test(expected = SecurityException.class)
    public void verifyZeroInput() throws Exception {
        pg.generate("", "");
    }

    @Test(expected = SecurityException.class)
    public void verifyWhitespaceInput() throws Exception {
        pg.generate("    ", "   ");
    }

    @Test(expected = SecurityException.class)
    public void verifyZeroKey() throws Exception {
        final String k = "", s = "!StA1Rs666$*";
        pg.generate(k, s);
    }

    @Test(expected = SecurityException.class)
    public void verifyZeroSalt() throws Exception {
        final String k = "SomeKey1234", s = "";
        pg.generate(k, s);
    }

    @Test
    public void verifyDeterministicGen() throws Exception {
        final String k = "hamza_kabizogullari@derinworksllc.com", s = "!StA1Rs666$*", p = "{1rO5;uZq2+V[3oK3{cBw4)K?1oO6;";
        final String gp = pg.generate(k, s);
        assertRequirements(gp);
        Assert.assertEquals(p, gp);
    }

    @Test
    public void verifyShortKey() throws Exception {
        final String k = "@", s = "!StA1Rs666$*", p = "_0kD4[nMy2?C_5kH4|nRy9?T";
        final String gp = pg.generate(k, s);
        assertRequirements(gp);
        Assert.assertEquals(p, gp);
    }

    @Test
    public void verifyShortSalt() throws Exception {
        final String k = "hamza_kabizogullari@derinworksllc.com", s = "~", p = "{3rB5:uUq2+B[3oB3:cUw2)B?3oB6:";
        final String gp = pg.generate(k, s);
        assertRequirements(gp);
        Assert.assertEquals(p, gp);
    }

    @Test
    public void verifyLongKey() throws Exception {
        final String k = "ThisIsAReallyLOooooong1234678KEEeeeYYYY:-)", s = "!StA1Rs666$*", p = "?3iC0!aMs3+Z,6mA";
        final String gp = pg.generate(k, s);
        assertRequirements(gp);
        Assert.assertEquals(p, gp);
    }

    @Test
    public void verifyLongSalt() throws Exception {
        final String k = "!StA1Rs666$*", s = "ThisIsAReallyLOooooong1234678SAaaaaLLLTTT:-)", p = "=2iC1+vOy1";
        final String gp = pg.generate(k, s);
        assertRequirements(gp);
        Assert.assertEquals(p, gp);
    }

    private static void assertRequirements(final String gp) {
        Assert.assertTrue(gp.length() <= PasswordGenerator.MAX_LEN && gp.length() >= PasswordGenerator.MIN_LEN);
        Assert.assertTrue(containsAny(gp, "abcdefghijklmnopqrstuvwxyz"));
        Assert.assertTrue(containsAny(gp, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        Assert.assertTrue(containsAny(gp, "0123456789"));
        Assert.assertTrue(containsAny(gp, "~!@#$%^&*()_+=-{}|:<>?,./';[]"));
    }

    private static boolean containsAny(final String gp, final String required) {
        for (char c : required.toCharArray()) {
            if (gp.indexOf(c) > -1) {
                return true;
            }
        }
        return false;
    }
}
