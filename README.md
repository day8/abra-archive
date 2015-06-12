# Status

Unpublished - but getting close

ToDO:
  * fix the - and _ issue (doesn't handle mixed case)
  * after you press "Debug" the debug window comes up exactly over the Session window.
  * In the Session Details, remember the values put in last time.  In fact save the last few values. 
  * add a spinner after translate
  * if Abra is already running, produce an error for the user  (chrome remote debugging port is already locked)
  * What can we do about the first translate taking so long?
  * Pressing "Stop" doesn't really stop
  - Verify that the project directory contains a "project.clj" or a ".boot".
  - Tried to `lein run` got this
    D:\Day8-git\Abra2>lein run
    Your project already has a package.json file.  Please remove it.
    -attempt to delete
  - fix the - and _ issue for the mixed case
  - change code to use https://github.com/clojure/clojurescript/blob/master/src/clj/cljs/repl.clj#L397
     to load namespaces
     look at can't set *cljs-ns* variable error in analyzer/parse
  - look at the ability to run macros in the nrepl





# Abra [![Build Status](https://magnum.travis-ci.com/Day8/Abra.svg?token=ZxqzShvq5GKw1TUp9DLf&branch=master)](https://magnum.travis-ci.com/Day8/Abra)

Abra is a proof of concept ClojureScript debugger.

It works!!  It does the stuff you can't do via a bRepl.

You can set breakpoints and, when they are hit, you can:
 - inspect the value of ClojureScript variables in the call stack
 - evaluate arbitrary cljs code, referencing locals on the stack.

In the future, we hope to evolve Abra towards being
a [Time Traveling Debugger] and perhaps include some effortless,
optional [tracing](https://github.com/spellhouse/clairvoyant) facilities.

We think designing a good debugger is a data visualization
problem. Not that we really know what that means, just yet ,except to say that
ClojureScript has a unique set of properties which should allow for
something quite special to be done.

# The Bad News

It is especially fruitless to tell people to "never judge a book by its cover". Everyone
does and there is absolutely nothing you can say to make them not do it.

But really, you shouldn't judge Abra by its current poor cover. That worked, right?

Our goal until now has been to simply prove the concept, not
to create something lovely to use.

As a result, you will be confronted with a user interface which is
basic and clunky.

Oh, and along the same lines, and please don't judge Abra badly for its lack of documentation and
poor performance. Did we mention how it is currently a proof of concept?

## What Does It Give Me?

Chrome's DevTools already do so much.  You can set breakpoints, inspect code and page
elements, profile, etc.  So powerful. You want access to that.

But it lives in a dystopian world wholly without ClojureScript. It
can't show you the stack or its locals in ClojureScript terms. A dark place indeed.

Abra solves the problem by putting a ClojusreScrit specific UI side by
side with DevTools. You get all the power of DevTools, combined with the
a custom ClojureScript capability. They work together.

## How Does It Work?

Magic!  Didn't you notice the name?

Should you look behind the curtain, we can neither confirm nor deny that you'd find pieces like:

  - Reagent
  - re-com    (our component library)
  - re-frame  (our framework)
  - [atom-shell](https://github.com/atom/atom-shell)
  - [Chrome Remote debugging protocol](https://developer.chrome.com/devtools/docs/debugger-protocol)
  - [crmux](https://github.com/sidorares/crmux)


## SetUp

To install Abra:

1. Install [Leiningen] and [Node.js].
1. Checkout this repo:

    ```sh
    git clone https://github.com/Day8/Abra.git
    ```

1. cd into root folder:

    ```sh
    cd Abra
    ```

1. download dependencies (you might see a fair bit of downloading):

    ```sh
    lein deps
    ```

1. compile the application (is actually two compiles):

    ```sh
    lein build
    ```

XXX trouble shooting -- how to run the tests??

## Run Abra

1. To run Abra we must start atom-shell the right way:

    ```sh
    lein run
    ```

1. When Abra starts ... XXXXXX



Dependencies
------------

Abra makes use of the following tools, libraries and technologies:

 * [node.js](http://nodejs.org)
     - "a platform built on Chrome's JavaScript runtime for easily building fast, scalable network applications."
     - This must be installed on your system, although we only use the npm command from this.
 * [electron] (https://github.com/atom/electron)
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



[Leiningen]:http://leiningen.org
[Node.js]:http://nodejs.org
[Atom Shell]:https://github.com/atom/atom-shell
[Time Traveling Debugger]:http://debug.elm-lang.org/