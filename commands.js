const { setMusicState } = require(`./MopidyFunctions/setMusicState`);
const { addSong } = require(`./MopidyFunctions/addSong`);
const { playBelter } = require(`./MopidyFunctions/playBelter`);
const { showPlaying } = require(`./MopidyFunctions/showPlaying`);
const { helpCommand } = require(`./MopidyFunctions/helpCommand`);
const { getQuote } = require(`./MopidyFunctions/getQuote`);
const { upNext } = require(`./MopidyFunctions/upNext`);
const { skipTrack } = require(`./MopidyFunctions/skipTrack`);

const commands = [
  { name: `what`, execute: showPlaying },
  { name: `play`, execute: addSong },
  { name: `yes`, execute: addSong },
  { name: `no`, execute: addSong },
  { name: `music`, execute: setMusicState },
  { name: `belter`, execute: playBelter },
  { name: `help`, execute: helpCommand },
  { name: `next`, execute: upNext },
  { name: `skip`, execute: skipTrack },
  { name: `quote`, execute: getQuote }
];

module.exports.commands = commands;
