/*
 * Copyright (c) 2013-2014, Nikita Lipsky, Excelsior LLC.
 *
 *  Java ReStart is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Java ReStart is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Java ReStart.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package javarestart;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class JavaRestartLauncher {

    private static File splashLocation;

    public static void fork(final String ... args)  {

        String javaHome = System.getProperty("java.home");
        System.out.println(javaHome);
        if (splashLocation == null) {
            File codeSource = new File(JavaRestartLauncher.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm().substring(6));
            System.out.println(codeSource);
            if (codeSource.isDirectory()) {
                splashLocation = new File (codeSource, "defaultSplash.gif");
            } else {
                splashLocation = Utils.fetchResourceToTempFile("defaultSplash",
                        ".gif", JavaRestartLauncher.class.getClassLoader().getResource("defaultSplash.gif"));
            }
        }
        String classpath = System.getProperty("java.class.path");

        final File javawPath;
        switch (OS.get()) {
            case WINDOWS:
                javawPath = new File(javaHome, "\\bin\\javaw");
                break;
            case NIX:
                javawPath = new File(javaHome,  "/bin/java");
                break;
            case MAC:
                throw new UnsupportedOperationException("mac is not tested yet");
            default:
                throw new UnsupportedOperationException();
        }

        String javaLauncher = javawPath.getAbsolutePath()
                + " -splash:" + splashLocation.getAbsolutePath()
                + " -Dbinary.css=false -cp \""
                + classpath
                + "\" "
                + JavaRestartLauncher.class.getName();

        for (final String arg: args) {
            javaLauncher = javaLauncher + " " + arg;
        }

        System.out.println(javaLauncher);

        final String finalJavaLauncher = javaLauncher;
        (new Thread(){
            @Override
            public void run() {
                try {
                    Runtime.getRuntime().exec(finalJavaLauncher).waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public static String getText(String url) throws IOException {
        URL website = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) website.openConnection();
        try (LineNumberReader in = new LineNumberReader(
                    new InputStreamReader(
                            connection.getInputStream())))
        {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            return response.toString();
        }
    }

    public static JSONObject getJSON(String url) throws IOException {
        return (JSONObject) JSONValue.parse(getText(url));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: <URL> {<MainClass>}");
            return;
        }

        if (args[0].equals("fork")) {
            String[] args2 = new String[args.length - 1];
            for (int i = 0; i < args.length -1; i++) {
                args2[i] = args[i + 1];
            }
            fork(args2);
            return;
        }

        AppClassloader loader = new AppClassloader(args[0]);
        Thread.currentThread().setContextClassLoader(loader);
        String main;
        JSONObject obj = getJSON(args[0]);
        if (args.length < 2) {
            main = (String) obj.get("main");
        } else {
            main = args[1];
        }

        String splash = (String) obj.get("splash");
        if ( splash != null) {
            SplashScreen scr = SplashScreen.getSplashScreen();
            if (scr != null) {
                URL url = loader.getResource(splash);
                scr.setImageURL(url);
            }
        }

        //auto close splash after 45 seconds
        Thread splashClose = new Thread(){
            @Override
            public void run() {
                try {
                    sleep(45000);
                } catch (InterruptedException e) {
                }
                SplashScreen scr = SplashScreen.getSplashScreen();
                if ((scr!=null) && (scr.isVisible())) {
                    scr.close();
                }
            }
        };
        splashClose.setDaemon(true);
        splashClose.start();

        Class mainClass = loader.loadClass(main);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.setAccessible(true);
        mainMethod.invoke(null, new Object[]{new String[0]});
    }
}
