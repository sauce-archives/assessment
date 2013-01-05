S3 Throughput Issue
===================

If you need any assistance, don't hesitate to drop me a mail at erik.nijkamp@testobject.com ;)

Description
-----------

TestObject is a cloud-based service, therefore our data (that is test scripts, reports, test apps and so forth) sits in the cloud.
More specifically, most of our binary data resides in Amazons distributed file system S3 and most of our textual data is stored in Amazons distributed DynamoDB.

Over the past few weeks, we've observed that uploading a large number of relatively small files (~1 kb per file) to S3 takes a tremendous amount of time.
In numbers, it takes roughly 10 seconds to upload 24 files which in total eat up 47 kilobytes ... that is 4 kilobytes / second ... really slow indeed!

Your mission, should you choose to accept it, is to significantly improve the S3 throughput for large numbers of small files. 


Prerequisites
-----------

This is our minimal chain of tools:

- Ubuntu 12.04 LTS
- OpenJDK 1.7
- Eclipse Juno for Java Developers [0]
- Amazon AWS account, instructions are below, if this is an issue (e.g. for privacy reasons), please ping us

Feel free to use tools of your choice, but keep in mind that we cannot provide any assistance for tools other than the listed above.

Setup
----

1. Get a local copy of the 'testobject/assessment' git repository [1] either using git clone or githubs zip file
2. Import the maven project 's3.throughput.issue' into your local eclipse workspace [2]
3. Sign up for an Amazon AWS S3 account [3] - YOU DON'T HAVE TO PAY for S3, the free-tier comes with plenty of free requests
4. Log into your AWS account, goto [4] and create a new access key
5. Copy and paste the 'Access Key ID' and 'Secret Access Key' into org.testobject.persistence.dao.ImageDaoTest.Constants
6. Run the unit-test org.testobject.persistence.dao.ImageDaoTest, both saveLocal() and saveRemote() should pass
7. Double-check that the image files are uploaded to S3 [5]
 

Tasks
----

1. Analyze the code and structure sitting in 's3.throughput.issue'
2. Find a way to significantly increase the throughput (by an order of magnitude or so) of remote S3 uploads, that is ImageDaoTest.saveRemote()
3. Implement your proposal ... feel free to improve the overall code or whatever you've got in mind
4. Write down your findings, improvements, suggestions (only a few lines)
5. Drop us a mail containing your findings and source code at erik.nijkamp@testobject.com
6. Cancel your AWS account [6]

Hints
----

- Look into [7], [8] to get a general understanding

- ImageDao.put(...) writes the array of images sequentially, this can be done in parallel, see [9] or java.lang.Thread

- S3FileSystemMapper.upload(...) utilizes http multi-part uploads, for small files (say less than 1 mb) multi-part uploads are not required, see AmazonS3Client.putObject(...) to upload a file in a single request

- Take a look at the actual image data ... there are other more creative approaches to approach this!

- Feel free to suggest alternative solutions, refactor the code, or try whatever you like ... and keep in mind that Google can give you a hand


References
-----

[0] http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/junosr1

[1] https://github.com/testobject/assessment

[2] http://www.sonatype.com/books/m2eclipse-book/reference/creating-sect-importing-projects.html

[3] https://aws.amazon.com/s3/?tag=bucket-20

[4] https://portal.aws.amazon.com/gp/aws/securityCredentials

[5] https://console.aws.amazon.com/s3/home?region=us-east-1

[6] https://portal.aws.amazon.com/gp/aws/manageYourAccount?ie=UTF8&action=cancel

[7] http://improve.dk/archive/2011/11/07/pushing-the-limits-of-amazon-s3-upload-performance.aspx

[8] http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/TransferManager.html

[9] http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService.html
