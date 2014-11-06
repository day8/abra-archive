Abra2
=====


Terminology
--------

The terminology used in the Atom Shell documentation can be confusing. When talking about "where code lives" 
they talk about "browser" and "client".  What they means by "browser" is the nodejs context. And "client" is the 
HTML world - chrome. 

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


Running
--------

Look in the `run.bat`

