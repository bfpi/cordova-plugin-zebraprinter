var exec = require('cordova/exec');

exports.print = function(macAddress, textToPrint, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'Zebraprinter', 'print', [macAddress, textToPrint]);
};
