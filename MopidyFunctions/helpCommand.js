async function helpCommand() {
  var helpInstructions =
    `Hello! You can tell me what to do with the following commands!` +
    `\n\n *play* "_insert search term_" : Queue up a song - respond with a yes or no.` +
    `\n *music* "_pause/play_" : Start or stop playback ` +
    `\n *what* : I'll tell you what's currently playing` +
    `\n *next* : Show's the current track list` +
    `\n *belter* : Queues up an absolute belter` +
    `\n *skip* : Vote to skip the current song` +
    `\n *keep* : Vote to keep the current song` +
    `\n *quote* : One of my classic quotes`;

  return helpInstructions;
}

module.exports.helpCommand = helpCommand;
