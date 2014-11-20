Abra2
=====

A ClojureScript debugging application.

There is a distinct lack of first-class debugging tools for ClojureScript. In particular, because ClojureScript compiles to 
JavaScript, and because the JavaScript debugger included in browsers such as Chrome only work in a JavaScript context, you 
can't easily view the value of complex ClojureScript objects or execute arbitrary ClojureScript, which is crucial when you're 
stopped at a breakpoint.

{probably add more explanation here}     

Abra2 is an integrated ClojureScript Debugger, written in ClojureScript. It runs under atom-shell.

You simply enter the URL of the ClojureScript app you want to debug in the address bar and press the Debug button. 
This does the following:

 * Launches the application in a new window.
 * Splits the Abra window into two vertical panes with a splitter inbetween.
 * The BOTTOM pane loads the standard Chrome DevTools window which gives you the full DevTools functionality, like view and 
   manipulate the HTML and CSS in the Elements tab, and view and manipulate the source code in the Sources tab, including setting
   breakpoints, watches etc. and viewing the call stack and local variables when the code is stopped at a breakpoint. Note that 
   everything in this pane is in a JavaScript context. 
 * The TOP pane loads our ClojureScript debugger functions, allowing you to view and manipulate everything in ClojureScript 
   context, including the call stack, local and global variables and a built-in ClojureScript repl which allows you to 
   execute arbitrary ClojureScript, in the current context, whether the app is at a breakpoint or not.   

{probably add more explanation here}     


Building
--------

There's two js to be built. 

First, the "node" code:
```
lein cljsbuild auto main
```

Second, the "client" code:
```
lein cljsbuild auto abra
```

or both together

```
lein cljsbuild auto
```

Running
--------

Look in the `run.bat`
```
atom run
```

Dependencies
------------

