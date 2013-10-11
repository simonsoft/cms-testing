package se.simonsoft.cms.testing.svn;

import java.io.File;

import se.simonsoft.cms.item.CmsRepository;

class CmsRepositoryLocal extends CmsRepository {

	private static final long serialVersionUID = 1L;

	private File adminPath;

	CmsRepositoryLocal(String repositoryUrl, File adminPath) {
		super(repositoryUrl);
		this.adminPath = adminPath;
	}
	
	public File getAdminPath() {
		return adminPath;
	}
	
}
