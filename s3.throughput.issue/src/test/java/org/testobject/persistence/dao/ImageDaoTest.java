package org.testobject.persistence.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.application_work_folder;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.aws_client_access_key;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.aws_client_secret_key;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.aws_prefix;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.aws_region;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.path;
import static org.testobject.persistence.dao.ImageDaoTest.Constants.system_name;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.testobject.commons.file.FileUtil;
import org.testobject.persistence.file.FileMapper;
import org.testobject.persistence.file.aws.S3FileSystemMapper;
import org.testobject.persistence.file.local.LocalFileSystemMapper;

import com.google.common.base.Preconditions;

public class ImageDaoTest {
	
	private static final Log log = LogFactory.getLog(ImageDaoTest.class);
	
	interface Constants {
		
		// ### TODO paste your AWS credentials here ###
		String aws_client_access_key = ">>> access key goes here <<<";
		String aws_client_secret_key = ">>> secret key goes here <<<";
		
		String aws_region = "EU";
		String aws_prefix = "unit-test";
		
		// ### TODO change the system name to some unique identifier ###
		String system_name = "foobar";
		String application_work_folder = ".";
		
		String path = "images";
	}
	
	@Before
	public void before() {
		Preconditions.checkState(">>> access key goes here <<<".equals(aws_client_access_key) == false, "aws client access key not set");
		Preconditions.checkState(">>> secret key goes here <<<".equals(aws_client_secret_key) == false, "aws client secret key not set");
	}
	
	@Test
	public void saveLocal() throws Exception {
		
		FileMapper mapper = prepare(createLocalFileMapper());
		
		save(new ImageDao(mapper));
	}
	
	@Test
	public void saveRemote() throws Exception {
		
		FileMapper mapper = prepare(createRemoteFileMapper());
		
		save(new ImageDao(mapper));
	}
	
	private void save(ImageDao dao) throws ZipException, IOException {
		
		// put
		final Data data = readData("tiny.zip");
		long begin = System.currentTimeMillis();
		{
			dao.put(path, data.images);
		}
		long end = System.currentTimeMillis();
		log.info("Writing " + data.images.length + " files (" + data.bytes + " bytes) took " + (end - begin) + " ms");
		
		
		// get
		BufferedImage image = dao.get(path, 0);
		assertThat(image.getWidth(), is(data.images[0].getWidth()));
		assertThat(image.getHeight(), is(data.images[0].getHeight()));
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				assertThat(image.getRGB(x, y), is(data.images[0].getRGB(x, y)));
			}
		}
		
	}

	static FileMapper prepare(FileMapper mapper) throws InterruptedException {
		mapper.deleteNamespace(ImageDao.NAMESPACE);
		mapper.createNamespace(ImageDao.NAMESPACE);
		
		return mapper;
	}

	static FileMapper createLocalFileMapper() {
		return new LocalFileSystemMapper(system_name, application_work_folder);
	}

	static FileMapper createRemoteFileMapper() {
		return new S3FileSystemMapper(application_work_folder, aws_prefix, aws_region,
				aws_client_access_key, aws_client_secret_key);
	}
	
	static class Data {
		public final BufferedImage[] images;
		public final long bytes;
		
		public Data(BufferedImage[] images, long bytes) {
			this.images = images;
			this.bytes = bytes;
		}
	}
	
	static Data readData(String file) throws ZipException, IOException {
		try(ZipFile zip = new ZipFile(FileUtil.toFileFromSystem(file))) {
			
			Enumeration<? extends ZipEntry> entries = zip.entries();
			
			List<BufferedImage> images = new ArrayList<>(zip.size());
			long bytes = 0;
			
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				bytes += entry.getSize();
				InputStream inputStream = zip.getInputStream(entry);
				images.add(ImageIO.read(inputStream));
			}
			
			return new Data(images.toArray(new BufferedImage[] {}), bytes);
		}
	}
}
