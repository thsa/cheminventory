package org.openmolecules.inventory;

import com.actelion.research.chem.CanonizerUtil;

import java.util.*;

public class Authorizer {
	private static Authorizer sInstance;

	private final TreeMap<String,Token> mTokenMap;
	private final TreeMap<String,LoginTries> mLoginTriesMap;
	private String mAdminUser,mAdminHash;

	public static Authorizer getInstance() {
		if (sInstance == null)
			sInstance = new Authorizer();

		return sInstance;
	}

	public void initialize(Properties config) {
		String user = config.getProperty(ConfigurationKeys.ADMIN_USER, "");
		String hash = config.getProperty(ConfigurationKeys.ADMIN_HASH, "");
		if (!user.isEmpty() && !hash.isEmpty()) {
			mAdminUser = user;
			mAdminHash = hash;
		}
	}

	public static String getPasswordHash(String password) {
		return (password == null || password.isEmpty()) ? "" : Long.toHexString(CanonizerUtil.StrongHasher.hash(password));
	}

	private Authorizer() {
		mTokenMap = new TreeMap<>();
		mLoginTriesMap = new TreeMap<>();

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				ArrayList<String> keysToRemove = new ArrayList<>();
				for (String key:mTokenMap.keySet())
					if (!mTokenMap.get(key).isValid(now))
						keysToRemove.add(key);
				for (String key:keysToRemove)
					mTokenMap.remove(key);

				keysToRemove.clear();
				for (String key:mLoginTriesMap.keySet())
					if (mLoginTriesMap.get(key).isOutdated(now))
						keysToRemove.add(key);
				for (String key:keysToRemove)
					mLoginTriesMap.remove(key);
			}
		}, Token.VALIDITY, Token.VALIDITY);
	}

	public boolean isBlocked(String ip) {
		LoginTries tries = mLoginTriesMap.get(ip);
		if (tries == null) {
			mLoginTriesMap.put(ip, new LoginTries());
			return false;
		}
		return !tries.isAccepted();
	}

	public String createToken(String user, String password) {
		if (mAdminUser != null && mAdminHash != null && mAdminUser.equals(user) && mAdminHash.equals(getPasswordHash(password))) {
			Token token = new Token(Token.ADMIN);
			mTokenMap.put(token.key, token);
			return token.key;
		}
		if (DatabaseConnector.isAuthorized(user, password)) {
			Token token = new Token();
			mTokenMap.put(token.key, token);
			return token.toString();
		}
		return null;
	}

	public boolean isValidToken(String key) {
		Token token = mTokenMap.get(key);
		return token != null && token.isValid(System.currentTimeMillis());
	}

	private static class Token	{
		private final String[] ACCESS = { "read", "write", "admin" };
		protected static final int READ = 0;
		protected static final int WRITE = 1;
		protected static final int ADMIN = 2;
		private static final long VALIDITY = 3600000;   // one hour
		private static final long GRACE_PERIOD = 300000;   // 5 minutes

		String key;
		int access;
		long millis;

		public Token() {
			this(WRITE);
		}

		public Token(int type) {
			access = type;
			millis = System.currentTimeMillis();
			key = UUID.randomUUID().toString();
		}

		public boolean isValid(long now) {
			return millis + VALIDITY + GRACE_PERIOD > now;
		}

		@Override
		public String toString() {
			return key.concat(":").concat(ACCESS[access]).concat(":").concat(Long.toString(VALIDITY));
		}
	}

	private static class LoginTries {
		private static final long DELAY = 60000;
		private static final long MAX = 5;

		long millis;
		int count;

		public LoginTries() {
			millis = System.currentTimeMillis();
			count = 1;
		}

		public boolean isAccepted() {
			long now = System.currentTimeMillis();
			if (millis < now - DELAY) {
				millis = now;
				count = 1;
				return true;
			}

			return ++count <= MAX;
		}

		public boolean isOutdated(long now) {
			return millis < now - DELAY;
		}
	}
}
