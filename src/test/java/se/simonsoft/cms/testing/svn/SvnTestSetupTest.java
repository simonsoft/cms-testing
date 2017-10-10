/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
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

import java.io.ByteArrayInputStream;
import java.io.File;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;

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
		assertTrue(repo.getAdminPath().exists());
		assertTrue(repo.getAdminPath().canRead());
		assertTrue("should be a subversion repository", new File(repo.getAdminPath(), "format").exists());
	}
	
	@Test
	public void testLoadDumpfile() throws SVNException {
		String dump = "SVN-fs-dump-format-version: 2\n"
				+ "\n"
				+ "UUID: 9ff1b372-1b0e-41ec-946b-24d40082c707\n"
				+ "\n"
				+ "Revision-number: 0\n"
				+ "Prop-content-length: 73\n"
				+ "Content-length: 73\n"
				+ "\n"
				+ "K 8\n"
				+ "svn:date\n"
				+ "V 27\n"
				+ "2012-09-25T19:07:32.517877Z\n"
				+ "K 4\n"
				+ "test\n"
				+ "V 3\n"
				+ "yes\n"
				+ "PROPS-END\n";
		CmsTestRepository repo = SvnTestSetup.getInstance().getRepository().load(new ByteArrayInputStream(dump.getBytes()));
		SvnGetProperties propget = repo.getSvnkitOp().createGetProperties();
		propget.setRevisionProperties(true);
		propget.setRevision(SVNRevision.create(0));
		propget.setSingleTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(repo.getUrl() + "/")));
		propget.setReceiver(new ISvnObjectReceiver<SVNProperties>() {
			@Override
			public void receive(SvnTarget target, SVNProperties object) throws SVNException {
				assertEquals("yes", SVNPropertyValue.getPropertyAsString(object.getSVNPropertyValue("test")));
			}
		});
		propget.run();
	}

	@Test
	public void testNamedRepository() {
		CmsTestRepository repo = SvnTestSetup.getInstance().getRepository("myNamedRepo");
		assertEquals("myNamedRepo", repo.getAdminPath().getName());
		assertTrue(repo.getUrl().endsWith("/myNamedRepo"));
		try {
			SvnTestSetup.getInstance().getRepository("myNamedRepo");
			fail("Should refuse to create a repository with same name as existing");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	@Test
	@Ignore // leaves a test repo every time
	public void testNamedRepositoryKeep() {
		CmsTestRepository repo = SvnTestSetup.getInstance().getRepository("testaut1");
		repo.keep();
		assertEquals("testaut1", repo.getAdminPath().getName());
		assertTrue(repo.getUrl().endsWith("/testaut1"));
		assertTrue(repo.getAdminPath().exists());
		tearDown();
		assertFalse("should have renamed to a unique name at keep so next text can use the name", repo.getAdminPath().exists());
	}
	
}
