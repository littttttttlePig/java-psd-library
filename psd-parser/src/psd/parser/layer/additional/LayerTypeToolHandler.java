package psd.parser.layer.additional;

import psd.parser.object.PsdDescriptor;

public interface LayerTypeToolHandler {

	void typeToolTransformParsed(Matrix transform);
	void typeToolDescriptorParsed(int version, PsdDescriptor descriptor);

}
