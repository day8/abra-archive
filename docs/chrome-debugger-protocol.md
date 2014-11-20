Okay, so let's imagine I'm in the debugger, at a breakpoint, and that I want to continue. I press the "resume button" in the page overlay. 

In response, the debugger will run to the next breakpoint, then stop.  Below I try to caputre the sort of backwards and forwards happening between dev-tools and the debugger as this process occurs. I try to exercise and annotate the protocol used.

To start with, the debugger tells dev-tools that the page has "resumed". The user, me, pressed "resume" in the page overlay, not in devtools itself, so dev-tools has to be told about the change:
```
{"method":"Debugger.resumed"}
```

Because dev-tools now knows that we're no longer at a breakpoint, it adjusts its GUi  so that the `Call Stack` part of the UI shows nothing but "not paused". Same with `Scope Variables` part.

<br/>

The debugger then runs for a while, before hitting another breakpoint and pausing.


<br/>

The debugger seends dev-tools a notification (event):
```
{
    "method": "Debugger.paused",
    "reason": "other",
    "hitBreakpoints": [
        "file:///D:/misc/gist/out/gist/core.js:48:97"
    ],
    "params": {
        "callFrames": []     <---- lots of these. See seperate doc. 
    }
}

```
Now, the `callframes` vector above is pretty big. Too big to show here.  Instead look at the sample file `Debugger.paused.json` for the full JSON notification.

The contents of `callFrames` can be used to populate the `Call Stack` section of the dev-tools GUI (on the right).

<br/>

Because we've gone into a "paused" state (at the beakpoint), dev-tools tells the debugger to put an overlay over the page being debugged. 
```
{"method":"Debugger.setOverlayMessage","id":51}
```

and gets a response of:

```
 {"result":{},"id":51}
```

Notice how the `id` of `51` matches the original request id above. You'll see that happeing with every message.

<br/>
Okay, so we have lodged at a breakpoint.  But dev-tools only has sufficient information to fill in the `Call Stack`.  It needs to fill in the rest. 

To update the  `Scope Variables` section of the GUI, dev-tools requests information on the top stackframe. It sends a `Runtime.getProperties` request:
```
{
    "method": "Runtime.getProperties",
    "params": {
        "objectId": "{\"injectedScriptId\":20,\"id\":84}",
        "ownProperties": false,
        "accessorPropertiesOnly": false
    },
    "id": 52
}
```
This request asks for information about an object -- except, in this case, the object is a stackframe.  The `objectId` supplied was provided to dev-tools via in the `callFrames` in the recently received `paused` notification.

Notice that the `objectId` is a string which appears to contain JSON, but we just treat it as a string (I think ... guessing here). 

The debugger responds with a vector of `result` items for each properties of the top stackframe object. And here's the thing -- this top stackframe object is just the function in which we are paused.  And these properties are shown in the `Locals` section of `Scope Variables`.   Just to be clear:  all we are doing is asking for the "properties" of an object, and that object is the function at the top of the stackframe. 

```
{
"result": {
    "result": [
        {
            "value": {
                "type": "undefined"        // <--- XXX no value
            },
            "writable": true,
            "enumerable": true,
            "configurable": true,
            "name": "G__5141",
            "isOwn": true
        },
        {
    		"value": {
    			"type": "number",          // <-- XXX simple number
    			"value": 32374988,
    			"description": "32374988"
    		},
    		"writable": true,
    		"enumerable": true,
    		"configurable": true,
    		"name": "cljs$lang$protocol_mask$partition0$",
    		"isOwn": true
    	},
        {
        	"name": "__proto__",
        	"value": {
        		"type": "object",
        		"objectId": "{\"injectedScriptId\":20,\"id\":174}",  // <-- XXX has objectId
        		"className": "Object",
        		"description": "Object"
        	},
        	"writable": true,
        	"configurable": true,
        	"enumerable": false,
        	"isOwn": true
        },
        {}      <---- lots of these. See seperate json sample. 
    }
}
```

Notice that each properties has a `type` of number, undefined, function or object (XXX any others ... array?).  

In the UI, `Locals` is a tree, and the user can more deeply explore nodes which represent objects (but not numbers, etc) by expanding the UI tree for the object. In response, dev-tools would make further calls to `Runtime.getProperties` on the object being explored, and it will fill in the tree just in time. A lazy tree.

See sample file `Runtime.getProperties.response.json` for full JSON response. 

Right, so now dev-tools has enough information to populate `Locals` in the `Scope Variables` section of the dev-tools GUI.

<br/>
But there's a `Globals` section in `Scope Variables` too.  What's that about?  Well, first off notice that it starts off in the UI tree as a closed node.  You can open the node, and, if you do, dev-tools will "just in time" make a call to `Runtime.getProperties`on the `windows` object.  Properties of `window` are the "global variables" of the HTML world. The "result" properties obtained are added as children to `Globals`. (XXX what objectId for window object, I wonder. XXX)

