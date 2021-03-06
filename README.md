Java ReStart
=====
"The Network is The Computer! Is not it?"

Java ReStart is a technology that allows to run Java applications instantly from Internet without installation, 
downloading necessary parts of an application dynamically at runtime and executing the application in parallel with downloading (analogy with YouTube and Web). 
Thus the application starts as fast as web applications and gains other advantages of the Web such as invisible autoupdates.
On the other hand, unlike to the Web, application will have native desktop/tablet look-n-feel and performance,
and a developer also has a rich choice of languages and technologies that are available on top of Java platform from JavaScript to Scala 
without compromising the performance. Also the technology is free from other limits and restrictions imposed by today's web technologies based on HTML/JavaScript.
Unlike Java Web Start (or Flash/Silverlight) an application comes to end-user not as a whole but partially, where only required parts are loaded, 
thus theoretically an application of any complexity can be deployed by this technology from rich IDEs to graphical designer/engineering tools.

How it works
=====
It is a client/server technology.
The server provides classes/resources of an application on a request and the client downloads classes/resources of an application on demand (lazily) 
and executes the application in parallel with downloading.
The server has a very simple REST interface now:

```
GET /{application} -- returns an application descriptor in JSON format (main class name of the application (entry point), splash, etc.)
GET /{application}?resource={resource} -- returns a class/resource of a referenced application
```

The client in turn has a very simple command line interface:

```
java javarestart.JavaRestartLauncher <URL>
```

where URL has a form [BaseURL]/[AppName].

First, the client fetches an application descriptor, if a splash is scpecified in the descriptor the client downloads it and immediatly shows, 
then it downloads main class of the application and loads it using a classloader that tries to emulate default JVM application classloader 
but instead of loading the classes from HDD it fetches them from URL using REST interface above. 
This way, only required classes/resources are downloaded by the client and application starts right from the first downloaded main class.

How to run 
=====
The sources come with a demo sustaining the concept.

First you need to run the server in an application server of your choice (Tomcat, Jetty, whatever).
Before the run you should setup demo applications: 
correct server/src/main/resources/application.properties "apps.path" property pointing to apps directory.
apps contains the following Java UI applications with the descriptions (app.properties): 
  * Java2Demo - standard AWT/Java2D demo, 
  * SwingSet2 - standard Swing demo, 
  * SWT - demo showing SWT standard controls,
  * Jenesis - Sega Genesis emulator written using Java OpenGL (jogl),
  * BrickBreaker - JavaFX arcanoid game demo
  * Ensemble - standard JavaFX ensemble demo
  * Game2048 - JavaFX version of 2048 game written by Bruno Borges (https://github.com/brunoborges/fx2048)

After launching the server, you may run the apps using
```
java javarestart.JavaRestartLauncher http://localhost:8080/apps/<AppName> 
```
command (URL example -- http://localhost:8080/apps/Java2Demo).

Or you may run JavaFX demo that in turn will run the demos above by itself (located in "demo" folder):
```
java javarestart.demo.JavaRestartDemo
```

You can also run the samples from forked version of Bruno Borges WebFX browser 
(https://github.com/pjBooms/webfx): 
point the browser to http://localhost:8080 and click "Java Restart Demo" link.

Run Notes:
Ensemble demo does not work with Java 8 now and with Java 7 it does not load all resources that are referenced by the demo (f.i. it does not load "close" button icon).
SWT, Jenesis can run only with 32-bit JRE on Windows (they are using 32-bit native libraries).
JavaRestartDemo is forking JVM to run demos and the way it forks JVM can work on Windows only.

Java ReStart on Jelastic
=====
The Java ReStart server with the sample applications is deployed now on Jelastic Cloud:

http://javarestart.jelasticloud.com/

So you may run the samples above now with just Java ReStart client (no need to deploy on local server):

```
java javarestart.JavaRestartLauncher http://javarestart.jelasticloud.com/apps/<AppName> 
```

Or via my fork of WebFX browser:

point the browser to http://javarestart.jelasticloud.com and click "Java Restart Demo" link.


Adding your own applications
=====
To add your own application that you would like to launch from Internet you need to put it to a subfolder of apps folder 
and provide app.properties where you describe main class and classpath of your application (see other applications located in apps for example). 
After that you may launch it with the client:

```
java javarestart.JavaRestartLauncher <BaseURL>/<AppName>
```
TODO
=====
1. Implement caching of downloaded classes by the client 
2. Support versioning of the apps on the server (thus if a version is not changed on the server, the cached version can be taken safely)
3. Implement profiling class/resource retrieving sequence on the server and change the server REST interface to allow to upload frequently used classes/resources 
   by a single HTTP response with a aggressively packed stream with the right class order. This way we can drastically optimize the startup of applications 
   (no need to handle thousands of HTTP requests).
4. Improve application descriptions. Now only classpath and main is specified for an application but it is good also to provide VM properties, splash, etc.
5. Support custom classloading. If an application is loaded not only by application classloader but with its own custom classloaders (OSGi, Netbeans RCP classloaders)
   we should add support for such classloaders both to the server and to the client:
   The server should perform class references resolution that is defined by the classloaders used by an application. 
   It also must give to the client not original classloader but it's client reflection that will redirect class loading requests to the server.
   The REST API interface should also be changed to support custom classloaders class references 
   (besides class names, the server should also get a classloader ID to know by which classloader the class should be loaded).
6. Test the technology against Netbeans/IDEA/Eclipse

Nominations
=====
The project has won in "Tech" nomination of HackDay #29 hackathon (www.hackday.ru). The pitch video is here (in Russian):
http://vk.com/video-45718857_166854884
