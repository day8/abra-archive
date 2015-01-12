var atom = require('atom-shell')
var proc = require('child_process')

// will something similar to print /Users/maf/.../Atom
console.log(atom)
var exec = require('child_process').exec,
    child;

child = exec(atom + ' .',
  function (error, stdout, stderr) {
    console.log('stdout: ' + stdout);
    console.log('stderr: ' + stderr);
    if (error !== null) {
      console.log('exec error: ' + error);
    }
});
// spawn atom-shell
//var child = proc.spawn(atom, ".")