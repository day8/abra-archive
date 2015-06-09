var atom = require('electron-prebuilt')
var proc = require('child_process')

// will something similar to print /Users/maf/.../Atom
var spawn = require('child_process').spawn,
    child;

console.log(process.cwd());

child = spawn(atom, ['.']);

child.stdout.on('data', function (data) {
  console.log('stdout: ' + data);});
child.stderr.on('data', function (data) {
  console.log('stderr: ' + data);});
child.on('close', function (code) {
  console.log('child process exited with code ' + code);});