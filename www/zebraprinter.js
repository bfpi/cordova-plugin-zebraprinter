var exec = require('cordova/exec');

module.exports = {
  discover: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'Zebraprinter', 'discover');
  },

  print: function(macAddress, textToPrint, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'Zebraprinter', 'print', [macAddress, textToPrint]);
  }
};
