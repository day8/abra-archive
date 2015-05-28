#!/usr/bin/env node
/*
The MIT License (MIT)

Copyright (c) 2013 Andrey Sidorov

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

var http      = require('http');
var bl        = require('bl');
var url       = require('url');
var WebSocket = require('ws');
var colors    = require('colors');


defaultConfig = {
    listen: 9223,      // our server listens on this port
    port: 9222,        // Chrome VM is listening on this port
    debug: false
};

function stop_crmux_server(server) {
    server.close();
}

function start_crmux_server(config) {
  config = config || defaultConfig;

  var lastId = 0;
  var upstreamMap = {};
  var cachedWsUrls = {};

  var cacheJson = function(res) {
      // if asked for /json
      // we have to get the upstream /json and then tweak it to contain a different port
      return http.request({
          port: config.port,
          path: '/json'
      }, function(upRes) {
          upRes.pipe(bl(function(err, data) {
              var tabs = JSON.parse(data.toString());
              var wsUrl, urlParsed;
              for (var i = 0; i < tabs.length; ++i) {
                  wsUrl = tabs[i].webSocketDebuggerUrl;

                  if (typeof wsUrl == 'undefined') {
                      wsUrl = cachedWsUrls[tabs[i].id];
                  }
                  if (typeof wsUrl == 'undefined')
                      continue;

                  urlParsed = url.parse(wsUrl, true);
                  urlParsed.port = config.listen;
                  delete urlParsed.host;
                  tabs[i].webSocketDebuggerUrl = url.format(urlParsed);
                  if (tabs[i].devtoolsFrontendUrl)
                      tabs[i].devtoolsFrontendUrl = tabs[i].devtoolsFrontendUrl.replace(wsUrl.slice(5), tabs[i].webSocketDebuggerUrl.slice(5));
                  // console.log(tabs[i].devtoolsFrontendUrl, wsUrl, tabs[i].webSocketDebuggerUrl);
                  // TODO: cache devtoolsFrontendUrl as well
                  cachedWsUrls[tabs[i].id] = wsUrl;
              }
              if (res) {
                  res.setHeader("Content-Type", "application/json"); // GR: This is JSON so we SHOULD set the content type! Ajax library is relying on it
                  res.end(JSON.stringify(tabs));
              }
          }));
      });
  }
  var server = http.createServer(function(req, res) {
      if (req.url == '/json') {
          cacheJson(res).end();
      } else {
          var options = {};
          options.port = config.port;
          options.path = req.url;
          http.request(options, function(upRes) {
              upRes.pipe(res);
          }).end();
      }
  });

  server.listen(config.listen);

  var wss = new WebSocket.Server({server: server});
  wss.on('connection', function(ws) {
      var jsonReq = cacheJson();
      jsonReq.end();

      jsonReq.on('close', function() {
          if (config.debug) {
              console.log('cachedWsUrls:', cachedWsUrls);
          }
      });

      ws._id = lastId++;

      var urlParsed = url.parse(ws.upgradeReq.url, true);
      urlParsed.protocol = 'ws:';
      urlParsed.slashes = '//';
      urlParsed.hostname = 'localhost';
      var wsUpstreamUrlPort = config.port;
      urlParsed.port = wsUpstreamUrlPort;
      delete urlParsed.query;
      delete urlParsed.search;
      delete urlParsed.host;
      var wsUpstreamUrl = url.format(urlParsed);
      var upstreamSocket;
      if (!upstreamMap[wsUpstreamUrl]) {
          upstreamSocket = new WebSocket(wsUpstreamUrl);
          upstreamMap[wsUpstreamUrl] = {
              localId: 0,
              socket: upstreamSocket,
              clients: [ws],
              localIdToRemote: {}
          };
          upstreamSocket.on('message', function(message) {
              var msgObj = JSON.parse(message);
              if (!msgObj.id) { // this is an event, broadcast it
                  upstreamMap[wsUpstreamUrl].clients.forEach(function(s) {
                      if (config.debug)
                          console.log('e> ' + message.cyan);
                      s.send(message);
                  });
              } else {
                  var local = msgObj.id;
                  var idMap = upstreamMap[wsUpstreamUrl].localIdToRemote[local];
                  msgObj.id = idMap.id;
                  idMap.client.send(JSON.stringify(msgObj));
                  if (config.debug) {
                      console.log(String(idMap.client._id).blue + "> " + idMap.message.yellow);
                      console.log(String(idMap.client._id).blue + "> " + JSON.stringify(msgObj).green);
                  }
                  delete upstreamMap[wsUpstreamUrl].localIdToRemote[local];
              }
          });
      } else {
          upstreamSocket = upstreamMap[wsUpstreamUrl].socket;
          upstreamMap[wsUpstreamUrl].clients.push(ws);
      }

      ws._upstream = upstreamSocket;
      ws._upstream.params = upstreamMap[wsUpstreamUrl];

      ws.on('message', function(message) {
          var upstream = ws._upstream;

          var msgObj;
          try {
              msgObj = JSON.parse(message);
          } catch(e) {
              console.log(e);
              return;
          }
          upstream.params.localId++;
          var local = upstream.params.localId;
          var remote = msgObj.id;
          msgObj.id = local;
          upstream.params.localIdToRemote[local] = {
              client: ws,
              id: remote,
              message: message
          };
          if (upstream.readyState == 0) {
              upstream.once('open', function() {
                  upstream.send(JSON.stringify(msgObj));
              });
          } else
              upstream.send(JSON.stringify(msgObj));
      });
      ws.on('close', function() {
          // TODO:
          // var upstream = ws._upstream;
          // for each key in upstream.params.localIdToRemote
          // delete all keys where ws._id = key.client._id

          var purged = upstreamMap[wsUpstreamUrl].clients.filter(
              function(s) { return s._id != ws._id; }
          );
          upstreamMap[wsUpstreamUrl].clients = purged;
      });
  });

  return server;
}


module.exports = {
  stop_crmux_server: stop_crmux_server,
  start_crmux_server: start_crmux_server
}