Abra makes use of the following tools, libraries and technologies:

 * [node.js](http://nodejs.org)
     - "a platform built on Chrome's JavaScript runtime for easily building fast, scalable network applications."
     - This must be installed on your system, although we only use the npm command from this.
 * [atom-shell] (https://github.com/atom/atom-shell)
     - Basically a custom browser that will deliver your stand alone application
     - This must be installed on your system.
	 -The terminology used in the Atom Shell documentation can be confusing. When talking about "where code lives" they talk about "browser" and "client".  What they means by "browser" is the nodejs context i.e. it is the browser process that can access the file system etc. And "client" is the HTML world - the webpage which is sandboxed. 
 * [crmux](https://github.com/sidorares/crmux)
     - "Chrome developer tools remote protocol multiplexer."
     - This doesn't have to be installed as it's included in the Abra source code.
     - Crmux depends on the following libraries (installed using `npm install`, see Getting Started): 
         - [ws](http://einaros.github.io/ws): "a node.js websocket implementation"
         - [bl](https://github.com/rvagg/bl): "Buffer List: collect buffers and access with a standard readable Buffer interface" 
         - [colors](https://github.com/Marak/colors.js): "get colors in your node.js console like what."
 * [Lieningen](http://leiningen.org/)
     - This must be installed on your system.
     - The following Leiningen plugins are required but will be installed with the `lein deps` command (see Getting Started).     
         - [lein-cljsbuild v1.0.2](https://github.com/emezeske/lein-cljsbuild): "Leiningen plugin to make ClojureScript development easy."
         - [lein-git-deps v0.0.1](https://github.com/tobyhede/lein-git-deps): "git dependencies for leiningen."
 * [Java](http://TODO.com)
 * Clojure/ClojureScript libraries & Tools:
     - These will be installed with the `lein deps` command (see Getting Started).
         - [org.clojure/clojure v1.5.1](https://github.com/clojure/clojure): TODO: Check for latest version.
         - [org.clojure/clojurescript v0.0-2173](https://github.com/clojure/clojurescript): TODO: Check for latest version.
         - [cljs-ajax v0.2.4](https://github.com/JulianBirch/cljs-ajax): "simple Ajax client for ClojureScript". Latest version is 0.2.6.
         - [reagent v0.4.2](http://holmsand.github.io/reagent): "Minimalistic [React](http://facebook.github.io/react) for ClojureScript."
     - This will be installed with a shortcut in the checkouts directory (see Getting Started).
         - [reagent-components v0.1.0](https://github.com/Day8/reagent-components): Our very own Day8 UI library. Note even though changes in your local re-com will be picked up by the 
         build you will still need to run ```lein install``` once to get your local maven repository to pick it up.
 * Other JavaScript/CSS libraries:
     - [bootstrap.css v3](http://getbootstrap.com): "HTML, CSS, and JS framework". We're only interested in the css.
     - [React.js v0.9.0](http://facebook.github.io/react): "A JavaScript library for building user interfaces"
     - [Google Closure v?](https://developers.google.com/closure): Google JavaScript Tools and libraries, including a compiler which 
       optimises JavaScript.

## Getting Started

After going through the following set of commands, you will be fully configured and able to run the application:

(Note: I have collated these steps a long time after having set myself up so there's a chance I have something wrong here.
Let me know if anything didn't work) 

Start by going to a command line or terminal window, then change directory to your dev folder. This procedure will create an Abra folder 
under your dev folder.

First we need to configure git to NOT prompt you for username and password when connecting to PRIVATE remote repositories, such as Day8.

Apart from this being very convenient, it is a must-have to be able to use the lein-git-deps plugin.

Start by setting up your GitHub username and email address: 

    git config --global user.name "{your-github-username}"
    git config --global user.email "{your-github-email-address}"

Now we need a password manager extension:

    git config --global credential.helper store

I believe this worked for DJ under Linux, but it didn't for me under Windows. Possibly because I had already installed a credentials 
manager by installing [git extensions](http://chocolatey.org/packages/gitextensions). The included credentials manager is 
called [git-credential-winstore](https://gitcredentialstore.codeplex.com). I had to use this command instead:
    
    git config --global credential.helper wincred

Now we can download the Abra repository, which will create an Abra folder in the current folder:

    git clone https://github.com/Day8/Abra2.git

Now we need to load some node.js modules so go the the folder containing package.json:

Then install the modules. It tries to compile the ws module (Web Sockets) but this fails if you don't have the expected c/c++ compilers 
but the module has a graceful fallback and will still work for us:

	$cd run
    $npm install


Now ask Leiningen to load the ClojureScript dependencies from clojars.com (specified in project.clj):

    lein deps

And load our UI library directly from GitHub (again specified in project.clj). This is where we need no prompting for username 
and password, otherwise it will just hang. You should be able to Ctrl+C or Ctrl+Break out if you do get stuck:

    lein git-deps

Now all the source files are installed. Let's build the app:

    lein cljsbuild once

Everything involved in the build is placed in the `run\js\compiled` folder.

Now it's ready to run (assuming atom-shell is somewhere on your path):

    atom run

Under Windows you can type `run` as a shortcut for the above.

If you want to automatically recompile each source file as it is saved, use this command:

    lein cljsbuild auto

Under Windows you can type `build-auto` as a shortcut for the above.

If ever the app is not working properly and you suspect some of the build files are in some way, broken, you can clear ALL build files 
from the `Abra\deploy\core\lib\out` folder with the following\command:

    lein cljsbuild clean

Then you can do a `lein cljsbuild once` and that should prove if the problem is you


## Original Mike notes

Okay, I've worked out one way we could do the debugger which would mean it was uttlerly self contained.
No other moving parts. No other servers, no other windows, no starting up crmux, etc.  Just one application.

-  it will be done as a nodeweb-kit application  (Duh! why didn't we think of this before)

-  which will launch with the debug port 9222 on itself turned on (via contents of package.json as you worked out yesterday)

-  on launch it will startup crmux (not as a seperate process, crmux is just javascript, right, designed to run on node, so can be started "internally" to the app)

-  and this crmux will point back at itself (after all this app started with port 9222 open for debugging)

-  it will ask the user for the page to launch  (the page to be debugged)

-  it will launch that page and then ...

-  it will launch the normal chrome debugger AND our cljs debugger both pointing to this page via the locally started crmux

-  final piece:  in our cljs debug page we need to turn cljs into javascript ... we have been talking about using a server to do that .... but what if we instead used an embedded java (clj) applet embedded in our page??   From javascript (ClojureScript) we could execute a java method (clj method) inside the applet to turn cljs into javascript.  It certainly looks possible from the quick bit of googling I've done. And, if we could pull that off, then there'd be no server required.
