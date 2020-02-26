const { setMusicState } = require(`./mopidyFunctions/setMusicState`);
const { addSong } = require(`./mopidyFunctions/addSong`);
const { playBelter } = require(`./mopidyFunctions/playBelter`);
const { showPlaying } = require(`./mopidyFunctions/showPlaying`);
const { helpCommand } = require(`./mopidyFunctions/helpCommand`);
const { getQuote } = require(`./mopidyFunctions/getQuote`);
const { upNext } = require(`./mopidyFunctions/upNext`);
const { skipTrack } = require(`./mopidyFunctions/skipTrack`);

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
  { name: `keep`, execute: skipTrack },
  { name: `quote`, execute: getQuote }
];

module.exports.commands = commands;
