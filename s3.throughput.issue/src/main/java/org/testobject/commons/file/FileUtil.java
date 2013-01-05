package org.testobject.commons.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileUtil {

	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;


	public static void move(File from, File to) throws IOException {
		copyFile(from, to);
		from.delete();
	}

	public static void replace(String oldstring, String newstring, File in, File out) throws IOException {

		if(in.getAbsolutePath().equals(out.getAbsolutePath())) {
			throw new IllegalArgumentException();
		}

	    try(BufferedReader reader = new BufferedReader(new FileReader(in)); PrintWriter writer = new PrintWriter(new FileWriter(out))) {
		    String line = null;
		    while ((line = reader.readLine()) != null) {
		        writer.println(line.replaceAll(oldstring, newstring));
		    }
	    }
	}

	public static long transfer(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static void copyFolder(File src, File dst, FilenameFilter filter) throws IOException {
		if (src.isDirectory()) {
			if (!dst.exists()) {
				dst.mkdir();
			}

			String files[] = src.list(filter);

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dst, file);
				copyFolder(srcFile, destFile);
			}

		} else {
			copyFile(src, dst);
		}
	}

	public static void copyFolder(File src, File dst) throws IOException {
		if (src.isDirectory()) {
			if (!dst.exists()) {
				dst.mkdir();
			}

			String files[] = src.list();

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dst, file);
				copyFolder(srcFile, destFile);
			}

		} else {
			copyFile(src, dst);
		}
	}

	public static void copyFile(File src, File dst) throws IOException {
		if (!dst.exists()) {
			dst.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(src).getChannel();
			destination = new FileOutputStream(dst).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	public static boolean removeDir(File directory) {

		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();

		// Some JVMs return null for File.list() when the directory is empty.
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				File entry = new File(directory, list[i]);
				if (entry.isDirectory()) {
					if (!removeDir(entry))
						return false;
				} else {
					if (!entry.delete())
						return false;
				}
			}
		}

		return directory.delete();
	}

	public static File toFile(URL url) {
		try {
			return new File(url.toURI());
		} catch (URISyntaxException e) {
			return new File(url.getPath());
		}
	}

	public static File toFileFromSystem(String file) {
		URL url = ClassLoader.getSystemResource(file);
		if (url == null) {
			throw new IllegalArgumentException("Cannot open file " + file);
		}
		return toFile(url).getAbsoluteFile();
	}

	public static File toFileFromClassloader(Class<?> clazz, String file) {
		URL url = clazz.getResource(file);
		if (url == null) {
			throw new IllegalArgumentException("Cannot open file " + file);
		}
		return toFile(url).getAbsoluteFile();
	}

	public static File toFileFromThread(String file) {
		URL url = Thread.currentThread().getContextClassLoader().getResource(file);
		if (url == null) {
			throw new IllegalArgumentException("Cannot open file " + file);
		}
		return toFile(url).getAbsoluteFile();
	}

	public static InputStream readFileFromSystem(String file) {
		return ClassLoader.getSystemResourceAsStream(file);
	}

	public static InputStream readFileFromSystem(File file) {
		return ClassLoader.getSystemResourceAsStream(file.getPath());
	}

	public static InputStream readFileFromClassPath(String file) {
		return FileUtil.class.getClassLoader().getResourceAsStream(file);
	}

	public static InputStream readFileFromClassPath(File file) {
		return FileUtil.class.getClassLoader().getResourceAsStream(file.getPath());
	}

	public static File getJarForClass(Class<?> clazz) {
		return new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
	}

	public static File extractFromJar(File jarFile, String target, String destDir) throws IOException {
		if (jarFile.getName().endsWith(".jar") == false) {
			throw new IllegalArgumentException("jar file must end with '.jar': " + jarFile.getPath());
		}
		File destinationFile = new File(destDir, jarFile.getName());
		try (JarFile jar = new java.util.jar.JarFile(jarFile)) {
			Enumeration<JarEntry> enumeration = jar.entries();
			while (enumeration.hasMoreElements()) {
				JarEntry file = enumeration.nextElement();
				if (file.getName().startsWith(target) == false) {
					continue;
				}
				File f = new File(destinationFile, file.getName());
				if (file.isDirectory()) {
					f.mkdirs();
					continue;
				}
				try (InputStream is = jar.getInputStream(file); FileOutputStream fos = new FileOutputStream(f);) {
					while (is.available() > 0) {
						fos.write(is.read());
					}
				}
			}
		}

		return new File(destinationFile, target);
	}

	public static void extractFromClasspath(String from, String to) throws IOException {

		InputStream src = readFileFromClassPath(from);
		OutputStream dst = new FileOutputStream(new File(to));

		transfer(src, dst);
	}
}