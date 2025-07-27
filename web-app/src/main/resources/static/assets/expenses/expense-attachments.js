var lastTarget = null;

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

window.addEventListener("drop", function (e) {
    e.preventDefault();
    document.querySelector("#dropzone").style.visibility = "hidden";
    document.querySelector("#dropzone").style.opacity = 0;
    document.querySelector("#textnode").style.fontSize = "42px";

    if (e.dataTransfer.files.length > 0) {
        const files = e.dataTransfer.files;
        const fileInput = document.getElementById("file-input");

        const selectedRow = document.querySelector('tr[data-expense-id].selected') || 
                           document.querySelector('tr[data-expense-id]:hover') ||
                           document.querySelector('tr[data-expense-id]');
        
        const expenseId = selectedRow?.getAttribute('data-expense-id');
        if (!expenseId) {
            alert("Please select an expense by clicking on a row before dropping files.");
            return;
        }

        const dataTransfer = new DataTransfer();
        for (let i = 0; i < files.length; i++) {
            dataTransfer.items.add(files[i]);
        }
        fileInput.files = dataTransfer.files;

        const expenseIdInput = document.getElementById("expense-id-input");
        if (expenseIdInput) {
            expenseIdInput.value = expenseId;
        }

        htmx.trigger(fileInput.form, "submit");
    } else {
        alert("No files dropped.");
    }
});

let selectedExpenseId = null;

document.addEventListener('DOMContentLoaded', function() {
    document.addEventListener('click', function(e) {
        const row = e.target.closest('tr[data-expense-id]');
        if (row) {
            document.querySelectorAll('tr[data-expense-id]').forEach(r => r.classList.remove('selected'));
            row.classList.add('selected');
            selectedExpenseId = row.getAttribute('data-expense-id');
        }
    });

    const fileInput = document.getElementById("file-input");
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files.length > 0 && selectedExpenseId) {
                const expenseIdInput = document.getElementById("expense-id-input");
                if (expenseIdInput) {
                    expenseIdInput.value = selectedExpenseId;
                }
                htmx.trigger(this.form, "submit");
            }
        });
    }
});

function selectExpenseAndUpload(expenseId) {
    selectedExpenseId = expenseId;
    document.querySelectorAll('tr[data-expense-id]').forEach(r => r.classList.remove('selected'));
    const row = document.querySelector(`tr[data-expense-id="${expenseId}"]`);
    if (row) {
        row.classList.add('selected');
    }
    document.getElementById("file-input").click();
}

document.addEventListener("htmx:afterSwap", function(e) {
    if (e.detail.target.id === "attachment-messages") {
        const fileInput = document.getElementById("file-input");
        if (fileInput) {
            fileInput.value = "";
        }
        
        if (e.detail.xhr.status === 200) {
            setTimeout(() => {
                const startDateInput = document.querySelector('input[name="startDate"]');
                if (startDateInput) {
                    htmx.trigger(startDateInput, 'change');
                }
            }, 1000);
        }
    }
}); 