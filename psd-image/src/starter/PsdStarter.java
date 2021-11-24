package starter;

import com.freeway.image.combiner.ImageCombiner;
import psd.Psd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Maokun.Zhong
 * @date 2021/11/24 9:51
 */
public class PsdStarter {

    public static void main(String[] args) {

    }

    private Psd psd = null;

    public PsdStarter(InputStream stream) {
        try {
            this.psd = new Psd(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PsdStarter(File psdFile) {
        try {
            this.psd = new Psd(psdFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ImageCombiner analyze(){
        if (psd == null){
            throw new RuntimeException("psd parser error!");
        }
        return psd.buildImageCombiner();
    }
}
