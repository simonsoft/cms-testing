/**
 * Copyright (C) 2009-2013 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.testing.svn;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepository;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.util.Version;

import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestClient;
import se.repos.restclient.RestURL;
import se.repos.restclient.auth.RestAuthenticationClientCert;
import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.item.encoding.Base32;

public class SvnTestSetup {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public static final String[] TRY_PATHS = {
		"/home/cmsadmin/testsvn",
		"/home/cmsadmin/svn",
		"/Users/Shared/testsvn", 
		"C:/Repositories" // Collabnet "Subversion 1.8.3 + Apache Server (Windows 32-bit)" frtom http://www.collab.net/downloads/subversion is good on windows, but add SVNListParentPath to /svn at the end of httpd.conf
	};
	
	public static final String[] TRY_URLS = {
		"http://localhost/svn/",
		"https://ubuntu-cheftest1.pdsvision.net/testsvn/",
		"http://localdev:8530/svn/"
		
	};
	
	private static SvnTestSetup instance = null;
	
	private String urlRoot = null;
	private File pathParent = null;
	
	private List<CmsTestRepository> testRepositories = new LinkedList<CmsTestRepository>();
	
	private SvnTestSetup() {}
	
	public static SvnTestSetup getInstance() {
		if (instance == null) instance = new SvnTestSetup();
		return instance;
	}
	
	public File getSvnParentPath() {
		if (pathParent == null) {
			pathParent = trySvnParentPaths();
		}
		return pathParent;
	}
	
	File trySvnParentPaths() {
		StringBuffer tried = new StringBuffer();
		for (String p : TRY_PATHS) {
			tried.append(", ").append(p);
			File f = new File(p);
			if (f.exists() && f.isDirectory() && f.canWrite()) {
				return f;
			}
		}
		throw new RuntimeException("Svn test setup failed to find a suitable parent path among: " + tried.substring(2));
	}
	
	/**
	 * @return repository parent URL, corresponding to {@link #getSvnParentPath()}, with trailing slash (just append repository name)
	 */
	public String getSvnHttpParentUrl() {
		if (urlRoot == null) {
			urlRoot = trySvnHttpParentUrls();	
		}
		return urlRoot;
	}
	
	String trySvnHttpParentUrls() {
		StringBuffer tried = new StringBuffer();
		for (String u : TRY_URLS) {
			tried.append(", ").append(u);
			if (isHttpUrlSvnParent(u)) return u;
		}
		throw new RuntimeException("Svn test setup failed because none of these URLS were found to be an svn parent path: " + tried.substring(2));
	}
	
	public String getSvnHttpUsername(String repositoryRootUrl) {
		return "testuser";
	}
	
	public String getSvnHttpPassword(String repositoryRootUrl) {
		return "testpassword";
	}
	
	private boolean isHttpUrlSvnParent(String httpUrl) {
		
		RestURL restUrl = new RestURL(httpUrl);
		RestAuthenticationClientCert auth = null;
		try {
			auth = new RestAuthenticationClientCert(getIgnoringTrustManager(), null, "testuser", "testpassword");
		} catch (KeyManagementException e1) {
			//Should not happen, so won't handle the error.
			e1.printStackTrace();
		}
		RestClient restClientJavaNet = new RestClientJavaNet(restUrl.r(), auth);
		ResponseHeaders head;
		
		try {
			
			head = restClientJavaNet.head(restUrl.p());
		} catch (java.net.ConnectException ec) {
			logger.debug("Rejecting URL", restUrl, "due to connection error:", ec.toString());
			return false;
		} catch (IOException e) {
			throw new RuntimeException("Svn test setup failed", e);
		}
		if (head.getStatus() != 200 && head.getStatus() != 401) {
			logger.debug("Rejecting URL", restUrl, "due to status", head.getStatus());
			return false;
		}
		// TODO check for "Colleciton of repositories"
		logger.debug("URL", restUrl, "ok with content type", head.getContentType());
		return true;
	}
	/**
	 * @return Mocked TrustManager that ignores cert's.
	 */
	
	private TrustManager getIgnoringTrustManager() {
		return new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {	return null; }
			
			public void checkClientTrusted(X509Certificate[] certs, String authType) {}
			
			public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		};
	}
	
	/**
	 * Creates a new repository and adds content from a dumpfile.
	 * Call {@link #tearDown()} after test.
	 * @param dumpfile from svnadmin dump
	 * @return repository with the dupfile loaded
	 */
	public CmsTestRepository getRepository() {
		String tempName = getTestName();
		return getRepository(tempName, false);
	}

	private String getTestName() {
		return "test-" + new Base32().encode(System.currentTimeMillis()) + "." + getCaller();
	}
	
	public CmsTestRepository getRepository(String name) {
		return getRepository(name, true);
	}
	
	/**
	 * @param isCmsName true if the name is important for the test due to cms functionality
	 */
	public CmsTestRepository getRepository(String name, boolean isCmsName) {
		String url = getSvnHttpParentUrl() + name;
		File dir = new File(getSvnParentPath(), name);
		// TODO might need to wait and retry if name is taken because build server may run simultaneous builds with modules using the same repository name (common for CMS tests)
		if (dir.exists()) {
			throw new IllegalArgumentException("Test repository folder " + dir.getAbsolutePath() + " already exists. Remove manually and rerun test.");
		}
		
		try {
			SVNRepositoryFactory.createLocalRepository(dir, true, false);
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		
		chmodNewRepository(dir);
		
		CmsTestRepository repo = connect(dir, url);
		repo.setRenameAtKeep(isCmsName);
		testRepositories.add(repo);
		
		return repo;
	}

	/**
	 * Set permissions so that the repository will be read-writable both by apache and test runner locally.
	 * @param dir The local repository folder
	 */
	private void chmodNewRepository(File dir) {
		File dav = new File(dir, "dav");
		dav.mkdir();
		new File(dav, "activities.d").mkdir(); // owned by wwwrun and not group writable on jenkins server
		// java.nio.file.attribute.PosixFileAttributes is Java 1.7 only
		try {
			String cmd = "chmod -R g+w " + dir.getAbsolutePath();
			//logger.debug("Running {}", cmd);
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			logger.info("Recursive chmod failed", e);
		}
		
	}

	public CmsTestRepository connect(File localRepositoryDir, String repositoryRootUrl) {
		SVNURL svnurl;
		try {
			svnurl = SVNURL.parseURIEncoded(repositoryRootUrl);
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		
		String version = Version.getMajorVersion() + "." + Version.getMinorVersion() + "." + Version.getMicroVersion();
		String revNumber = Version.getRevisionString();
		String verMsg = MessageFormatter.format("SVNKit version {}", new Object[] { version + " (r" + revNumber + ")" }).getMessage();
		logger.info(verMsg);

		
		SVNRepository svnkit;
		DAVRepositoryFactory.setup();
		try {
			svnkit = SVNRepositoryFactory.create(svnurl);
			
			// SVNKit keeps HTTPv2 disabled by default in 1.9.0. 
			if (svnkit instanceof DAVRepository && false) {
				DAVRepository dav = (DAVRepository) svnkit; 
				dav.setHttpV2Enabled(true);
				logger.warn("Enabled HttpV2 support in DAVRepository instance.");
			}
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		String svnHttpUsername = getSvnHttpUsername(repositoryRootUrl);
		String svnHttpPassword = getSvnHttpPassword(repositoryRootUrl);
		svnkit.setAuthenticationManager(new BasicAuthenticationManager(svnHttpUsername, svnHttpPassword));
		
		CmsTestRepository repo = new CmsTestRepository(svnkit, localRepositoryDir, svnHttpUsername, svnHttpPassword);
		return repo;
	}
	
	/**
	 * Always call this after tests, clears temporary files from local file system.
	 */
	public void tearDown() {
		for (CmsTestRepository r : testRepositories) {
			if (r.isKeep()) {
				if (r.isRenameAtKeep()) {
					String name = r.getName() + "-" + getTestName();
					File dest = new File(r.getAdminPath().getParentFile(), name);
					r.getAdminPath().renameTo(dest);
					System.out.println("Test repoistory " + r.getName() + " kept at:"
							+ "\n" + dest.getAbsolutePath());
				} else {
					System.out.println("Test repository " + r.getName() + " kept at:"
							+ "\n file://" + r.getAdminPath().getAbsolutePath()
							+ "\n " + r.getUrl());
				}
			} else {
				try {
					FileUtils.deleteDirectory(r.getAdminPath());
				} catch (IOException e) {
					throw new RuntimeException("Error not handled", e);
				}
			}
		}
		testRepositories.clear();
	}
	
	private String getCaller() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (int i = 1; i < stackTrace.length; i++) {
			if (!this.getClass().getName().equals(stackTrace[i].getClassName())) {
				return stackTrace[i].getClassName();// + "." + stackTrace[i].getMethodName();
			}
		}
		return stackTrace[0].getClassName();
	}
	
}
