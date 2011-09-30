import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javax.media.*;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Krishna Vangapandu *
 */
class VideoUtility {
    @SuppressWarnings("deprecation")
    /** * videoFile - path to the video File. */
    public static Player getPlayer(String videoFile)
            throws NoPlayerException, IOException {
        File f = new File(videoFile);
        if (!f.exists()) throw new FileNotFoundException("File doesnt exist");
        MediaLocator ml = new MediaLocator(f.toURL());
        Player player = Manager.createPlayer(ml);
        player.realize();
        while (player.getState() != Player.Realized) ;
        return player;
    }

    public static float getFrameRate(Player player) {
        return (float) noOfFrames(player) / (float) player.getDuration().getSeconds();
    }

    public static int noOfFrames(Player player) {
        FramePositioningControl fpc = (FramePositioningControl) player.getControl("javax.media.control.FramePositioningControl");
        Time duration = player.getDuration();
        int i = fpc.mapTimeToFrame(duration);
        if (i != FramePositioningControl.FRAME_UNKNOWN) return i;
        else return -1;
    }

    /**
     * * @param player - the player from which you want to get the image
     *
     * @param frameNumber - the framenumber you want to extract
     * @return Image at the current frame position
     */
    public static Image getImageOfCurrentFrame(Player player, int frameNumber) {
        FramePositioningControl fpc = (FramePositioningControl) player.getControl("javax.media.control.FramePositioningControl");
        FrameGrabbingControl fgc = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");
        return getImageOfCurrentFrame(fpc, fgc, frameNumber);
    }

    public static Image getImageOfCurrentFrame(FramePositioningControl fpc, FrameGrabbingControl fgc, int frameNumber) {
        fpc.seek(frameNumber);
        Buffer frameBuffer = fgc.grabFrame();
        BufferToImage bti = new BufferToImage((VideoFormat) frameBuffer.getFormat());
        return bti.createImage(frameBuffer);
    }

    public static FramePositioningControl getFPC(Player player) {
        FramePositioningControl fpc = (FramePositioningControl) player.getControl("javax.media.control.FramePositioningControl");
        return fpc;
    }

    public static FrameGrabbingControl getFGC(Player player) {
        FrameGrabbingControl fgc = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");
        return fgc;
    }

    public static ArrayList<Image> getAllImages(Player player) throws IOException {
        ArrayList<Image> imageSeq = new ArrayList<Image>();
        int numberOfFrames = noOfFrames(player);
        FramePositioningControl fpc = getFPC(player);
        FrameGrabbingControl fgc = getFGC(player);
        for (int i = 0; i <= numberOfFrames; i++) {
            Image img = getImageOfCurrentFrame(fpc, fgc, i);
            if (img != null) {
                //ImageIO.write((BufferedImage) img, "jpg", new File("Z:\\snapshot\\Test.jpg"));
                //ImageIO.write((BufferedImage)img, "PNG", new File("C:\\Test.PNG"));
                BufferedImage bimg = null;
                int w = img.getWidth(null);
                int h = img.getHeight(null);
                int[] pixels = new int[w * h];
                PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
                try {
                    pg.grabPixels();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

                bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                bimg.setRGB(0, 0, w, h, pixels, 0, w);

// Encode as a JPEG
                FileOutputStream fos = new FileOutputStream("Z:\\out.jpg");
                JPEGImageEncoder jpeg = JPEGCodec.createJPEGEncoder(fos);
                jpeg.encode(bimg);
                fos.close();
                break;
                //imageSeq.add(img);
            }
        }
        return imageSeq;
    }

    public static ArrayList<Image> getAllImages(String fileName) throws NoPlayerException, IOException {
        Player player = getPlayer(fileName);
        ArrayList<Image> img = getAllImages(player);
        player.close();
        return img;
    }

    public static void main(String[] args) {
        try {
            getAllImages("C:\\Users\\ksalic\\Downloads\\example-video1.flv");
        } catch (NoPlayerException e) {
            System.out.println(e.getLocalizedMessage());
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}