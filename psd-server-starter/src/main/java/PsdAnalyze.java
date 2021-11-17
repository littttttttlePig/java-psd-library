import lombok.extern.slf4j.Slf4j;
import psd.Layer;
import psd.Psd;
import psd.parser.layer.LayerType;
import psd.parser.object.PsdDescriptor;
import psd.parser.object.PsdObject;
import psd.parser.object.PsdTextData;

import javax.imageio.ImageIO;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;


/**
 * @author Maokun.Zhong
 * @date 2021/11/17 10:01
 */
@Slf4j
public class PsdAnalyze {
    public static void main(String[] args) {
        String dir = "C:\\Users\\20201217-010\\Desktop\\PSD文件\\PSD文件\\";
        String fileName = "0301-钟-Loafers (5).psd";
        long startTime = System.currentTimeMillis();

        try {
            processPsd(new File(dir.concat(fileName)), new File(dir));
        } catch (Exception e) {
            e.printStackTrace();
        }

        long finishTime = System.currentTimeMillis();
        long time = finishTime - startTime;
        String timeStr = "" + (time / 1000) + "." + (time % 1000);
        log.info("Time: " + timeStr + " sec.");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void processPsd(File inputFile, File outputDir) throws IOException {
        Psd psdFile = new Psd(inputFile);
        outputDir.mkdirs();

        int total = psdFile.getLayersCount();
        for (int i = 0; i < total; i++) {
            Layer layer = psdFile.getLayer(i);
            log.info("processing: " + layer.getName() + " - " + (i * 100 / total) + "%");
            writeLayer(psdFile, layer, outputDir);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeLayer(Psd psd, Layer layer, File baseDir) throws IOException {
        if (layer.getType() == LayerType.NORMAL) {
            String path = getPath(layer);
            File outFile = new File(baseDir, path + ".png");
            outFile.getParentFile().mkdirs();
            if (layer.getImage() != null) {
                ImageIO.write(layer.getImage(), "png", outFile);
            } else {
                log.warn("!!!!NULL layer: " + layer.getName());
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(baseDir, path + ".txt")));
            writer.write("psd");
            writer.newLine();
            writer.write("width: " + psd.getWidth());
            writer.newLine();
            writer.write("height: " + psd.getHeight());
            writer.newLine();
            writer.newLine();
            writer.write("layer:" + layer.getName());
            writer.newLine();
            writer.write("x: " + layer.getLeft());
            writer.newLine();
            writer.write("y: " + layer.getTop());
            writer.newLine();
            writer.newLine();
            writer.write("end_x: " + (layer.getLeft() + layer.getWidth()));
            writer.newLine();
            writer.write("end_y: " + (layer.getTop() + layer.getHeight()));
            writer.newLine();
            writer.newLine();
            writer.write("width: " + layer.getWidth());
            writer.newLine();
            writer.write("height: " + layer.getHeight());
            writer.newLine();

            if (layer.getTypeTool() != null) {
                writeTypeTool(writer, layer.getTypeTool());
            }
            writer.close();
        }
    }

    private static void writeTypeTool(BufferedWriter writer, PsdDescriptor typeTool) throws IOException {
        writer.newLine();
        writer.newLine();
        writer.write("-*- text layer -*-");
        writer.newLine();

        writer.write("TEXT: " + typeTool.get("Txt "));
        writer.newLine();
        writer.write("METRICS: ");
        writer.newLine();

        PsdTextData textData = (PsdTextData) typeTool.get("EngineData");
        Map<String, Object> properties = textData.getProperties();
        writeMap(writer, properties, 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeMap(BufferedWriter writer, Map<String, Object> map, int level) throws IOException {
        writeTabs(writer, level); writer.write("{"); writer.newLine();

        for (String key : map.keySet()) {
            writeTabs(writer, level + 1); writer.write(key + ": ");
            Object value = map.get(key);
            if (value instanceof Map) {
                writer.newLine();
                writeMap(writer, (Map) value, level + 1);
            } else {
                writer.write(String.valueOf(value));
                writer.newLine();
            }
        }
        writeTabs(writer, level); writer.write("}"); writer.newLine();
    }

    private static void writeTabs(BufferedWriter writer, int tabsCount) throws IOException {
        while (tabsCount > 0) {
            writer.write("\t");
            tabsCount--;
        }
    }
    private static String getPath(Layer layer) {
        String dir = "";
        if (layer.getParent() != null) {
            dir = getPath(layer.getParent()) + File.pathSeparator;
        }
        return dir + layer.getName();
    }
}
