package incanshift.gameobjects;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.StringBuilder;

public class TextParser {

	public static String text = "#intro.0\n"+
			"”We, the human race, will be known through out time by the traces that we leave behind.\"\n"+
			"- Dakota\n"+
			"#intro.1\n"+
			"\"How the children of tomorrow will interpenetrate these traces however, is beyond our comprehension.\"\n"+
			"- Christoffer Lundberg\n"+
			"\n"+
			"#well.0                   \n"+
			"Gold coins.\n"+
			"#well.1\n"+
			"Human bones.\n"+
			"#well.2     \n"+
			"Incense.\n"+
			"#well.3     \n"+
			"Jade statue.\n"+
			"#well.4     \n"+
			"All these things have been sacrificed to the gods.\n"+
			"\n"+
			"#chiuateteo.0\n"+
			"You are no god, and yet, I recognize your face... Have you been... resurrected?\n"+
			"#chiuateteo.1\n"+
			"You, who used to be of such great service to the jaguar god... what could possibly have killed you?\n"+
			"#chiuateteo.2\n"+
			"We, the Chiuateteo, are messengers from the kingdom of death, and servants of Tetzcatlipoca, the Jaguar god, to whom this holy site is dedicated.\n"+
			"#chiuateteo.3\n"+
			"Why did you return here?\n"+
			"#chiuateteo.4\n"+
			"A resurrected human must be powerful. In the Red Temple, the Feathered Serpent might give you a second chance.\n"+
			"#chiuateteo.5\n"+
			"All the people have disappeared. Poof! Gone! Just like that! And without man, there is no one to speak the names of the gods.\n"+
			"#chiuateteo.6\n"+
			"You appear to be lost.\n"+
			"#chiuateteo.7\n"+
			"The rivalry between Tetzcatlipoca and Quetzcoatl is finally at its end.\n"+
			"\n"+
			"#player.0\n"+
			"Where am I? What is this place?\n"+
			"#player.1\n"+
			"What god does this land belong to?\n"+
			"#player.2\n"+
			"This whole place feels strangely familiar. Yet... so different.\n"+
			"#player.3\n"+
			"(press right mousebutton)With THE MASK the snake gods powers infuse you. \n"+			
			"#player.4\n"+
			"Was I... a servant of the jaguar god?\n"+
			"#player.5\n"+
			"That mask... I feel like I have used it before. In some kind of ritual or maybe... was it a different mask?\n"+
			"\n"+
			"#glyphs.0\n"+
			"Ever since the creation of the worlds, a persistent rivalry between Quetzcoatl and Tezcatlipoca has layed as the foundation for many conflicts of man.\n"+
			"#glyphs.1\n"+
			"Some blue masks are symbols of the jaguar god, Tezcatlipoca. Others are of the feathered serpent, Quetzcoatl.\n"+
			"\n"+
			"#glyphs.2\n"+
			"To enter his sanctuary, Kukulcan requires a sacrifice of blood. We praise him with our blood. We honor him with our blood. We give thanks to him with our blood! To him, the feathered serpent, Quetzcoatl, Kukulcan, to our master we willingly give our blood!\n"+
			"\n"+
			"#gods.0\n"+			
			"#hint.0\n"+
			"The Mask[TAB] reveal the Truth. The Truth reveals the masks.\n"+
			"#hint.2\n"+
			"I can hear the snake inside of you(right mousebutton).\n"+
			"#hint.1\n"+
			"You have a MASK[TAB]...\n"+
			"#hint.3\n"+
			"One Mask a day keeps the other masks away. [TAB]\n"+	
			"#hint.4\n"+
			"If you wear a Mask[TAB] you can enter the invisible portals of blood. \n"+			
			"#hint.5\n"+
			"When you wear that Mask[TAB], I can almost see the gods through your eyes.\n"+
			"\n"+
			"#hint.5\n"+
			"With THE MASK the snake gods powers infuse you(press right mousebutton). \n"+			
			"\n"+
			"#temp.0\n"+
			"Learn to see the differance between the masks, and you may change the destiny of all mankind for eternity.";

	public static ArrayMap<String, Array<String>> parse(FileHandle textFile) {
		ArrayMap<String, Array<String>> textMap = new ArrayMap<String, Array<String>>();
//		String text = textFile.readString();
		String[] sections = text.split("#");

		for (int i = 0; i < sections.length; i++) {
			String chapter = sections[i];
			String[] chapterLines = chapter.split("\n");
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
