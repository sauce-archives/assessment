package org.testobject.persistence.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface FileMapper {
	
	interface Writer {
		void write(OutputStream out) throws IOException;
	}
	
	void createNamespace(String namespace);
	
	void deleteNamespace(String namespace);
	
	void write(String namespace, String[] path, Writer writer);

	InputStream read(String namespace, String[] path);

	void delete(String namespace, String[] path);

}
