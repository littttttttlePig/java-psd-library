package psd.parser.object;


import java.io.*;
import java.util.*;
import psd.parser.PsdInputStream;

/**
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
public class PsdTextData extends PsdObject {
	private final Map<String, Object> properties;
	private int cachedByte = -1;
	private boolean useCachedByte;

	public PsdTextData(PsdInputStream stream) throws IOException {
		int size = stream.readInt();
		int startPos = stream.getPos();
		properties = readMap(stream);
		assert startPos + size == stream.getPos();
	}

	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	private Map<String, Object> readMap(PsdInputStream stream) throws IOException {
		skipWhitespaces(stream);
		char c = (char) readByte(stream);

		if (c == ']') {
			return null;
		} else if (c == '<') {
			skipString(stream, "<");
		}
		HashMap<String, Object> map = new HashMap<>();
		while (true) {
			skipWhitespaces(stream);
			c = (char) readByte(stream);
			if (c == '>') {
				skipString(stream, ">");
				return map;
			} else {
				assert c == '/' : "unknown char: " + c + ", byte: " + (byte) c;
				String name = readName(stream);
				skipWhitespaces(stream);
				c = (char) lookForwardByte(stream);
				if (c == '<') {
					map.put(name, readMap(stream));
				} else {
					map.put(name, readValue(stream));
				}
			}
		}
	}

	private String readName(PsdInputStream stream) throws IOException {
		StringBuilder name = new StringBuilder();
		while (true) {
			char c = (char) readByte(stream);
			if (c == ' ' || c == 10) {
				break;
			}
			name.append(c);
		}
		return name.toString();
	}

	private Object readValue(PsdInputStream stream) throws IOException {
		char c = (char) readByte(stream);
		if (c == ']') {
			return null;
		} else if (c == '(') {
			// unicode string
			StringBuilder string = new StringBuilder();
			int stringSignature = readShort(stream) & 0xFFFF;
			assert stringSignature == 0xFEFF;
			while (true) {
				byte b1 = readByte(stream);
				if (b1 == ')') {
					return string.toString();
				}
				byte b2 = readByte(stream);
				if (b2 == '\\') {
					b2 = readByte(stream);
				}
				if (b2 == 13) {
					string.append('\n');
				} else {
					string.append((char) ((b1 << 8) | b2));
				}
			}
		} else if (c == '[') {
			ArrayList<Object> list = new ArrayList<>();
			while (true) {
				skipWhitespaces(stream);
				c = (char) lookForwardByte(stream);
				Object val;
				if (c == '<') {
					val = readMap(stream);
				} else {
					val = readValue(stream);
				}
				if (val == null) {
					return list;
				} else {
					list.add(val);
				}
			}
		} else {
			StringBuilder val = new StringBuilder();
			do {
				val.append(c);
				c = (char) readByte(stream);
			} while (c != 10 && c != ' ');
			if ("true".equals(val.toString()) || "false".equals(val.toString())) {
				return Boolean.valueOf(val.toString());
			} else {
				return Double.valueOf(val.toString());
			}
		}
	}

	private void skipWhitespaces(PsdInputStream stream) throws IOException {
		byte b;
		do {
			b = readByte(stream);
		} while (b == ' ' || b == 10 || b == 9);
		putBack();
	}

	private void skipString(PsdInputStream stream, String string) throws IOException {
		for (int i = 0; i < string.length(); i++) {
			char streamCh = (char) readByte(stream);
			assert streamCh == string.charAt(i) : "char " + streamCh + " mustBe " + string.charAt(i);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return properties.toString();
	}

	private byte readByte(PsdInputStream stream) throws IOException {
		if (useCachedByte) {
			assert cachedByte != -1;
			useCachedByte = false;
		} else {
			cachedByte = stream.read();
		}
		return (byte) cachedByte;
	}

	private short readShort(PsdInputStream stream) throws IOException {
		cachedByte = -1;
		useCachedByte = false;
		return stream.readShort();
	}

	private void putBack() {
		assert cachedByte != -1;
		assert !useCachedByte;
		useCachedByte = true;
	}

	private byte lookForwardByte(PsdInputStream stream) throws IOException {
		byte b = readByte(stream);
		putBack();
		return b;
	}
}


