import cn.hutool.core.convert.Convert;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import psd.Layer;
import psd.Psd;
import psd.parser.layer.LayerType;
import psd.parser.object.PsdDescriptor;
import psd.parser.object.PsdTextData;

import javax.imageio.ImageIO;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Maokun.Zhong
 * @date 2021/11/17 10:01
 */
@Slf4j
public class PsdAnalyze {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("java -jar psd-analizer.jar source.psd dest.dir");
            return;
        }
        String dir = args[0];
        String fileName = args[1];
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
        Layer baseLayer = psdFile.getBaseLayer();
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
            Map<String,Object> json  = new HashMap<>();
            json.put("layerWidth",psd.getWidth());
            json.put("layerHeight",layer.getHeight());
            json.put("width",layer.getWidth());
            json.put("height",psd.getWidth());
            json.put("name",layer.getName());
            json.put("startX",layer.getLeft());
            json.put("startY",layer.getTop());
            json.put("endX",(layer.getLeft() + layer.getWidth()));
            json.put("endY",(layer.getTop() + layer.getHeight()));

            writer.write("psd");
            writer.newLine();
            writer.write("parent_width: " + psd.getWidth());
            writer.newLine();
            writer.write("parent_height: " + psd.getHeight());
            writer.newLine();
            writer.newLine();
            writer.write("layer:" + layer.getName());
            writer.newLine();
            writer.write("start_x: " + layer.getLeft());
            writer.newLine();
            writer.write("start_y: " + layer.getTop());
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
                json.put("engineData",layer.getTypeTool().get("EngineData"));
                BufferedWriter writer1 = new BufferedWriter(new FileWriter(new File(baseDir, path + ".json")));
                String jsonString = JSONObject.toJSONString(json);
                writer1.write(jsonString);
                writer1.close();
            }
            writer.close();
        }
    }

    private static void writeTypeTool(BufferedWriter writer, PsdDescriptor typeTool) throws IOException {
        writer.newLine();
        writer.newLine();
        writer.write("-*- text layer -*-");
        writer.newLine();

        writer.write("TEXT: " + typeTool.get("Txt"));
        writer.newLine();
        writer.write("METRICS: ");
        writer.newLine();
        writeMap(writer, Convert.convert(PsdTextData.class,typeTool.get("EngineData")).getProperties(), 0);

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeMap(BufferedWriter writer, Map<String, Object> map, int level) throws IOException {
        writeTabs(writer, level); writer.write("{"); writer.newLine();
        for (String key : map.keySet()) {
            writeTabs(writer, level + 1); writer.write(key + ": ");
            Object value = map.get(key);
            log.info(value.toString());
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
