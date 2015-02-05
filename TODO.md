
Big Ticket
==========

 * Capture and study the JSON messages being sent backwards and forward between Chrome
    dev-tools and a "host" page.
      * for example when a breakpoint is hit, it seems as if dev-tools requests a huge
        amount of data so it can refresh its "Watch Expressions", "Call Stack" and
        "Locals/Globals" widgets.
      * We want to do that too.  So what do we have to send?  What do we get back?
      * what question does it ask to figure out the list if "locals" and "globals" ??
      * helpful?:
          *  https://github.com/cyrus-and/chrome-remote-interface
          *  https://github.com/cyrus-and/chrome-remote-interface/blob/master/lib/protocol.json
          *  https://developer.chrome.com/devtools/docs/protocol/1.1/debugger
          *  https://code.google.com/p/chromium/codesearch#chromium/src/third_party/WebKit/Source/devtools/protocol.json&q=protocol.json&sq=package:chromium&type=cs

  * Create a GUI which shows stuff  (Call Stack, Watch Expressions).  This can
    largely be achieved via prn_str, I think?  No transpiling from ClojureScript into
    javascript?

  * Create a console GUI ... transpiles the entered ClojureScript into Javascript etc
      * use https://github.com/fogus/himera
      * perhaps https://github.com/kanaka/clojurescript


Along The Way
=============

  * The Chrome dev-tools panel only shows up every second "refresh" of the app.  Ie.
    if it isn't there this time, just refresh the app and click 'Debug' again.


DONE
====

  * [DONE] Solve cljsbuild issue which means we have to build a large js file each time.

  * [DONE] Fix bug where calling localhost:9223/json is interpreted as EDN (required a mode to crmux to set content type to json)

  * [DONE] when they click "Debug", actually lanuch a new window for that URL

  * [DONE] Clicking the "Debug" button works, but hitting <return> in the URL field STILL doesn't
    work. <return> seems to restart the app ... the same as clicking "refresh" button.
    Hmm.  Could this is a focus thing??

  * then point the Chrome dev-tools panel at this new window, via these steps:
       * [DONE] obtain list from http://localhost:9223/json
       * [DONE] ignore the ABRA one. The one we want is the other one.
       * [DONE] manipulate the URL given so it says "ws=localhost:9223"
       * [DONE] launch dev-tools iframe with that manipulated URL   (:devtoolsURL  @app-state)
       * [SUNG] joyously see Chrome devtools. Hear Angels singing.

 