<br/>
As a final step in this "hit a breakpoint" process, dev-tools has to handle anything in the "Watch Expressions" part of the GUI.

Each expression is really just a string to be 'evaled' in the context of the currently selected call stack. 

For this, dev-tools uses the `Debugger.evaluateOnCallFrame` method.

```
{
    "method": "Debugger.evaluateOnCallFrame",
    "params": {
        "callFrameId": "{\"ordinal\":0,\"injectedScriptId\":20}",
        "expression": "prev",      // <----  javascript we want evaluated.
        "objectGroup": "watch-group",
        "includeCommandLineAPI": false,
        "doNotPauseOnExceptionsAndMuteConsole": true,
        "returnByValue": false,
        "generatePreview": false
    },
    "id": 203
}
```

There's various things to notice about this request. 

Notice that `callFrameId` is an object id (a string/json thing). If, in the UI, the user chooses a different Call Stack item, then evaluation of "Watch Expressions" must happen in the context of the object representing that stackframe.

In the JSON, notice the string `expression` to be evaluated. 

Notice also the use of `doNotPauseOnExceptionsAndMuteConsole` set to true, so there's no exception if evaluating the watched expression causes an exception.

The response looks like this:
```
{
    "result": {
        "result": {
            "type": "object",
            "objectId": "{\"injectedScriptId\":20,\"id\":14551}",
            "className": "datascript.TxReport",
            "description": "datascript.TxReport"
        },
        "wasThrown": false
    },
    "id": 203
}
```

So that's a pretty standard `result` response. We've now seen a few of them.

So we're at a breakpoint, and the GUI is all filled in. 

Imagine, that I then started using the dev-tool GUI.  I click on "tx_data" in the "Scope Variable" "local" section (which will cause the "tree" to expand). So dev-tools sends:
```
{
    "method": "Runtime.getProperties",
    "params": {
        "objectId": "{\"injectedScriptId\":20,\"id\":165}",
        "ownProperties": true,
        "accessorPropertiesOnly": false
    },
    "id": 64
}
```

See `Runtime.getProperties.response.json` for full JSON response.



```
{
    "method": "Page.getResourceContent",
    "params": {
        "frameId": "17736.1",
        "url": "file:///D:/misc/gist/out/gist/core.js"
    },
    "id": 33
}
```


##When Breakpoint Hit

This causes an "overlay" to appear across the page-being-debugged. 

Shows "Paused In Debugger" together with buttons to continue or stepover.
```
{
    "method": "Debugger.setOverlayMessage",
    "params": {
        "message": "Paused in debugger"
    },
    "id": 46
}
```

```
{
    "result": {
        
    },
    "id": 46
}
```

##Set A Breakpoint

If the user clicks on the "resume" overlay showing across the page

```
  {
    "method": "Debugger.setBreakpointByUrl",
    "params": {
        "lineNumber": 50,
        "url": "file:///D:/misc/gist/out/gist/core.js",
        "columnNumber": 0,
        "condition": ""
    },
    "id": 143
  }
```


##Set A Breakpoint

####dev-tools sends: 

```
  {
    "method": "Debugger.setBreakpointByUrl",
    "params": {
        "lineNumber": 50,
        "url": "file:///D:/misc/gist/out/gist/core.js",
        "columnNumber": 0,
        "condition": ""
    },
    "id": 143
  }
```


####debugger responds
```
  {
    "result": {},
    "id": 143
  } 
```

##Remove A Breakpoint



dev-tools sends:

```
  {
    "method": "Debugger.removeBreakpoint",
    "params": {
        "breakpointId": "file:///D:/misc/gist/out/gist/core.js:50:0"
    },
    "id": 132
  }
```

debugger responds:

```
  {
    "result": {},
    "id": 132
  }
```


##Console Message Added Event

The debugger sends notifications like this through when something is added to the colnsole.  The idea is that dev-tools adds it to the console too. 

References: [developer.chrome.com](https://developer.chrome.com/devtools/docs/protocol/1.1/console)  and 
[chrome-remote-interface](https://github.com/cyrus-and/chrome-remote-interface/blob/master/lib/protocol.json#L912)



```
{
    "method": "Console.messageAdded",
    "params": {
        "message": {
            "source": "javascript",
            "level": "error",
            "text": "Uncaught BlahError: horrible, horrible thing",
            "timestamp": 1401067178.27504,
            "type": "log",
            "line": 12580,
            "column": 63,
            "url": "",
            "stackTrace": [
                {
                    "functionName": "cljs.core.BitmapIndexedNode.inode_assoc",
                    "scriptId": "488",
                    "url": "",
                    "lineNumber": 12580,
                    "columnNumber": 63
                },
                { ... stacktrace items  }
            ]
        }
    }
}
```

##What Does this do???


```
  {
    "method": "Runtime.releaseObjectGroup",
    "params": {
        "objectGroup": "popover"
    },
    "id": 139
}
```

```
  {
    "result": {},
    "id": 139
  } 
```
