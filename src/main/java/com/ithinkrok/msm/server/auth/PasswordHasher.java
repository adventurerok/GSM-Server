package com.ithinkrok.msm.server.auth;

import com.lambdaworks.crypto.SCrypt;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Created by paul on 16/02/15.
 */
public class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int SCRYPT_N = 1 << 16; //General work factor, iteration count.
    public static final int SCRYPT_R = 8; //blocksize in use for underlying hash; fine-tunes the relative memory-cost.
    public static final int SCRYPT_P = 1; //parallelization factor; fine-tunes the relative cpu-cost.

    public static class ScryptParameters {
        public byte[] salt;
        public int n, r, p;

        public void generateParameters(){
            salt = new byte[32];
            RANDOM.nextBytes(salt);

            n = SCRYPT_N;
            r = SCRYPT_R;
            p = SCRYPT_P;
        }
    }


    public static byte[] hash(byte[] password, byte[] salt, int n, int r, int p){
        try {
            return SCrypt.scrypt(password, salt, n, r, p, 32);
        } catch (GeneralSecurityException ignored) {
            throw new RuntimeException("Incorrect Scrypt parameters");
        }
    }

    public static byte[] hash(byte[] password, ScryptParameters params){
        return hash(password, params.salt, params.n, params.r, params.p);
    }

}
