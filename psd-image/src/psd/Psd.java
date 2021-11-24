/*
 * This file is part of java-psd-library.
 * 
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package psd;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import com.freeway.image.combiner.ImageCombiner;
import com.freeway.image.combiner.element.RectangleElement;
import com.freeway.image.combiner.element.TextElement;
import com.freeway.image.combiner.enums.OutputFormat;
import psd.parser.*;
import psd.parser.header.*;
import psd.parser.layer.*;
@SuppressWarnings("unused")
public class Psd implements LayersContainer {
	private Header header;
	private final List<Layer> layers;
    private Layer baseLayer;
    private final String name;

    public Psd(InputStream stream) throws IOException {
        name = "unknown name";

		PsdFileParser parser = new PsdFileParser();
		parser.getHeaderSectionParser().setHandler(header -> Psd.this.header = header);

        final List<Layer> fullLayersList = new ArrayList<>();
		parser.getLayersSectionParser().setHandler(new LayersSectionHandler() {
            @Override
            public void createLayer(LayerParser parser) {
                fullLayersList.add(new Layer(parser));
            }

            @Override
            public void createBaseLayer(LayerParser parser) {
                Psd.this.baseLayer = new Layer(parser);
                if (fullLayersList.isEmpty()) {
                    fullLayersList.add(Psd.this.baseLayer);
                }
            }
        });

		parser.parse(stream);

        layers = makeLayersHierarchy(fullLayersList);
    }

	public Psd(File psdFile) throws IOException {
        name = psdFile.getName();

		PsdFileParser parser = new PsdFileParser();
		parser.getHeaderSectionParser().setHandler(header -> Psd.this.header = header);

        final List<Layer> fullLayersList = new ArrayList<>();
		parser.getLayersSectionParser().setHandler(new LayersSectionHandler() {
            @Override
            public void createLayer(LayerParser parser) {
                fullLayersList.add(new Layer(parser));
            }

            @Override
            public void createBaseLayer(LayerParser parser) {
                baseLayer = new Layer(parser);
                if (fullLayersList.isEmpty()) {
                    fullLayersList.add(baseLayer);
                }
            }
        });

		BufferedInputStream stream = new BufferedInputStream(new FileInputStream(psdFile));
		parser.parse(stream);
		stream.close();

        layers = makeLayersHierarchy(fullLayersList);
	}
	
    private List<Layer> makeLayersHierarchy(List<Layer> layers) {
        LinkedList<LinkedList<Layer>> layersStack = new LinkedList<>();
        ArrayList<Layer> rootLayers = new ArrayList<>();
        for (Layer layer : layers) {
            switch (layer.getType()) {
            case HIDDEN: {
                layersStack.addFirst(new LinkedList<>());
                break;
            }
            case FOLDER: {
                assert !layersStack.isEmpty();
                LinkedList<Layer> folderLayers = layersStack.removeFirst();
                for (Layer l : folderLayers) {
                    layer.addLayer(l);
                }
                if (layersStack.isEmpty()) {
                    rootLayers.add(layer);
                } else {
                    layersStack.getFirst().add(layer);
                }
                break;
            }
            case NORMAL: {
                if (layersStack.isEmpty()) {
                    rootLayers.add(layer);
                } else {
                    layersStack.getFirst().add(layer);
                }
                break;
            }
            default:
                assert false;
            }
        }
        return rootLayers;
    }

    public int getWidth() {
        return header.getWidth();
    }

    public int getHeight() {
        return header.getHeight();
    }

    public int getChannelsCount() {
        return header.getChannelsCount();
    }

    public int getDepth(){
        return header.getDepth();
    }

    public ColorMode getColorMode() {
        return header.getColorMode();
    }

    @Override
    public Layer getLayer(int index) {
        return layers.get(index);
    }

    @Override
    public int indexOfLayer(Layer layer) {
        return layers.indexOf(layer);
    }

    @Override
    public int getLayersCount() {
        return layers.size();
    }

    @Override
    public String toString() {
        return name;
    }

    public Layer getBaseLayer() {
        return this.baseLayer;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public void setBaseLayer(Layer baseLayer) {
        this.baseLayer = baseLayer;
    }

    public ImageCombiner buildImageCombiner(){
        Random random = new Random();
        int colorBound = 255;
        int i = 0;
        ImageCombiner imageCombiner = null;
        do {
            Layer layer = layers.get(i);
            if (i == 0) {
                imageCombiner = new ImageCombiner(layer.getWidth(), layer.getHeight(), Color.GRAY, OutputFormat.PNG);
            } else {
                Color color = new Color(random.nextInt(colorBound), random.nextInt(colorBound), random.nextInt(colorBound));
                RectangleElement rectangleElement = new RectangleElement(layer.getX(), layer.getY(), layer.getWidth(), layer.getHeight());
                rectangleElement.setColor(color);
                imageCombiner.addElement(rectangleElement);
            }
            i++;
        } while (i < this.getLayersCount());
        try {
            imageCombiner.combine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageCombiner;
    }
}
