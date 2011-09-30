import com.xuggle.xuggler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @version $Id$
 */
public class Test {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) {
       // if (args.length == 0)
          //  throw new IllegalArgumentException("must pass in a filename as the first argument");

        String file = "C:\\Users\\ksalic\\Downloads\\Fig21_06_07 (1)\\Fig21_06_07\\bailey.mpg";
        File theFile = new File(file);
        if (!theFile.exists())
            throw new IllegalArgumentException("the file " + file + " does not exist.");

        System.out.println("Opening video file: " + file);

        // copied/modified from Xuggler code below
        // Create a Xuggler container object
        IContainer container = IContainer.make();

        // Open up the container
        if (container.open(file, IContainer.Type.READ, null) < 0)
            throw new IllegalArgumentException("could not open file: " + file);

        if (container.queryStreamMetaData() < 0)
            throw new IllegalStateException("couldn't query stream meta data for some reason...");

        for (int i = 0; i < container.getNumProperties(); i++) {
            IProperty prop = container.getPropertyMetaData(i);
            System.out.println(prop.getHelp() + " (" + prop.getName() + "): \t" + container.getPropertyAsString(prop.getName()));
        }

        // query how many streams the call to open found
        int numStreams = container.getNumStreams();
        //log.info("file \"" + file + "\": " + numStreams + " stream" + (numStreams == 1 ? "" : "s"));
        System.out.printf("file \"%s\": %d stream%s; ", file, numStreams, numStreams == 1 ? "" : "s");
        System.out.printf("duration (ms): %s; ", container.getDuration() == Global.NO_PTS ? "unknown" : "" + container.getDuration() / 1000);
        System.out.printf("start time (ms): %s; ", container.getStartTime() == Global.NO_PTS ? "unknown" : "" + container.getStartTime() / 1000);
        System.out.printf("file size (bytes): %d; ", container.getFileSize());
        System.out.printf("bit rate: %d; ", container.getBitRate());
        container.getParameters();
        System.out.printf("\n");

        // and iterate through the streams to print their meta data
        for (int i = 0; i < numStreams; i++) {
            // Find the stream object
            IStream stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            // and now print out the meta data.
            System.out.printf("stream %d: ", i);
            System.out.printf("type: %s; ", coder.getCodecType());
            System.out.printf("codec: %s; ", coder.getCodecID());
            System.out.printf("duration: %s; ", stream.getDuration() == Global.NO_PTS ? "unknown" : "" + stream.getDuration());
            System.out.printf("start time: %s; ", container.getStartTime() == Global.NO_PTS ? "unknown" : "" + stream.getStartTime());
            System.out.printf("language: %s; ", stream.getLanguage() == null ? "unknown" : stream.getLanguage());
            System.out.printf("timebase: %d/%d; ", stream.getTimeBase().getNumerator(), stream.getTimeBase().getDenominator());
            System.out.printf("coder tb: %d/%d; ", coder.getTimeBase().getNumerator(), coder.getTimeBase().getDenominator());

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                System.out.printf("sample rate: %d; ", coder.getSampleRate());
                System.out.printf("channels: %d; ", coder.getChannels());
                System.out.printf("format: %s", coder.getSampleFormat());
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                System.out.printf("width: %d; ", coder.getWidth());
                System.out.printf("height: %d; ", coder.getHeight());
                System.out.printf("format: %s; ", coder.getPixelType());
                System.out.printf("frame-rate: %5.2f; ", coder.getFrameRate().getDouble());
            }
            System.out.printf("\n");
        }

        // If the user passes -Dxuggle.options, then we print
        // out all possible options as well.
        String optionString = System.getProperty("xuggle.options");
        if (optionString != null)
            printOptions(container);
    }

    /**
     * This method iterates through the container, and prints out available
     * options for the container, and each stream.
     *
     * @param aContainer Container to print options for.
     */
    private static void printOptions(IContainer aContainer) {

        System.out.printf("\n");
        System.out.printf("IContainer Options:\n");
        int numOptions = aContainer.getNumProperties();

        for (int i = 0; i < numOptions; i++) {
            IProperty prop = aContainer.getPropertyMetaData(i);
            printOption(aContainer, prop);
        }
        System.out.printf("\n");

        int numStreams = aContainer.getNumStreams();
        for (int i = 0; i < numStreams; i++) {
            IStreamCoder coder = aContainer.getStream(i).getStreamCoder();
            System.out.printf(
                    "IStreamCoder options for Stream %d of type %s:\n", i,
                    coder.getCodecType());
            numOptions = coder.getNumProperties();
            for (int j = 0; j < numOptions; j++) {
                IProperty prop = coder.getPropertyMetaData(j);
                printOption(coder, prop);
            }
        }
    }

    private static void printOption(IConfigurable configObj, IProperty aProp) {
        if (aProp.getType() != IProperty.Type.PROPERTY_FLAGS) {
            System.out.printf("  %s: %s\n", aProp.getName(), configObj
                    .getPropertyAsString(aProp.getName()));
        } else {
            // it's a flag
            System.out.printf("  %s: %d (", aProp.getName(), configObj
                    .getPropertyAsLong(aProp.getName()));
            int numSettings = aProp.getNumFlagSettings();
            long value = configObj.getPropertyAsLong(aProp.getName());
            for (int i = 0; i < numSettings; i++) {
                IProperty prop = aProp.getFlagConstant(i);
                long flagMask = prop.getDefault();
                boolean isSet = (value & flagMask) > 0;
                System.out.printf("%s%s; ", isSet ? "+" : "-", prop.getName());
            }
            System.out.printf(")\n");
        }
    }


}
