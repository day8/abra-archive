# Status

Works!  But it is stuck at a proof of concept stage.  

Somehow we've never seemed to adopt it in our daily practice as developers. Somehow println/cljs-devtools/figwheel seemed to win.  So we're not pushing on with it.  Waiting instead for tech like cljs-devtools and dirac to mature.  And for Colin to do something better in Cursive - hurry up Colin, we're paying customers now :-)

[![Build Status](https://magnum.travis-ci.com/Day8/Abra.svg?token=ZxqzShvq5GKw1TUp9DLf&branch=master)](https://magnum.travis-ci.com/Day8/Abra)

# Description 

Abra is a proof of concept ClojureScript debugger.

It works!!  It does the stuff you can't do via a normal repl. 

You can set breakpoints and, when they are hit, you can:
 - inspect the value of ClojureScript variables in the call stack
 - evaluate arbitrary cljs code, referencing locals on the stack.

In the future, Abra may evolve towards being
a [Time Traveling Debugger] and perhaps include some effortless,
optional [tracing](https://github.com/spellhouse/clairvoyant) facilities.

We think designing a good debugger is a data visualization
problem. Not that we really know what that means, just yet ,except to say that
ClojureScript, being a lisp, has a  set of properties which should allow for
a really nice debugging story.

# The Bad News

This is clunky and ugly. 

It is especially fruitless to tell people to "never judge a book by its cover". Everyone
does and there is absolutely nothing you can say to make them not do it.

But really, you shouldn't judge Abra by its current poor cover. 

That worked, right?

Our goal until now has been to simply prove the concept, not
to create something lovely to use. As a result, you will be confronted with a user interface which is
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

Crmux is a bit of secret source. That's the way you can run two debuggers (consoles) off the one VM:  traditional dev-tools and something extra (that Abra provides).  

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


# Todo:

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
