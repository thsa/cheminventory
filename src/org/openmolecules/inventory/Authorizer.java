package org.openmolecules.inventory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;

public class Authorizer {
	private static Authorizer sInstance;

	private TreeMap<String,Token> mTokenMap;
	private TreeMap<String,LoginTries> mLoginTriesMap;

	public static Authorizer getInstance() {
		if (sInstance == null)
			sInstance = new Authorizer();

		return sInstance;
	}

	private Authorizer() {
		mTokenMap = new TreeMap<>();
		mLoginTriesMap = new TreeMap<>();

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				for (String key:mTokenMap.keySet())
					if (!mTokenMap.get(key).isValid(now))
						mTokenMap.remove(key);
				for (String key:mLoginTriesMap.keySet())
					if (mLoginTriesMap.get(key).isOutdated(now))
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
		if (DatabaseConnector.isAuthorized(user, password)) {
			Token token = new Token();
			mTokenMap.put(token.key, token);
			return token.key;
		}
		return null;
	}

	public boolean isValidToken(String key) {
		Token token = mTokenMap.get(key);
		return token != null && token.isValid(System.currentTimeMillis());
	}

	private class Token	{
		private static final long VALIDITY = 3600000;   // one hour

		String key;
		long millis;

		public Token() {
			millis = System.currentTimeMillis();
			key = UUID.randomUUID().toString();
		}

		public boolean isValid(long now) {
			return millis > now - VALIDITY;
		}
	}

	private class LoginTries {
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
