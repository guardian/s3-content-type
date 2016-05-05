package S3ContentType

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._

import collection.JavaConverters._

object scala extends App {
  implicit val s3Client: AmazonS3Client = new AmazonS3Client().withRegion(Regions.EU_WEST_1)

  def listObjects(bucketName: String, prefix: Option[String] = None)(implicit client: AmazonS3Client): Iterator[S3ObjectSummary] = new Iterator[Seq[S3ObjectSummary]] {
    val request = prefix.foldLeft(new ListObjectsV2Request().withBucketName(bucketName)){_.withPrefix(_)}

    var lastResponse: Option[ListObjectsV2Result] = Some(client.listObjectsV2(request))

    def hasNext: Boolean = lastResponse.isDefined

    def next(): Seq[S3ObjectSummary] = {
      val objects = lastResponse.get.getObjectSummaries.asScala
      lastResponse = lastResponse.flatMap { response =>
        if (response.isTruncated) {
          Some(client.listObjectsV2(request.withContinuationToken(response.getNextContinuationToken)))
        } else None
      }
      objects
    }
  }.flatMap(summaries => summaries)

  def updateContentType(objectSummary: S3ObjectSummary, contentType: String): Unit = {
    val bucketName = objectSummary.getBucketName
    val key = objectSummary.getKey
    val metadata = s3Client.getObjectMetadata(bucketName, key)
    if (metadata.getContentType != contentType) {
      val newMetadata = metadata.clone()
      newMetadata.setContentType(contentType)
      s3Client.copyObject(new CopyObjectRequest(bucketName, key, bucketName, key).withNewObjectMetadata(newMetadata))
      println(s"Updated: $key (Content-Type was ${metadata.getContentType})")
    } else {
      println(s"Key: $key (Content-Type is ${metadata.getContentType})")
    }
  }

  val objects = listObjects("static-origin.guim.co.uk", Some("ni/"))
  val svgObjects = objects.filter(_.getKey.toLowerCase.endsWith(".svg"))
  svgObjects.foreach(updateContentType(_, "image/svg+xml"))
}
