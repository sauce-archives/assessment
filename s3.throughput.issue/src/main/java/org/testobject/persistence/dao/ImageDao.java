package org.testobject.persistence.dao;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.testobject.persistence.file.FileMapper;

public class ImageDao {
	
	public static final String NAMESPACE = "org.foo.bar.images";
	
	private final FileMapper fileMapper;
	
	public ImageDao(FileMapper fileMapper) {
		this.fileMapper = fileMapper;
		
		fileMapper.createNamespace(NAMESPACE);
	}

	public void put(String path, final BufferedImage ... images) {
		
		// ### TODO parallelize sequential upload using multiple threads? ###
		
		for(int i = 0; i < images.length; i++) {
			final BufferedImage image = images[i];
			fileMapper.write(NAMESPACE, new String[] { path, Integer.toString(i) + ".png" }, new FileMapper.Writer() {
				@Override
				public void write(OutputStream out) throws IOException {
					ImageIO.write(image, "png", out);
				}
			});
		}
	}
	
	public void remove(String path, int image) {
		fileMapper.delete(NAMESPACE, new String[] { path, Integer.toString(image) + ".png" });
	}

	public BufferedImage get(String path, int image) throws IOException {
		try(InputStream input = fileMapper.read(NAMESPACE, new String[] { path, Integer.toString(image) + ".png" })) {
			return ImageIO.read(input);
		}
	}

}
