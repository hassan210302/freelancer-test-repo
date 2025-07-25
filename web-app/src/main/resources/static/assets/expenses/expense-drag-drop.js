var lastTarget = null;
var selectedFiles = [];

function isFile(evt) {
    var dt = evt.dataTransfer;
    for (var i = 0; i < dt.types.length; i++) {
        if (dt.types[i] === "Files") {
            return true;
        }
    }
    return false;
}

window.addEventListener("dragenter", function (e) {
    if (isFile(e)) {
        lastTarget = e.target;
        document.querySelector("#dropzone").style.visibility = "";
        document.querySelector("#dropzone").style.opacity = 1;
        document.querySelector("#textnode").style.fontSize = "48px";
    }
});

window.addEventListener("dragleave", function (e) {
    e.preventDefault();
    if (e.target === lastTarget || e.target === document) {
        document.querySelector("#dropzone").style.visibility = "hidden";
        document.querySelector("#dropzone").style.opacity = 0;
        document.querySelector("#textnode").style.fontSize = "42px";
    }
});

window.addEventListener("dragover", function (e) {
    e.preventDefault();
});

function addSingleFile(file) {
    selectedFiles = [file];
    updateFileInput();
}

function updateFileInput() {
    const fileInput = document.getElementById("file-input");
    const dataTransfer = new DataTransfer();
    
    selectedFiles.forEach(file => {
        dataTransfer.items.add(file);
    });
    
    fileInput.files = dataTransfer.files;
    updateLabel();
}

function updateLabel() {
    const label = document.querySelector(".file-feedback");
    if (!label) return;
    
    if (selectedFiles.length === 0) {
        label.textContent = "No files selected";
    } else {
        label.textContent = `1 file selected: ${selectedFiles[0].name}`;
    }
}

window.addEventListener("drop", function (e) {
    e.preventDefault();
    document.querySelector("#dropzone").style.visibility = "hidden";
    document.querySelector("#dropzone").style.opacity = 0;
    document.querySelector("#textnode").style.fontSize = "42px";

    if (e.dataTransfer.files.length > 0) {
        addSingleFile(e.dataTransfer.files[0]);
    } else {
        alert("No files dropped.");
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById("file-input");
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files.length > 0) {
                addSingleFile(this.files[0]);
                this.value = '';
            }
        });
    }
}); 