package se.c0la.fatcat.context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Operator
{
	private String username;
	private String password;
	private final static byte[] salt = "535ert3532r234dr3".getBytes();

	public Operator(String username, String password)
	throws NoSuchAlgorithmException
	{
		this.username = username;
		this.password = password;
	}

	public String getUser() { return this.username; }

	public boolean tryPassword(String v)
	throws NoSuchAlgorithmException
	{
		String challenge = getHash(v);
		return password.equals(challenge);
	}

	public static String getHash(String password)
	throws NoSuchAlgorithmException
	{
		byte[] bytes;
		String hex;
		StringBuffer sb = new StringBuffer();
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		
		digest.reset();
		digest.update(salt);
		bytes = digest.digest(password.getBytes());
		
		for(byte b : bytes) {
			hex = Integer.toHexString(b & 0xFF);
			if(hex.length() == 1)
				sb.append("0");
			sb.append(hex);
		}
		
		return sb.toString();
	}
}

