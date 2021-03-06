package incanshift.desktop;

import java.io.IOException;
import java.util.Arrays;

import com.badlogicgames.packr.Packr;
import com.badlogicgames.packr.Packr.Config;
import com.badlogicgames.packr.Packr.Platform;

public class DesktopPacker {

	public static void main(String[] arg) {
		Config config = new Config();
		config.platform = Platform.windows;
		config.jdk = "/home/user/incoming/openjdk-1.7.0-u80-unofficial-windows-i586-installer.zip";
		config.executable = "myapp";
		config.jar = "/home/user/inca.jar";
		config.mainClass = "incanshift/desktop/DesktopLauncher";
		config.vmArgs = Arrays.asList("-Xmx1G");
		config.minimizeJre = new String[]{"jre/lib/rt/com/sun/corba",
				"jre/lib/rt/com/sun/jndi"};
		config.outDir = "out-windows";

		try {
			new Packr().pack(config);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
