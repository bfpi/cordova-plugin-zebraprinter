var exec = require('cordova/exec');

module.exports = {
  print: function(macAddress, textToPrint, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'Zebraprinter', 'print', [macAddress, textToPrint]);
  }
};
