package org.testobject.persistence.file.local;

import static org.testobject.commons.file.Constants.application_work_folder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.testobject.commons.file.Constants;
import org.testobject.commons.file.FileUtil;
import org.testobject.persistence.file.FileMapper;

public class LocalFileSystemMapper implements FileMapper {

	private final String prefix;
	private final File workingFolder; 

	@Inject
	public LocalFileSystemMapper(@Named(Constants.system_name) String prefix, @Named(application_work_folder) String workFolderPath) {
		this.prefix = prefix;
		this.workingFolder = new File(workFolderPath);
	}

	@Override
	public void createNamespace(String namespace) {
		new File(workingFolder, getBucketName(namespace)).mkdirs();
	}
	

	@Override
	public void deleteNamespace(String namespace) {
		FileUtil.removeDir(new File(workingFolder, getBucketName(namespace)));
	}

	@Override
	public InputStream read(String namespace, String[] path) {
		try {
			return new FileInputStream(getFile(getBucketName(namespace), toString(path)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(String namespace, String[] path, Writer writer) {
		File file = getFile(getBucketName(namespace), toString(path));
		if (file.exists() == false) {
			createFile(file);
		}
		
		try (FileOutputStream out = new FileOutputStream(file, false)) {
			writer.write(out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void createFile(File file) {
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String toString(String[] path) {
		StringBuilder b = new StringBuilder();
		for (String string : path) {
			b.append(File.separator).append(string);
		}
		return b.toString();
	}

	@Override
	public void delete(String namespace, String[] path) {
		getFile(getBucketName(namespace), toString(path)).delete();
	}

	private File getFile(String namespace, String key) {
		return new File(workingFolder, namespace + File.separator + key);
	}

	private String getBucketName(String entityName) {
		return prefix + "-" + entityName;
	}
}
