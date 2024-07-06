this.inputEditor = CodeMirror.fromTextArea(document.getElementById("source"), {
    lineNumbers: false, mode: "text/bau"
});
setInterval(function () {
    inputEditor.refresh();
    inputEditor.save();
}, 200)