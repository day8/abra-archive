# JSON <-> EDN Conversion

This document explains how to convert both JSON strings and JavaScript objects to EDN format and visa-versa


## JSON to EDN (js->clj)

Take the following sample JSON string:

	{
	    "glossary": {
	        "title": "example glossary",
	        "GlossDiv": {
	            "title": "S",
	            "GlossList": {
	                "GlossEntry": {
	                    "ID": "SGML",
	                    "SortAs": "SGML",
	                    "GlossTerm": "Standard Generalized Markup Language",
	                    "Acronym": "SGML",
	                    "Abbrev": "ISO 8879:1986",
	                    "GlossDef": {
	                        "para": "A meta-markup language, used to create markup languages such as DocBook.",
	                        "GlossSeeAlso": [
	                            "GML",
	                            "XML"
	                        ]
	                    },
	                    "GlossSee": "markup"
	                }
	            }
	        }
	    }
	}



Flatten it out:

	{"glossary": {"title": "example glossary","GlossDiv": {"title": "S","GlossList": {"GlossEntry": {"ID": "SGML","SortAs": "SGML","GlossTerm": "Standard Generalized Markup Language","Acronym": "SGML","Abbrev": "ISO 8879:1986","GlossDef": {"para": "A meta-markup language, used to create markup languages such as DocBook.","GlossSeeAlso": ["GML","XML"]},"GlossSee": "markup"}}}}}



Turn it into an escaped string:

	"{\"glossary\": {\"title\": \"example glossary\",\"GlossDiv\": {\"title\": \"S\",\"GlossList\": {\"GlossEntry\": {\"ID\": \"SGML\",\"SortAs\": \"SGML\",\"GlossTerm\": \"Standard Generalized Markup Language\",\"Acronym\": \"SGML\",\"Abbrev\": \"ISO 8879:1986\",\"GlossDef\": {\"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\"GlossSeeAlso\": [\"GML\",\"XML\"]},\"GlossSee\": \"markup\"}}}}}"



In ClojureScript, you can convert this JSON string into a JavaScript object:

	(def jsobj (js* "{\"glossary\": {\"title\": \"example glossary\",\"GlossDiv\": {\"title\": \"S\",\"GlossList\": {\"GlossEntry\": {\"ID\": \"SGML\",\"SortAs\": \"SGML\",\"GlossTerm\": \"Standard Generalized Markup Language\",\"Acronym\": \"SGML\",\"Abbrev\": \"ISO 8879:1986\",\"GlossDef\": {\"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\"GlossSeeAlso\": [\"GML\",\"XML\"]},\"GlossSee\": \"markup\"}}}}}"))



And output to the console in EDN format using my little pr_str helper function: 

	(console-log-prstr "jsobj" jsobj)
	
	Output:
	jsobj: #js {:glossary #js {:title "example glossary", :GlossDiv #js {:title "S", :GlossList #js {:GlossEntry #js {:ID "SGML", :SortAs "SGML", :GlossTerm "Standard Generalized Markup Language", :Acronym "SGML", :Abbrev "ISO 8879:1986", :GlossDef #js {:para "A meta-markup language, used to create markup languages such as DocBook.", :GlossSeeAlso #js ["GML" "XML"]}, :GlossSee "markup"}}}}}



Now convert the JSON object to EDN:

	(def ednobj (js->clj jsobj :keywordize-keys true))

Note: If you don't use the :keywordize-keys option, the keywords are defined as quoted strings



Output to the console as EDN using the same helper function:

    (console-log-prstr "ednobj" ednobj)
	
	Output:
	ednobj: {:glossary {:title "example glossary", :GlossDiv {:title "S", :GlossList {:GlossEntry {:ID "SGML", :SortAs "SGML", :GlossTerm "Standard Generalized Markup Language", :Acronym "SGML", :Abbrev "ISO 8879:1986", :GlossDef {:para "A meta-markup language, used to create markup languages such as DocBook.", :GlossSeeAlso ["GML" "XML"]}, :GlossSee "markup"}}}}}



Now turn this into an escaped string:

	{:glossary {:title \"example glossary\", :GlossDiv {:title \"S\", :GlossList {:GlossEntry {:ID \"SGML\", :SortAs \"SGML\", :GlossTerm \"Standard Generalized Markup Language\", :Acronym \"SGML\", :Abbrev \"ISO 8879:1986\", :GlossDef {:para \"A meta-markup language, used to create markup languages such as DocBook.\", :GlossSeeAlso [\"GML\" \"XML\"]}, :GlossSee \"markup\"}}}}}



Unfortunately, ClojureScript does not support pretty printing, so...

Load a Clojure REPL (lein repl) and you can pretty print the EDN string:

	(pprint (read-string "{:glossary {:title \"example glossary\", :GlossDiv {:title \"S\", :GlossList {:GlossEntry {:ID \"SGML\", :SortAs \"SGML\", :GlossTerm \"Standard Generalized Markup Language\", :Acronym \"SGML\", :Abbrev \"ISO 8879:1986\", :GlossDef {:para \"A meta-markup language, used to create markup languages such as DocBook.\", :GlossSeeAlso [\"GML\" \"XML\"]}, :GlossSee \"markup\"}}}}}"))
	
	Output:
	{:glossary
	 {:title "example glossary",
	  :GlossDiv
	  {:title "S",
	   :GlossList
	   {:GlossEntry
	    {:ID "SGML",
	     :SortAs "SGML",
	     :GlossTerm "Standard Generalized Markup Language",
	     :Acronym "SGML",
	     :Abbrev "ISO 8879:1986",
	     :GlossDef
	     {:para
	      "A meta-markup language, used to create markup languages such as DocBook.",
	      :GlossSeeAlso ["GML" "XML"]},
	     :GlossSee "markup"}}}}}


## EDN to JSON (clj->js)

Take the following sample EDN object:

	{:a {:first "gregg", :last "ramsey", :age 50},
	 :b 45,
	 :c [1 2 3 4 5],
	 :d #{1 2 3 "a" "b" "c"}}


And flatten it and load it into ClojureScript like so:

    (def ednobj {:a {:first "gregg" :last "ramsey" :age 50} :b 45 :c [1 2 3 4 5] :d #{1 2 3 "a" "b" "c"}})


Now we can convert this to a JavaScript object with:

    (def jsobj (clj->js ednobj))


We can use the following to display the EDN object:

    (console-log-prstr "ednobj" ednobj)
	
	Output:
	ednobj: {:a {:first "gregg", :last "ramsey", :age 50}, :b 45, :c [1 2 3 4 5], :d #{1 "a" 2 "b" 3 "c"}}


And the following to display the JavaScript object as JSON:

    (console-log-stringify "jsobj" jsobj)
	
	Output:
	jsobj: {"a":{"first":"gregg","last":"ramsey","age":50},"b":45,"c":[1,2,3,4,5],"d":[1,"a",2,"b",3,"c"]}

Note: the d member has been converted to a JavaScript array as there is no equivalent to the ClojureScript set


And the following to display it in EDN format:

    (console-log-prstr "jsobj" jsobj)
	
	Output:
	jsobj: #js {:a #js {:first "gregg", :last "ramsey", :age 50}, :b 45, :c #js [1 2 3 4 5], :d #js [1 "a" 2 "b" 3 "c"]}

