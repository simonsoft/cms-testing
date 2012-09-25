/**
 * Copyright (C) 2009-2012 Simonsoft Nordic AB
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
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import se.repos.lgr.Lgr;
import se.repos.lgr.LgrFactory;
import se.repos.restclient.ResponseHeaders;
import se.repos.restclient.RestClient;
import se.repos.restclient.RestURL;
import se.repos.restclient.javase.RestClientJavaNet;
import se.simonsoft.cms.item.encoding.Base32;

public class SvnTestSetup {

	private final Lgr logger = LgrFactory.getLogger();
	
	public static final String[] TRY_PATHS = {
		"/home/cmsadmin/svn"
	};
	
	public static final String[] TRY_URLS = {
		"http://localhost/svn/",
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
		return "test";
	}
	
	public String getSvnHttpPassword(String repositoryRootUrl) {
		return "test";
	}
	
	private boolean isHttpUrlSvnParent(String httpUrl) {
		RestURL restUrl = new RestURL(httpUrl);
		RestClient restClientJavaNet = new RestClientJavaNet(restUrl.r(), null);
		ResponseHeaders head;
		try {
			head = restClientJavaNet.head(restUrl.p());
		} catch (java.net.ConnectException ec) {
			logger.debug("Rejecting URL", restUrl, "due to connection error:", ec.toString());
			return false;
		} catch (IOException e) {
			throw new RuntimeException("Svn test setup failed", e);
		}
		if (head.getStatus() != 200) {
			logger.debug("Rejecting URL", restUrl, "due to status", head.getStatus());
			return false;
		}
		// TODO check for "Colleciton of repositories"
		logger.debug("URL", restUrl, "ok with content type", head.getContentType());
		return true;
	}
	
	/**
	 * Creates a new repository.
	 * Call {@link #tearDown()} after test.
	 * @return svn URL, http ideally
	 */
	public CmsTestRepository getRepository() {
		return getRepository(null);
	}
	
	/**
	 * Creates a new repository and adds content from a dumpfile.
	 * @param dumpfile from svnadmin dump
	 * @return repository with the dupfile loaded
	 */
	public CmsTestRepository getRepository(InputStream dumpfile) {
		String tempName = new Base32().encode(System.currentTimeMillis()) + "." + Thread.currentThread().getStackTrace()[1].getClassName();
		String url = getSvnHttpParentUrl() + tempName;
		File dir = new File(getSvnParentPath(), tempName);
		SVNURL svnurl;
		try {
			svnurl = SVNURL.parseURIEncoded(url);
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		
		try {
			SVNRepositoryFactory.createLocalRepository(dir, true, false);
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		
		SVNRepository svnkit;
		DAVRepositoryFactory.setup();
		try {
			svnkit = SVNRepositoryFactory.create(svnurl);
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		String svnHttpUsername = getSvnHttpUsername(url);
		String svnHttpPassword = getSvnHttpPassword(url);
		svnkit.setAuthenticationManager(new BasicAuthenticationManager(svnHttpUsername, svnHttpPassword));
		
		CmsTestRepository repo = new CmsTestRepository(svnkit, dir, svnHttpUsername, svnHttpPassword);
		testRepositories.add(repo);
		
		if (dumpfile != null) {
			SVNAdminClient svnadmin = new SVNAdminClient(SVNWCUtil.createDefaultAuthenticationManager(), null);
			try {
				svnadmin.doLoad(dir, dumpfile);
			} catch (SVNException e) {
				throw new RuntimeException("Error not handled", e);
			}
		}
		
		return repo;
	}
	
	/**
	 * Always call this after tests, clears temporary files from local file system.
	 */
	public void tearDown() {
		for (CmsTestRepository r : testRepositories) {
			if (r.isKeep()) {
				System.out.println("Test repository " + r.getName() + " kept at:"
						+ "\n file://" + r.getLocalFolder().getAbsolutePath()
						+ "\n " + r.getUrl());
			} else {
				try {
					FileUtils.deleteDirectory(r.getLocalFolder());
				} catch (IOException e) {
					throw new RuntimeException("Error not handled", e);
				}
			}
		}
		testRepositories.clear();
	}
	
}
