const { setMusicState } = require("./MopidyFunctions/setMusicState");
const { addSong } = require("./MopidyFunctions/addSong");
const { playBelter } = require("./MopidyFunctions/playBelter");
const { showPlaying } = require("./MopidyFunctions/showPlaying");
const { helpCommand } = require("./MopidyFunctions/helpCommand");
const commands = [
  { name: `what`, execute: showPlaying },
  { name: `play`, execute: addSong },
  { name: `music`, execute: setMusicState },
  { name: `belter`, execute: playBelter },
  { name: `help`, execute: helpCommand }
];

module.exports.commands = commands;
