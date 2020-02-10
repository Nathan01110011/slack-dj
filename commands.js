const {
  setMusicState,
  addSong,
  showPlaying,
  playBelter
} = require("./functions.js");

const commands = [
  { name: `what`, execute: showPlaying },
  { name: `play`, execute: addSong },
  { name: `music`, execute: setMusicState },
  { name: `belter`, execute: playBelter }
];

module.exports.commands = commands;
