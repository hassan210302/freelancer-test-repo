let costCounter = 0;

function addCost() {
    costCounter++;
    const costsContainer = document.getElementById('costs-container');
    const costTemplate = document.getElementById('cost-template');
    const newCost = costTemplate.content.cloneNode(true);
    
    // Update all IDs to be unique
    const costElements = newCost.querySelectorAll('[id]');
    costElements.forEach(element => {
        if (element.id) {
            element.id = element.id.replace('cost-template', `cost-${costCounter}`);
        }
    });
    
    // Update labels to reference the new IDs
    const labels = newCost.querySelectorAll('label[for]');
    labels.forEach(label => {
        if (label.getAttribute('for')) {
            label.setAttribute('for', label.getAttribute('for').replace('cost-template', `cost-${costCounter}`));
        }
    });
    
    // Add remove button functionality
    const removeButton = newCost.querySelector('.r-remove-cost-btn');
    if (removeButton) {
        removeButton.onclick = function() {
            this.closest('.r-cost-item').remove();
            updateCostNumbers();
        };
    }
    
    costsContainer.appendChild(newCost);
    updateCostNumbers();
}

function updateCostNumbers() {
    const costItems = document.querySelectorAll('.r-cost-item');
    costItems.forEach((item, index) => {
        const costNumber = item.querySelector('.r-cost-number');
        if (costNumber) {
            costNumber.textContent = `Cost ${index + 1}`;
        }
    });
}

function removeCost(button) {
    button.closest('.r-cost-item').remove();
    updateCostNumbers();
}

document.addEventListener('DOMContentLoaded', function() {
    // Initialize the first cost
    updateCostNumbers();
    
    // Add event listener for the add cost button
    const addCostButton = document.getElementById('add-cost-btn');
    if (addCostButton) {
        addCostButton.onclick = addCost;
    }
}); 