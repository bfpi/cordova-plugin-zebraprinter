var exec = require('cordova/exec');

exports.discover = function (successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'Zebraprinter', 'discover');
};

exports.print = function (macAddress, textToPrint, successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'Zebraprinter', 'print', [macAddress, textToPrint]);
};
