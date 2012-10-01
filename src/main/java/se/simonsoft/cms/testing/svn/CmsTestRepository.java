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
import java.io.InputStream;

import javax.inject.Provider;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

import se.simonsoft.cms.item.CmsRepository;

public class CmsTestRepository extends CmsRepository {

	private SVNRepository svnkit;
	private File repoFolder;
	private String user;
	private String password;
	
	private boolean keep = false;

	public CmsTestRepository(SVNRepository svnkit, File repoFolder, String user, String password) {
		super(getUrl(svnkit));
		this.svnkit = svnkit;
		this.repoFolder = repoFolder;
		this.user = user;
		this.password = password;
	}
	
	/**
	 * @param dumpfile from svnadmin dump
	 * @return the instance
	 */
	public CmsTestRepository load(InputStream dumpfile) {
		SVNAdminClient svnadmin = new SVNAdminClient(SVNWCUtil.createDefaultAuthenticationManager(), null);
		try {
			svnadmin.doLoad(repoFolder, dumpfile);
		} catch (SVNException e) {
			throw new RuntimeException("Error not handled", e);
		}
		return this;
	}
	
	/**
	 * Flags to test setup that the repository should be kept after tearDown, for manual investigation.
	 */
	public void keep() {
		this.keep = true;
	}
	
	public boolean isKeep() {
		return keep;
	}

	/**
	 * @param keep see {@link #keep()}
	 */
	public void setKeep(boolean keep) {
		this.keep = keep;
	}
	
	/**
	 * @return repository connection, authenticated
	 */
	public SVNRepository getSvnkit() {
		return svnkit;
	}
	
	/**
	 * @return access to the new relatively maintainer friendly wc2 operations api
	 */
	public SvnOperationFactory getSvnkitOp() {
		SvnOperationFactory op = new SvnOperationFactory();
		op.setAuthenticationManager(getSvnkit().getAuthenticationManager());
		return op;
	}
	
	/**
	 * @return for svnkit based impls that get repository provider injected
	 */
	public Provider<SVNRepository> getSvnkitProvider() {
		final CmsTestRepository repo = this;
		return new Provider<SVNRepository>() {
			@Override
			public SVNRepository get() {
				return repo.getSvnkit();
			}
		};
	}

	public File getLocalFolder() {
		return repoFolder;
	}

	public String getAuthenticatedUser() {
		return user;
	}

	public String getAuthenticatedPassword() {
		return password;
	}
	
	static String getUrl(SVNRepository repository) {
		try {
			SVNDirEntry info = repository.info("/", SVNRevision.HEAD.getNumber());
			return info.getURL().toString();
		} catch (SVNException e) {
			throw new RuntimeException("Failed to verify connection to test repository", e);
		}
	}

}
