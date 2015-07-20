package incanshift.gameobjects;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.StringBuilder;

public class TextParser {

	public static ArrayMap<String, Array<String>> parse(FileHandle textFile) {
		ArrayMap<String, Array<String>> textMap = new ArrayMap<String, Array<String>>();
		String text = textFile.readString();
		String[] sections = text.split("#");

		for (int i = 0; i < sections.length; i++) {
			String chapter = sections[i];
			String[] chapterLines = chapter.split(System
					.getProperty("line.separator"));
			String[] nameAndIndex = (chapterLines[0]).split("\\.");
			if (nameAndIndex.length < 2) {
				continue;
			}
			String name = nameAndIndex[0];
			// int index = Integer.parseInt(nameAndIndex[1]);
			String chapterText = "";
			if (chapterLines.length >= 2) {
				for (int j = 1; j < chapterLines.length; j++) {
					chapterText += ("\n" + chapterLines[j]);
				}
			}
			StringBuilder builder = new StringBuilder();
			String[] words = chapterText.split(" ");
			int currentLineLength = 0;
			// TODO: move the line formatting into billboard class...
			int maxLineLength = 60;
			for (String word : words) {
				if (word.equals("\n")) {
					continue;
				}
				int wordLengthWithSpace = word.length() + 1;
				currentLineLength += wordLengthWithSpace;
				if (word.startsWith("\n")) {
					builder.append(word);
					currentLineLength = wordLengthWithSpace;

				} else if (currentLineLength > maxLineLength) {
					builder.append("\n" + word);
					currentLineLength = wordLengthWithSpace;
				} else {
					builder.append(" " + word);
					currentLineLength += wordLengthWithSpace;
				}
			}

			// TODO: organize from index number not order
			if (!textMap.containsKey(name)) {
				textMap.put(name, new Array<String>());
			}
			textMap.get(name).add(builder.toString());
		}
		return textMap;

	}

}
