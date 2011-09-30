package vidcapminimal;

import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;

/**
 * This class will get the FLV meta data information.
 * @author SANTHOSH REDDY MANDADI
 * @version 1.0
 * @since 10th June 2009
 */
public class FLVMetaData
{
  /**
   * Represents the FLV path either a URL or a absolute file path. If it is URL
   * url property should be true to access the file from web
   */
  private String flv=null;

  /**
   * Represents whether the FLV is remote or local, if this is true
   * data will be fetched through the HTTP connection otherwise data will
   * be fetched from the local file.
   */
  private boolean url=true;

  //All the FLV properties
  private String duration;
  private double width;
  private double height;
  private double audioDataRate;
  private double videoDataRate;
  private double fileSize;
  private String createdDate;
  private String mimeType;
  private double frameRate;

  /**
   * Constructs an object with flv as a url
   * @param flv represents the web url
   * @since 10-Jun-2009
   */
  public FLVMetaData(String flv) throws Exception
  {
    this.flv=flv;
    getMetaData();
  }

  /**
   * Constructs an object with flv and boolean value url.
   * @param flv represents the FLV path either a URL or a absolute file path.
   * @param url represents boolean value.
   */
  public FLVMetaData(String flv, boolean url) throws Exception
  {
    this.flv=flv;
    this.url=url;
    getMetaData();
  }

  /**
   * Extract the metadata for the flv and sets them in the properties.
   * If the property has 0.0 or null, then the information is not available on
   * the target FLV.
   * @throws Exception if something goes wrong.
   */
 private void getMetaData() throws Exception
 {
   InputStream fis=null;
    try{
      if(url)
      {
        //Creating the URL object
        URL url = new URL(flv);
        //Establishing the connection to the server
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        //Getting the remote input stream
        fis=connection.getInputStream();
      }
      else
      {
        fis=new FileInputStream(flv);
      }
      //Creating the bytes array to read the first 400 bytes data from input stream
      byte bytes[]=new byte[400];
      //Reading the data from the input stream
      fis.read(bytes);

      /*Fetching the properties. If the output shows -1 or null then
      consider that the FLV doesn't have that info on metadata*/
      double duration=getDouble(bytes,"duration");
      DecimalFormat f = new DecimalFormat("00");
      setDuration(f.format((int)duration/60)+":"+f.format((int)duration%60));
      setWidth(getDouble(bytes,"width"));
      setHeight(getDouble(bytes,"height"));
      setAudioDataRate(getDouble(bytes,"audiodatarate"));
      setVideoDataRate(getDouble(bytes,"videodatarate"));
      setFileSize(getDouble(bytes,"filesize"));
      setCreatedDate(getString(bytes,"creationdate"));
      setMimeType(getString(bytes,"mimetype"));
      setFrameRate(getDouble(bytes,"framerate"));

      //Closing the remote input stream
      fis.close();
    }catch(Exception e) {
      throw new Exception(e);
    }
    finally {
      if(fis!=null) {
        fis.close();
      }
    }
 }
 private double getDouble(byte[] bytes, String property)
 {
  //Creating a string from the bytes
  String metaData=new String(bytes);
  //Checking whether the property exists on the metadata
  int offset=metaData.indexOf(property);
  if(offset!=-1)
  {
   //Calculating the value from the bytes received from getBytes method
   return ByteBuffer.wrap(getBytes(bytes, offset+property.length()+1, 8)).getDouble();
  }
  else
  {
   //Returning -1 to notify the info not available
   return -1;
  }
 }
 private String getString(byte[] bytes, String property)
 {
  //Creating a string from the bytes
  String metaData=new String(bytes);
  //Checking whether the property exists on the metadata
  int offset=metaData.indexOf(property);
  if(offset!=-1)
  {
   //Constructing the string from the bytes received from getBytes method
   return new String(getBytes(bytes, offset+property.length()+3, 24));
  }
  else
  {
   //Returning null to notify the info not available
   return null;
  }
 }
 private byte[] getBytes(byte[] bytes, int offset, int length)
 {
  //Fetching the required number of bytes from the source and returning
  byte requiredBytes[]=new byte[length];
  for(int i=offset, j=0;j<length;i++,j++)
  {
   requiredBytes[j]=bytes[i];
  }
  return requiredBytes;
 }

  public void setFlv(String flv)
  {
    this.flv = flv;
  }

  public String getFlv()
  {
    return flv;
  }

  public void setUrl(boolean url)
  {
    this.url = url;
  }

  public boolean isUrl()
  {
    return url;
  }

  public void setCreatedDate(String createdDate)
  {
    this.createdDate = createdDate;
  }

  public String getCreatedDate()
  {
    return createdDate;
  }

  public void setMimeType(String mimeType)
  {
    this.mimeType = mimeType;
  }

  public String getMimeType()
  {
    return mimeType;
  }

  public void setWidth(double width)
  {
    this.width = width;
  }

  public double getWidth()
  {
    return width;
  }

  public void setHeight(double height)
  {
    this.height = height;
  }

  public double getHeight()
  {
    return height;
  }

  public void setAudioDataRate(double audioDataRate)
  {
    this.audioDataRate = audioDataRate;
  }

  public double getAudioDataRate()
  {
    return audioDataRate;
  }

  public void setVideoDataRate(double videoDataRate)
  {
    this.videoDataRate = videoDataRate;
  }

  public double getVideoDataRate()
  {
    return videoDataRate;
  }

  public void setFileSize(double fileSize)
  {
    this.fileSize = fileSize;
  }

  public double getFileSize()
  {
    return fileSize;
  }

  public void setFrameRate(double frameRate)
  {
    this.frameRate = frameRate;
  }

  public double getFrameRate()
  {
    return frameRate;
  }

  public void setDuration(String duration)
  {
    this.duration = duration;
  }

  public String getDuration()
  {
    return duration;
  }

  public static void main(String args[]) throws Exception
  {
   FLVMetaData metaData=new FLVMetaData("C:\\Users\\ksalic\\Downloads\\Fig21_06_07 (1)\\Fig21_06_07\\bailey.mpg", false);
   System.out.println(metaData.getDuration());
   metaData.setUrl(false);
   //metaData.setFlv("example-video1.flv");
   metaData.getMetaData();
   System.out.println(metaData.getDuration());
      System.out.println(metaData.getAudioDataRate());
      System.out.println(metaData.getCreatedDate());
      System.out.println(metaData.getHeight());
      System.out.println(metaData.getWidth());
      System.out.println(metaData.getMimeType());
      System.out.println(metaData.getFileSize());
  }
}