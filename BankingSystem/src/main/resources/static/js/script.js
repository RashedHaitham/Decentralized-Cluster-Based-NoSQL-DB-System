'use strict';

$(document).ready(function () {
    fetchAndDisplayDatabases();
    let selectedDbName = localStorage.getItem('selectedDbName') || '';
    let selectedCollectionName = localStorage.getItem('selectedCollectionName') || '';
    console.log('selectedDbName:', selectedDbName);
    console.log('selectedCollectionName:', selectedCollectionName);

    $('#createDbBtn').click(function () {
    $('#createDbModal').modal('show');
});

    function showChooseCollectionMessage() {
    $('#choose-collection-message').removeClass('d-none');
}

    // Function to hide the message
    function hideChooseCollectionMessage() {
    $('#choose-collection-message').addClass('d-none');
}

    function showNoDocumentsMessage() {
    $('#document-display').html('<p>No documents exist yet. Add a new one to see what happens.</p>');
    $('#documentCount').text('');
    $('#prevDocumentBtn').hide();
    $('#nextDocumentBtn').hide();
    $('#documentMessage').text('');
}

    function hideNoDocumentsMessage() {
    $('#document-display').empty().hide();
}

    async function displayDatabases() {
    try {
    const databaseNames = await $.get('/admin-dashboard/banking-system/fetchExistingDatabases');
    $('#databases').empty();
    if (!Array.isArray(databaseNames) || databaseNames.length === 0) {
    $('#no-databases-message').removeClass('d-none');
    return;
} else {
    $('#no-databases-message').addClass('d-none');
}
    let anyCollectionSelected = false;
    for (const dbName of databaseNames) {
    const databaseElement = $(`
               <div class="mb-3 database-section">
                    <button class="btn btn-sm expand-btn"><i class="fas fa-chevron-right"></i></button>
                    <strong class="database-name ml-2">${dbName}</strong>
                    <button class="icon-btn btn btn-sm ml-2 rounded-circle delete-btn"><i class="fas fa-trash-alt fa-xs"></i></button>
                    <button class="icon-btn btn btn-sm rounded-circle create-col-btn">+</button>
                    <div class="collection-container ml-4">
               </div>
                </div>
            `);

    $('#databases').append(databaseElement);
    const collections = await fetchAndDisplayCollections(databaseElement, dbName);
    if (collections.length === 0) {
    databaseElement.find('.expand-btn').prop('disabled', true);
}
    if (collections.includes(selectedCollectionName)) {
    anyCollectionSelected = true;
}
}
    console.log("Selected Database: ", selectedDbName);
    console.log("Selected Collection: ", selectedCollectionName);

    if (selectedDbName && selectedCollectionName) {
    console.log("Fetching documents for:", selectedDbName, selectedCollectionName);
    await fetchDocuments(selectedDbName, selectedCollectionName);
}

    if (!selectedCollectionName && anyCollectionSelected) {
    showChooseCollectionMessage();
} else {
    hideChooseCollectionMessage();
}
    if (anyCollectionSelected) {
    hideNoDocumentsMessage();
} else {
    showNoDocumentsMessage();
}
} catch (error) {
    console.error('An error occurred:', error);
}
}

    let shouldDisplayDocuments = false;

    let collectionItemClickEnabled = true;
    $(document).on('click', '.collection-item', function () {
    if (!collectionItemClickEnabled) {
    return;
}
    const dbName = $(this).closest('.database-section').find('.database-name').text();
    const collectionName = $(this).find('span').text();
    $('.collection-item').not(this).removeClass('selected');
    $(this).toggleClass('selected');
    localStorage.setItem('selectedDbName', dbName);
    localStorage.setItem('selectedCollectionName', collectionName);
    hideChooseCollectionMessage();
    shouldDisplayDocuments = true;
    fetchDocuments(dbName, collectionName)
    .catch(function (error) {
    console.error('Error fetching documents:', error);
});
});

    function fetchAndDisplayCollections(dbElement, dbName) {
    const collectionContainer = dbElement.find('.collection-container');
    return new Promise((resolve, reject) => {
    $.get(`/admin-dashboard/banking-system/fetchExistingCollections?db_name=${dbName}`, function (collections) {
    console.log(`Collections for ${dbName}:`, collections); // Log the collections to the console for debugging
    if (Array.isArray(collections)) {
    console.log(`Number of collections for ${dbName}:`, collections.length); // Log the collections to the console for debugging
    collectionContainer.empty();
    collections.forEach(collection => {
    appendCollectionToContainer(collectionContainer, collection);
});
    const hasCollections = collections.length > 0;
    console.log(`Has collections for ${dbName}:`, hasCollections); // Log whether there are collections
    dbElement.find('.expand-btn').prop('disabled', !hasCollections);
    resolve(collections); // Resolve the promise with the collections data
} else {
    console.error('Invalid collections data received for', dbName, collections);
    reject('Invalid collections data received');
}
}).fail(err => {
    console.error('Failed to fetch collections for', dbName, err);
    reject(err);
});
});
}

    function appendCollectionToContainer(container, collectionName) {
    if (collectionName.trim() !== '') { // Only add collections with non-empty names
    const collectionHtml = `
            <div class="collection-item d-flex justify-content-between align-items-center">
                <span class="ml-3">${collectionName}</span>
                    <div>
                       <button class="icon-btn btn btn-sm rounded-circle delete-collection-btn"><i class="fas fa-trash-alt fa-xs"></i></button>
                       <button class="icon-btn btn btn-sm rounded-circle add-document-btn">+</button>
                    </div>
            </div>
        `;
    container.append(collectionHtml);

}
}

    function fetchAndDisplayDatabases() {
    $.get('/admin-dashboard/banking-system/fetchExistingDatabases', function (data) {
    displayDatabases(data);
});
}

    $('#createDbForm').submit(function (e) {
    e.preventDefault();
    let dbName = $('#db_name').val();

    $.ajax({
    url: '/admin-dashboard/banking-system/createDB',
    method: 'POST',
    data: { db_name: dbName },
    success: function (response, textStatus, xhr) {
    if (xhr.status === 201) { // DB created successfully
    displayDatabases(response);
    $('#errorMsg').addClass('d-none');
    $('#createDbModal').modal('hide');

    let successMsg = $(`<div class="alert alert-success mt-3">Database has been added successfully.</div>`);
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
} else if (xhr.status === 409) { // Database already exists
    $('#errorMsg').removeClass('d-none').text("Database already exists.");
} else {
    $('#errorMsg').removeClass('d-none').text(response);
}
},
    error: function (jqXHR) {
    $('#errorMsg').removeClass('d-none').text(jqXHR.responseText || "There was an error processing your request. Please try again.");
}
});
});

    $(document).on('click', '.delete-btn', function () {
    const dbName = $(this).siblings('.database-name').text();
    const userConfirmation = confirm(`Are you sure you want to delete the database "${dbName}"?`);
    if (!userConfirmation) {
    return;
}
    $.ajax({
    url: '/admin-dashboard/banking-system/deleteDB',
    method: 'DELETE',
    data: {
    db_name: dbName
}
})
    .done(function (response) {
    if (Array.isArray(response)) {
    displayDatabases();
    let successMsg = $(`<div class="alert alert-success mt-3">Database ${dbName} has been deleted.</div>`);
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
    $('#document-display').empty().hide();
    $('#documentCount').text('');
    $('#prevDocumentBtn').hide();
    $('#nextDocumentBtn').hide();
    $('#documentMessage').text('');
    documents = [];
    currentDocumentIndex = 0;
    selectedDbName = '';
    selectedCollectionName = '';
    localStorage.setItem('selectedDbName', '');
    localStorage.setItem('selectedCollectionName', '');
    hideChooseCollectionMessage();
} else {
    alert(response);
}
})
    .fail(function () {
    alert("There was an error processing your request. Please try again.");
});
});

    $(document).on('click', '.create-col-btn', function () {
    selectedDbName = $(this).siblings('.database-name').text();
    selectedCollectionName = '';
    localStorage.setItem('selectedDbName', selectedDbName);
    localStorage.setItem('selectedCollectionName', '');
    $('#selectedDbName').text(selectedDbName);
    $('#createCollectionModal').modal('show');
});

    $('#createCollectionForm').submit(function (e) {
    e.preventDefault();
    let collectionName = $('#collection_name').val();
    $('#db_name_input').val(selectedDbName);
    $('#collection_name_input').val(collectionName);
    selectedCollectionName = collectionName;
    $.ajax({
    url: '/admin-dashboard/banking-system/createCol',
    method: 'POST',
    data: { db_name: selectedDbName, collection_name: collectionName },
    success: function (response, textStatus, xhr) {
    if (xhr.status === 201) { // Collection created successfully
    $('#collectionExistsMsg').addClass('d-none');
    $('#createCollectionModal').modal('hide');
    let successMsg = $(`<div class="alert alert-success mt-3">Collection created successfully.</div>`);
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
    const container = $(`.database-name`).filter(function () {
    return $(this).text() === selectedDbName;
}).siblings('.collection-container');

    container.append(`
                        <div class="collection-item d-flex justify-content-between align-items-center">
                            <span class="ml-3">${collectionName}</span>
                            <div>
                                <button class="icon-btn btn btn-sm rounded-circle delete-collection-btn"><i class="fas fa-trash-alt fa-xs"></i></button>
                                <button class="icon-btn btn btn-sm rounded-circle add-document-btn" data-dbname="${selectedDbName}" data-colname="${collectionName}">+</button>
                            </div>
                        </div>
                    `);

    container.prevAll('.expand-btn').children('i').removeClass('fa-chevron-right').addClass('fa-chevron-down');
    container.show();

} else if (xhr.status === 409) { // Collection already exists
    $('#collectionExistsMsg').removeClass('d-none').text("Collection already exists.");
}
},
    error: function (jqXHR) {
    $('#collectionExistsMsg').removeClass('d-none').text(jqXHR.responseText || "There was an error processing your request. Please try again.");
}
});
});

    $(document).on('click', '.delete-collection-btn', function () {
    const collectionItem = $(this).closest('.collection-item');
    const collectionName = collectionItem.find('span').text();
    const dbName = collectionItem.closest('.database-section').find('.database-name').text();
    const userConfirmation = confirm(`Are you sure you want to delete the collection "${collectionName}"?`);
    if (!userConfirmation) {
    return;
}
    collectionItemClickEnabled = false;
    $.ajax({
    url: '/admin-dashboard/banking-system/deleteCol',
    method: 'DELETE',
    data: {
    db_name: dbName,
    collection_name: collectionName
}
})
    .done(function (response) {
    if (Array.isArray(response)) {
    hideNoDocumentsMessage();
    collectionItem.fadeOut(300, function () {
    $(this).remove();
    const remainingCollections = $(this).closest('.database-section').find('.collection-item');
    if (remainingCollections.length === 0) {
    hideNoDocumentsMessage();
    showChooseCollectionMessage();
}
});
    let successMsg = $(`<div class="alert alert-success mt-3">Collection ${collectionName} has been deleted in ${dbName} database</div>`);
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
    selectedCollectionName = '';
    localStorage.setItem('selectedCollectionName', '');
    documents = [];
    currentDocumentIndex = 0;
    displayCurrentDocument();
} else {
    alert(response);
}
})
    .fail(function () {
    alert("There was an error processing your request. Please try again.");
})
    .always(function () {
    collectionItemClickEnabled = true;
});
});

    $(document).on('click', '.expand-btn', function () {
    const container = $(this).siblings('.collection-container');
    const icon = $(this).children('i');
    if (container.is(':visible')) {
    container.slideUp(300);
    icon.removeClass('fa-chevron-down').addClass('fa-chevron-right');
} else {
    container.slideDown(300);
    icon.removeClass('fa-chevron-right').addClass('fa-chevron-down');
}
});

    $(document).on('click', '.add-document-btn', function () {
    const dbName = $(this).closest('.database-section').find('.database-name').text();
    const colName = $(this).closest('.collection-item').find('span').text();

    $('#db_name_input').val(dbName);
    $('#collection_name_input').val(colName);
    $('#openAccountModal').modal('show');
});

    $(document).on('click', '.update-btn', function () {
    const dbName = localStorage.getItem('selectedDbName');
    const colName = localStorage.getItem('selectedCollectionName');

    $('#update_db_name_input').val(dbName);
    $('#update_collection_name_input').val(colName);
    const currDoc=documents[currentDocumentIndex];

    $('#UpdatedAccountNumber').val(currDoc.accountNumber);
    $('#UpdatedBalance').val(currDoc.balance);
    $('#UpdatedClientName').val(currDoc.clientName);

    console.log('data-dbname:', dbName);
    console.log('data-colname:', colName);
    $('#updateAccountModal').modal('show');
});

    $('#openAccountForm').submit(function (e) {
    e.preventDefault();
    $.ajax({
    type: "POST",
    url: "/admin-dashboard/banking-system/createAccount",
    data: $('#openAccountForm').serialize(),
    dataType: "json",
    success: function (response, textStatus, xhr) {
    $('#openAccountModal').modal('hide'); // Close the modal
    if (xhr.status === 201) { // Account created successfully
    const successMsg = $('<div class="alert alert-success mt-3"></div>').text("Account created successfully.");
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
    if (Array.isArray(response)) { // Checking if the response is an array of accounts
    documents = response; // Update the documents with the response data
    currentDocumentIndex = 0;  // Reset to the first document or set it to the latest
    displayCurrentDocument(); // Display the updated list
}
} else if (xhr.status === 409) {
    $('#accountExistsMsg').removeClass('d-none').text("Account with the same number already exists.");
    setTimeout(() => $('#accountExistsMsg').addClass('d-none'), 3000);
} else {
    $('#accountExistsMsg').removeClass('d-none').text(response.message || "Unknown error.");
    setTimeout(() => $('#accountExistsMsg').addClass('d-none'), 3000);
}
},
    error: function (jqXHR) {
    const errorMessage = jqXHR.responseText || "There was an error processing your request. Please try again.";
    $('#accountExistsMsg').removeClass('d-none').text(errorMessage);
    setTimeout(() => $('#accountExistsMsg').addClass('d-none'), 3000);
}
});
});

    let documents = [];
    let currentDocumentIndex = 0;

    function displayCurrentDocument() {
    const documentDisplay = $('#document-display');
    const documentCount = $('#documentCount');
    const prevBtn = $('#prevDocumentBtn');
    const nextBtn = $('#nextDocumentBtn');
    const documentMessage = $('#documentMessage');

    // Check if documents exist
    if (documents.length === 0 || currentDocumentIndex < 0 || currentDocumentIndex >= documents.length) {
    documentDisplay.html('<p>No documents exist yet. Add a new one to see what happens.</p>');
    documentCount.text('');
    prevBtn.hide();
    nextBtn.hide();
    documentMessage.text('');
    return;
}

    const currentDocument = documents[currentDocumentIndex];
    currentDocument.status = currentDocument.status ? "Active" : "Disabled";
    const formattedProperties = [];
    for (const key in currentDocument) {
    if (key === "_id") {
    formattedProperties.push(`<div><strong class="property-name">${key}</strong>: <span class="property-value" style="color: orange;">ObjectId("${currentDocument[key]}")</span></div>`);
} else {
    formattedProperties.push(`<div><strong class="property-name">${key}</strong>: <span class="property-value">${currentDocument[key]}</span></div>`);
}
}
    const formattedDocument = formattedProperties.join('');
    documentDisplay.html('<pre>' + formattedDocument + '</pre');
    const ActionBtns = `<button id="deleteDocumentBtn" class="btn btn-danger mt-2">Delete Account</button>
                               <button class="btn update-btn btn-warning mt-2">Update Account</button>`;

    documentDisplay.append(ActionBtns);

    const docCountText = `Displaying documents <strong>${currentDocumentIndex + 1} - ${documents.length}</strong>`;
    documentCount.html(docCountText);
    if (documents.length > 1) {
    prevBtn.show();
    nextBtn.show();
} else {
    prevBtn.hide();
    nextBtn.hide();
}
    if (currentDocumentIndex === 0) {
    documentMessage.html('<strong>You are on the first document</strong>');
} else {
    documentMessage.text('');
}
}

    $(document).on('click', '#deleteDocumentBtn', function () {
    const currentDocument = documents[currentDocumentIndex];
    const currentAccountNumber = currentDocument && currentDocument.accountNumber ? currentDocument.accountNumber : null;
    const currentDocumentId = currentDocument && currentDocument._id ? currentDocument._id : null;
    if (!currentDocumentId) {
    console.error('Error: The current document does not have a valid _id property.');
    return;
}
    const isUserSure = confirm(`Are you sure you want to delete the document with account number ${currentAccountNumber}?`);
    if (isUserSure) {
    deleteDocument(currentDocumentId);
} else {
    // If user clicked 'Cancel', do nothing
    console.log('User cancelled the deletion.');
}
});

    function deleteDocument(documentId) {
    const dbName = localStorage.getItem('selectedDbName');
    const collectionName = localStorage.getItem('selectedCollectionName');

    $.ajax({
    url: `/admin-dashboard/banking-system/deleteAccount`,
    method: 'DELETE',
    data: {
    db_name: dbName,
    collection_name: collectionName,
    doc_id: documentId
}
})
    .done(function(response) {
    if (Array.isArray(response)) {
    let successMsg = $(`<div class="alert alert-success mt-3">Account has been deleted.</div>`);
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
    documents = response;
    if (documents.length === 0) {
    currentDocumentIndex = 0;
} else if (currentDocumentIndex > documents.length - 1) {
    currentDocumentIndex--;
}
    displayCurrentDocument();
} else {
    // Display error on the page
    let errorMsg = $('#addErrorMsg');
    errorMsg.text(response || "Error while deleting the document.");
    errorMsg.removeClass('d-none');
    setTimeout(() => errorMsg.addClass('d-none'), 3000);
}
})
    .fail(function(jqXHR) {
    let errorMsg = $('#addErrorMsg');
    if (jqXHR.status === 401) {
    errorMsg.text("Unauthorized access. Please log in.");
} else if (jqXHR.status === 404) {
    errorMsg.text("Account not found.");
} else {
    errorMsg.text("There was an error processing your request. Please try again.");
}
    errorMsg.removeClass('d-none');
    setTimeout(() => errorMsg.addClass('d-none'), 3000);
});
}

    $('#updateAccountForm').submit(function (e)  {
    e.preventDefault();
    const currentDocument = documents[currentDocumentIndex];
    const currentDocumentId = currentDocument._id;
    $.ajax({
    type: "PUT",
    url: "/admin-dashboard/banking-system/updateDocument",
    data: $('#updateAccountForm').serialize() + '&doc_id=' + currentDocumentId,
    dataType: "json",
    success: function (response, textStatus, xhr) {
    $('#updateAccountModal').modal('hide'); // Close the modal
    if (xhr.status === 202) { // Account updated successfully
    const successMsg = $('<div class="alert alert-success mt-3"></div>').text("Account Updated successfully.");
    $('.container').prepend(successMsg);
    setTimeout(() => successMsg.fadeOut(), 3000);
    if (Array.isArray(response)) { // Checking if the response is an array of accounts
    documents = response; // Update the documents with the response data
    displayCurrentDocument(); // Display the updated list
}
} else if (xhr.status === 409) {
    $('#UpdateAccountExistsMsg').removeClass('d-none').text("Account with the same number already exists.");
    setTimeout(() => $('#UpdateAccountExistsMsg').addClass('d-none'), 3000);
} else {
    $('#UpdateAccountExistsMsg').removeClass('d-none').text(response.message || "Unknown error.");
    setTimeout(() => $('#UpdateAccountExistsMsg').addClass('d-none'), 3000);
}
},
    error: function (jqXHR) {
    const errorMessage = jqXHR.responseText || "There was an error processing your request. Please try again.";
    $('#UpdateAccountExistsMsg').removeClass('d-none').text(errorMessage);
    setTimeout(() => $('#UpdateAccountExistsMsg').addClass('d-none'), 3000);
}
});
});
    async function fetchDocuments(dbName, collectionName) {
    return $.get(`/admin-dashboard/banking-system/readAccounts?db_name=${dbName}&collection_name=${collectionName}`)
    .then(function (data) {
    console.log('Documents fetched:', data);
    if (Array.isArray(data)) {
    documents = data.sort((a, b) => a.accountNumber - b.accountNumber);
} else {
    documents = [];
}
    currentDocumentIndex = 0;
    if (shouldDisplayDocuments) {
    displayCurrentDocument();
    $('#document-display').css('display', 'block');
}
    return documents;
})
    .fail(function (error) {
    console.error('Failed to fetch documents:', error);
    throw error;
});
}

    $('#prevDocumentBtn').click(function () {
    if (currentDocumentIndex > 0) {
    currentDocumentIndex--;
    displayCurrentDocument();
}
});

    $('#nextDocumentBtn').click(function () {
    if (currentDocumentIndex < documents.length - 1) {
    currentDocumentIndex++;
    displayCurrentDocument();
}
});
})
