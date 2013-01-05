package org.testobject.persistence.file.aws;

import static org.testobject.commons.file.Constants.dynamodb_client_access_key;
import static org.testobject.commons.file.Constants.dynamodb_client_secret_key;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.testobject.commons.file.Constants;
import org.testobject.persistence.file.FileMapper;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.google.common.base.Preconditions;

public class S3FileSystemMapper implements FileMapper {
	
	private final String prefix;
	private final AmazonS3Client client;

	private final Region region;
    private final File workFolder;

    @Inject
	public S3FileSystemMapper(@Named(Constants.application_work_folder) String workFolder, @Named(Constants.system_name) String prefix, @Named(Constants.s3_region) String region, @Named(dynamodb_client_access_key) String accessKey, @Named(dynamodb_client_secret_key) String secretKey) {
		this.prefix = prefix;
		this.region = Region.fromValue(region);
		this.client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        this.workFolder = new File(workFolder, "org.testobject.runtime.persistence.tmp");
	}

	@Override
	public void createNamespace(String namespace) {
		try {
			client.getBucketLocation(getBucketName(namespace));
            new File(workFolder, namespace).mkdirs();
		} catch(AmazonServiceException e) {
			client.createBucket(getBucketName(namespace), region);
		}
	}

	@Override
	public void deleteNamespace(String namespace) {
		String bucket = getBucketName(namespace);
		if(client.doesBucketExist(bucket)) {
			ObjectListing listObjects = client.listObjects(bucket);
			for(S3ObjectSummary summary : listObjects.getObjectSummaries()) {
				client.deleteObject(bucket, summary.getKey());
			}
			client.deleteBucket(bucket);
		}
	}
	
	@Override
	public InputStream read(String namespace, String[] path) {
		String key = toKey(path);
		
		S3Object s3Object = client.getObject(new GetObjectRequest(getBucketName(namespace), key));
		Preconditions.checkNotNull(s3Object, "given key '" + key + "' doesn't exist in bucket '" + getBucketName(namespace) + "'");

		return s3Object.getObjectContent();
	}
	
	@Override
	public void write(String namespace, String[] path, Writer writer) {
		
		String key = toKey(path);
		
        UUID tmp = UUID.randomUUID();
        File tmpFolder = new File(workFolder, namespace);
        File tmpFile = new File(tmpFolder, tmp.toString());

        try {
            tmpFile.createNewFile();
            writer.write(new FileOutputStream(tmpFile));

            upload(namespace, key, tmpFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(tmpFile.exists()){
                tmpFile.delete();
            }
        }
	}

	@Override
	public void delete(String namespace, String[] path) {
		client.deleteObject(namespace, toKey(path));
	}

    private void upload(String existingBucketName, String keyName, File file) {
    	
    	// ### TODO do not use multi-part upload for files smaller than 1 mb? ###
    	
        // Create a list of UploadPartResponse objects. You get one of these for each part upload.
        List<PartETag> partETags = new ArrayList<>();

        // Step 1: Initialize.
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(getBucketName(existingBucketName), keyName);
        InitiateMultipartUploadResult initResponse = client.initiateMultipartUpload(initRequest);

        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {
            // Step 2: Upload parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Last part can be less than 5 MB. Adjust part size.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(getBucketName(existingBucketName)).withKey(keyName)
                        .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);

                // Upload part and add response to our list.
                partETags.add(client.uploadPart(uploadRequest).getPartETag());

                filePosition += partSize;
            }

            // Step 3: complete.
            CompleteMultipartUploadRequest compRequest = new
                    CompleteMultipartUploadRequest(getBucketName(existingBucketName),
                    keyName,
                    initResponse.getUploadId(),
                    partETags);

            client.completeMultipartUpload(compRequest);
        } catch (Exception e) {
            client.abortMultipartUpload(new AbortMultipartUploadRequest(
                    getBucketName(existingBucketName), keyName, initResponse.getUploadId()));

            throw new RuntimeException(e);
        }
    }
	
	private String getBucketName(String entityName){
		return prefix + "-" + entityName;
	}
	
	private static String toKey(String ... path) {
		StringBuilder sb = new StringBuilder();
		for (String string : path) {
			sb.append("/").append(string);
		}
		return sb.toString();
	}
}
