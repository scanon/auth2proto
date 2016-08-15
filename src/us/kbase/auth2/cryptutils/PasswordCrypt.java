package us.kbase.auth2.cryptutils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordCrypt {
	
	/* code mostly stolen from
	 * https://www.javacodegeeks.com/2012/05/secure-password-storage-donts-dos-and.html
	 */

	//TODO ONMOVE unit tests
	//TODO ONMOVE docs
	
	/* PBKDF2 with SHA-1 as the hashing algorithm. Note that the NIST
	 * specifically names SHA-1 as an acceptable hashing algorithm for
	 * PBKDF2
	 */
	private static final String CRYPTALG = "PBKDF2WithHmacSHA1";
	private final SecretKeyFactory keyfac;
	
	// VERY important to use SecureRandom instead of just Random
	private final SecureRandom random;
	

	public PasswordCrypt() throws NoSuchAlgorithmException {
		random = SecureRandom.getInstance("SHA1PRNG");
		keyfac = SecretKeyFactory.getInstance(CRYPTALG);
	}
	
	public boolean authenticate(
			final char[] attemptedPassword,
			final byte[] encryptedPassword,
			final byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Encrypt the clear-text password using the same salt that was used to
		// encrypt the original password
		byte[] encryptedAttemptedPassword =
				getEncryptedPassword(attemptedPassword, salt);

		// Authentication succeeds if encrypted password that the user entered
		// is equal to the stored hash
		return equals(encryptedPassword, encryptedAttemptedPassword);
	}

	/* pass all the way through the array rather than stopping at the first
	 * error as this allows for a timing attack
	 * https://crackstation.net/hashing-security.htm
	 */
	private boolean equals(
			final byte[] encryptedPassword,
			final byte[] encryptedAttemptedPassword) {
		if (encryptedAttemptedPassword == null || encryptedPassword == null) {
			throw new NullPointerException("Passwords cannot be null");
		}
		if (encryptedAttemptedPassword.length !=
				encryptedAttemptedPassword.length) {
			throw new ArrayIndexOutOfBoundsException(
					"Encrypted passwords are not the same length");
		}
		boolean eq = true;
		for (int i = 0; i < encryptedAttemptedPassword.length; i++)
			if (encryptedAttemptedPassword[i] != encryptedPassword[i]) {
				eq = false;
			}
		return eq;
	}

	public byte[] getEncryptedPassword(char[] password, byte[] salt) {
		if (password == null || salt == null) {
			throw new NullPointerException("password and salt cannot be null");
		}
		// SHA-1 generates 160 bit hashes, so that's what makes sense here
		final int derivedKeyLength = 160;
		/* The NIST recommends at least 1,000 iterations:
		 * http://csrc.nist.gov/publications/nistpubs/800-132/nist-sp800-132.pdf
		 * iOS 4.x reportedly uses 10,000:
		 * http://blog.crackpassword.com/2010/09/smartphone-forensics-cracking-blackberry-backup-passwords/
		 */
		final int iterations = 20000;

		final KeySpec spec = new PBEKeySpec(password, salt,
				iterations, derivedKeyLength);
		try {
			return keyfac.generateSecret(spec).getEncoded();
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException("This should never happen", e);
		}
	}

	public byte[] generateSalt() {
		// Generate a 8 byte (64 bit) salt as recommended by RSA PKCS5
		final byte[] salt = new byte[8];
		random.nextBytes(salt);
		return salt;
	}
	
	//TODO ONMOVE remove
	public static void main(String[] args) throws Exception {
		String pwd = "foobar";
		String attpwd = "foobar";
		PasswordCrypt pc = new PasswordCrypt();
		byte[] salt = pc.generateSalt();
		byte[] encr = pc.getEncryptedPassword(pwd.toCharArray(), salt);
		boolean auth = pc.authenticate(attpwd.toCharArray(), encr, salt);
		System.out.println(toHex(salt));
		System.out.println(toHex(encr));
		System.out.println(auth);
	}
	
	public static String toHex(final byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
}
