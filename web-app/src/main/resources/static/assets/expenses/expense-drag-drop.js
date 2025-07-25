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

function addFiles(files) {
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const existingFile = selectedFiles.find(f => f.name === file.name && f.size === file.size);
        if (!existingFile) {
            selectedFiles.push(file);
        }
    }
    updateFileInput();
}

function removeFile(fileName) {
    selectedFiles = selectedFiles.filter(file => file.name !== fileName);
    updateFileInput();
}

function clearSelectedFiles() {
    selectedFiles = [];
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
    } else if (selectedFiles.length === 1) {
        label.textContent = `1 file selected: ${selectedFiles[0].name}`;
    } else {
        const fileNames = selectedFiles.map(file => file.name).join(", ");
        label.textContent = `${selectedFiles.length} files selected: ${fileNames}`;
    }
}

window.addEventListener("drop", function (e) {
    e.preventDefault();
    document.querySelector("#dropzone").style.visibility = "hidden";
    document.querySelector("#dropzone").style.opacity = 0;
    document.querySelector("#textnode").style.fontSize = "42px";

    if (e.dataTransfer.files.length > 0) {
        addFiles(Array.from(e.dataTransfer.files));
    } else {
        alert("No files dropped.");
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById("file-input");
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files.length > 0) {
                addFiles(Array.from(this.files));
            }
        });
    }
    
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function(e) {
            const fileInput = document.getElementById("file-input");
        });
    }
    
    document.addEventListener('submit', function(e) {
        if (e.target.tagName === 'FORM') {
            setTimeout(() => {
                clearSelectedFiles();
            }, 100);
        }
    });
}); 