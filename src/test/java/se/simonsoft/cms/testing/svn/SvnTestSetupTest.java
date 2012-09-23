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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SvnTestSetupTest {

	@After
	public void tearDown() {
		SvnTestSetup.getInstance().tearDown();
	}

	@Test
	public void testGetSvnHttpParentUrl() {
		String url = SvnTestSetup.getInstance().getSvnHttpParentUrl();
		assertNotNull(url);
		System.out.println("Got svn parent URL " + url);
	}

	@Test
	public void testGetRepository() throws SVNException {
		CmsTestRepository repo = SvnTestSetup.getInstance().getRepository();
		SVNRepository repository = repo.getSvnkit();
		SVNDirEntry info = repository.info("/", SVNRevision.HEAD.getNumber());
		assertNotNull(info);
	}
	
	@Test
	public void testGetRepositoryUrl() {
		CmsTestRepository repo = SvnTestSetup.getInstance().getRepository();
		String repoUrl = repo.getUrl();
		assertTrue("got " + repoUrl, repoUrl.startsWith(SvnTestSetup.getInstance().getSvnHttpParentUrl()));
		assertTrue("got " + repoUrl, repoUrl.length() > SvnTestSetup.getInstance().getSvnHttpParentUrl().length() + 10);
		assertTrue(!repoUrl.endsWith("/"));
	}

}
