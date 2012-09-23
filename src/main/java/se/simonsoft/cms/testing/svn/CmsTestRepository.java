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

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;

import se.simonsoft.cms.item.CmsRepository;

public class CmsTestRepository extends CmsRepository {

	private SVNRepository svnkit;
	private File repoFolder;
	private String user;
	private String password;

	public CmsTestRepository(SVNRepository svnkit, File repoFolder, String user, String password) {
		super(getUrl(svnkit));
		this.svnkit = svnkit;
		this.repoFolder = repoFolder;
		this.user = user;
		this.password = password;
	}
	
	/**
	 * @return repository connection, authenticated
	 */
	public SVNRepository getSvnkit() {
		return svnkit;
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
