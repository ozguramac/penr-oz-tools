package com.oz.pswdgen;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Created by consultant on 11/24/2016.
 */
public class PasswordGenerator {

    private static final int ASCII_BEGIN = '!';
    private static final int ASCII_END = '~';
    private static final char[][] HASH_MATRIX = {
         ";+_)%|$!%$+=[:;+=[:|?/,<,>{?};+_)%|$!%$;;+_)%|$!|?/,<,>{?} %+_$+=[:;|?/,<,>{?};)%|$!%$+=[:;|?/,".toCharArray()
        ,"1923847560192384756923847560192384756019201923847560923856013847474756019192384756012385601923".toCharArray()
        ,"zsqrthgbawdxevcfrtnawdxevcfymujkiolmpzsqawdzsqhgbnymujkicfrthgbnolmpxevymujkiolmevcfrtpzsqawdx".toCharArray()
        ,"ACZDSXQWRFVEBTGYHNUJMKIOLMPACZDSXQWRFVEBTGYHNUJMKIOLMPACZDSXQWRFVEBTGYHNUJMKIOLMPACZDSXQWRFVEB".toCharArray()
        ,"9231923847560147560195601919238475601238560192323847847569238575609238560138474746019201923847".toCharArray()
        ,";|?/,<,>{?};)%|$!%$+=[:;|?/;+_)%|$)%|$!%$;;+_)%|$!%$+=[:;+=[:|?/,<,>{?};+_!|?/,<,>{?} %+_$+=[:,".toCharArray()
        ,"ymujkiolmpzsqzsqrthgbawdxevcfrtnawdxevcfawdzsqhgxevymujkiolmevcfrtpzsqawdxbnymujkicfrthgbnolmp".toCharArray()
        ,"MKIOLMPACZDSXACJMKIOLMPACZDSXQWRFVEBTGYHNUJQWRFVEBZDSXQWRFVEBTGYHNUJMKIOLMPACZDSXQWRFVEBTGYHNU".toCharArray()
        ,"ymujkicfrthgbnzsqrthgbawolmpxevymujkiolmevcfrtpzsqawdxdxevcfrtnawdxevcfymujkiolmpzsqawdzsqhgbn".toCharArray()
        ,"3192384756019238475692384756019238560138474747560191923847560123856019238475601920192384756092".toCharArray()
        ,"<,>{?};)%;+_)%|$!%$%$;;+_)%|$!|?/,<,>{?} %+_$+=[:;|?/,|$!%$+=[:;|?/+=[:;+=[:|?/,<,>{?};+_)%|$!,".toCharArray()
        ,"TGYHNUJMKIOACZDSXQWRFVEBTGYHNUJMKIOLMPACZLMPACZDSXQWRFVEBDSXQWRFVEBTGYHNUJMKIOLMPACZDSXQWRFVEB".toCharArray()
    };

    private static final int[] MAGIC_SWAP = { 4, 3, 6, 1, 0, 2, 8, 9, 5, 7 };

    static final int MIN_LEN = 9;
    static final int MAX_LEN = 32;

    public static void main(String[] args){
        if(args == null || args.length != 3){
            System.out.println("Usage: java -jar pswdgen-1.0.0-SNAPSHOT.jar [secret] [salt] [fileName]");
            System.out.println("Note: the input file should contain the username one per line without any additional character");
            return;
        }else{
            final String secret = args[0];
            final PasswordGenerator generator;
            try {
                generator = new PasswordGenerator(secret);
            } catch (SecurityException exe){
                System.err.println("Could not create password generator due to "+exe.getMessage());
                return;
            }

            final String salt = args[1];
            final String fileName = args[2];
            try (final Stream<String> stream = Files.lines(Paths.get(fileName))) {
                final Path path = Paths.get(fileName + ".result");
                final BufferedWriter writer = Files.newBufferedWriter(path);
                stream.forEach(line -> generator.processLine(salt, writer, line));
                writer.close();
                System.out.println("A result file has been generated: " + path.toString());
            } catch (IOException exe){
                System.err.println("Could process file "+fileName+" due to "+exe.getMessage());
            }
        }
    }

    private void processLine(final String salt, final BufferedWriter writer, final String line) {
        if(line == null || line.trim().length() == 0){
            return;
        }

        final String password;
        try {
            password = generate(line, salt);
        } catch (SecurityException exe){
            System.err.println("Could not generate a password for "+line+" due to "+exe.getMessage());
            return;
        }

        try {
            writer.write(line + " : " + password);
            writer.newLine();
        } catch (IOException exe) {
            System.err.println("Could not write a line out for "+line+" due to "+exe.getMessage());
        }
    }

    /**
     * Constructs password generator
     * @param secret - Pass in secret to be able to instantiate
     */
    public PasswordGenerator(final String secret) {
        if (false == "PeNR^0z_G3n".equals(secret)) {
            throw new SecurityException("Access denied: Cannot instantiate without correct secret!!");
        }
    }

    /**
     * Generates deterministic password based on key and salt
     * @param key - some alphanumeric string
     * @param salt - same alphanumeric string with special characters shared by apps
     *             that want same generated password for same inputs
     * @return deterministically generated password based on inputs
     */
    public String generate(final String key, final String salt) {
        if (false == isValid(key) || false == isValid(salt)) {
            throw new SecurityException("Please do not specify null or whitespace input!");
        }

        final StringBuffer sbn = normalize(key, salt);
        final char[] gend = new char[sbn.length()];
        for (int i = sbn.length() - 1; i >= 0 ; i--) {
            final int a = Math.min(sbn.charAt(i), ASCII_END) - ASCII_BEGIN;
            final int h = i%HASH_MATRIX.length;
            gend[i] = HASH_MATRIX[h][a];
        }
        return new String(gend);
    }

    private StringBuffer normalize(final String key, final String salt) {
        final int keyLen = key.length();
        final int saltLen = salt.length();
        final int maxLen = Math.max(keyLen, saltLen);
        final int magicLen = MIN_LEN + magicSwap(maxLen)%(MAX_LEN - MIN_LEN);

        final StringBuffer sb = new StringBuffer();
        for (int i = magicLen/2; i >= 0 ; i--) {
            sb.append( key.charAt(i%keyLen) );
            sb.append( salt.charAt(i%saltLen) );
        }

        return sb;
    }

    private int magicSwap(int num) {
        int res = 0;
        for (int f = 1; num > 0; num /= 10, f *=10) {
            final int d = num%10;
            res += MAGIC_SWAP[d] * f;
        }
        return res;
    }

    private static boolean isValid(final String s) {
        return s != null && s.trim().length() > 0;
    }
}
